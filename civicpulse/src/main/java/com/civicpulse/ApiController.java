package com.civicpulse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.nio.file.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class ApiController {
  private final IssueRepo repo; private final CitizenRepo citizens; private final IssueService svc; private final Categorizer ai;
  public ApiController(IssueRepo r, CitizenRepo c, IssueService s, Categorizer a) { repo = r; citizens = c; svc = s; ai = a; }

  @GetMapping("/issues") public List<Issue> list() { return repo.findAllByOrderByCreatedAtDesc(); }

  @PostMapping(value = "/issues", consumes = "multipart/form-data")
  public Issue create(@RequestParam String title, @RequestParam(defaultValue = "") String description,
      @RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = "Anonymous") String reporter,
      @RequestParam(required = false) String category, @RequestParam(required = false) MultipartFile media) throws Exception {
    Issue i = new Issue();
    i.title = title; i.description = description; i.lat = lat; i.lng = lng; i.reporter = reporter.isBlank() ? "Anonymous" : reporter; i.category = category;
    if (media != null && !media.isEmpty()) {
      Path dir = Paths.get("uploads"); Files.createDirectories(dir);
      String ext = Optional.ofNullable(media.getOriginalFilename()).filter(n -> n.contains(".")).map(n -> n.substring(n.lastIndexOf('.')).replaceAll("[^.A-Za-z0-9]", "")).orElse("");
      String name = UUID.randomUUID() + ext;
      Files.copy(media.getInputStream(), dir.resolve(name));
      i.mediaUrl = "/uploads/" + name;
      i.mediaType = Optional.ofNullable(media.getContentType()).orElse("").startsWith("video") ? "video" : "image";
    }
    return svc.create(i);
  }
  @PostMapping("/issues/{id}/verify")
  public ResponseEntity<Issue> verify(@PathVariable Long id, @RequestParam(defaultValue = "Anonymous") String who) {
    return svc.verify(id, who).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }
  @PatchMapping("/issues/{id}/status")
  public ResponseEntity<Issue> status(@PathVariable Long id, @RequestParam String value) {
    return svc.setStatus(id, value).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }
  @PostMapping("/categorize") public Map<String, Object> categorize(@RequestBody Map<String, String> b) { return ai.classify(b.get("text")); }
  @GetMapping("/insights") public Map<String, Object> insights() { return svc.insights(); }
  @GetMapping("/leaderboard") public List<Citizen> leaderboard() { return citizens.findTop10ByOrderByPointsDesc(); }
  @GetMapping("/stream") public SseEmitter stream() { return svc.subscribe(); }
}
