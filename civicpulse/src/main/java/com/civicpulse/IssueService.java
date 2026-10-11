package com.civicpulse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class IssueService {
  private final IssueRepo issues; private final CitizenRepo citizens; private final Categorizer ai;
  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
  public IssueService(IssueRepo i, CitizenRepo c, Categorizer a) { issues = i; citizens = c; ai = a; }

  public SseEmitter subscribe() {
    SseEmitter e = new SseEmitter(0L);
    emitters.add(e); e.onCompletion(() -> emitters.remove(e)); e.onTimeout(() -> emitters.remove(e));
    return e;
  }
  private void push(String type, Issue i) {
    for (SseEmitter e : emitters) try { e.send(SseEmitter.event().name(type).data(i)); } catch (Exception ex) { emitters.remove(e); }
  }
  private Citizen citizen(String name) { return citizens.findById(name).orElseGet(() -> new Citizen(name)); }

  public Issue create(Issue i) {
    if (i.category == null || i.category.isBlank()) {
      var r = ai.classify(i.title + " " + i.description);
      i.category = (String) r.get("category"); i.severity = (int) r.get("severity");
    }
    if (i.severity == 0) i.severity = 2;
    Issue saved = issues.save(i);
    Citizen c = citizen(i.reporter); c.points += 10; c.reports++; citizens.save(c);
    push("created", saved); return saved;
  }
  public Optional<Issue> verify(Long id, String who) {
    return issues.findById(id).map(i -> {
      i.verifyCount++;
      if (i.verifyCount >= 3 && i.status.equals("OPEN")) i.status = "VERIFIED";
      Citizen c = citizen(who); c.points += 3; c.verifications++; citizens.save(c);
      Issue s = issues.save(i); push("updated", s); return s;
    });
  }
  public Optional<Issue> setStatus(Long id, String status) {
    return issues.findById(id).map(i -> {
      i.status = status;
      if (status.equals("RESOLVED")) i.resolvedAt = Instant.now();
      if (status.equals("RESOLVED")) { Citizen c = citizen(i.reporter); c.points += 20; citizens.save(c); }
      Issue s = issues.save(i); push("updated", s); return s;
    });
  }

  public Map<String, Object> insights() {
    List<Issue> all = issues.findAll();
    Instant now = Instant.now();
    Map<String, Long> byCat = all.stream().collect(Collectors.groupingBy(i -> i.category, TreeMap::new, Collectors.counting()));
    Map<String, Long> byStatus = all.stream().collect(Collectors.groupingBy(i -> i.status, TreeMap::new, Collectors.counting()));
    long resolved = byStatus.getOrDefault("RESOLVED", 0L);
    // Hotspots: ~1km grid cells ranked by open issue count weighted by severity
    Map<String, List<Issue>> cells = all.stream().filter(i -> !i.status.equals("RESOLVED"))
      .collect(Collectors.groupingBy(i -> String.format("%.2f,%.2f", i.lat, i.lng)));
    List<Map<String, Object>> hot = cells.entrySet().stream().map(e -> {
      List<Issue> l = e.getValue(); String[] p = e.getKey().split(",");
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("lat", Double.parseDouble(p[0])); m.put("lng", Double.parseDouble(p[1]));
      m.put("count", l.size()); m.put("score", l.stream().mapToInt(x -> x.severity).sum());
      m.put("topCategory", l.stream().collect(Collectors.groupingBy(x -> x.category, Collectors.counting()))
        .entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey());
      return m; }).sorted((a, b) -> (int) b.get("score") - (int) a.get("score")).limit(5).toList();
    // Forecast: compare last 7 days with the 7 before; project next week by applying the trend
    List<Map<String, Object>> forecast = new ArrayList<>();
    for (String cat : byCat.keySet()) {
      long cur = all.stream().filter(i -> i.category.equals(cat) && i.createdAt.isAfter(now.minus(7, ChronoUnit.DAYS))).count();
      long prev = all.stream().filter(i -> i.category.equals(cat) && i.createdAt.isBefore(now.minus(7, ChronoUnit.DAYS)) && i.createdAt.isAfter(now.minus(14, ChronoUnit.DAYS))).count();
      long next = Math.max(0, Math.round(cur + (cur - prev) * 0.5));
      forecast.add(Map.of("category", cat, "last7", cur, "prev7", prev, "next7", next));
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("total", all.size()); out.put("resolved", resolved);
    out.put("resolutionRate", all.isEmpty() ? 0 : Math.round(100.0 * resolved / all.size()));
    out.put("byCategory", byCat); out.put("byStatus", byStatus);
    out.put("hotspots", hot); out.put("forecast", forecast);
    return out;
  }
}
