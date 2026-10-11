package com.civicpulse;
import org.springframework.stereotype.Service;
import java.util.*;
/** Keyword-scoring classifier. Swap classify() for a vision/LLM call (e.g. Claude API) to read the photo itself. */
@Service
public class Categorizer {
  private static final Map<String, String[]> KEYWORDS = Map.of(
    "ROAD", new String[]{"pothole","road","crack","asphalt","footpath","bridge","traffic"},
    "WASTE", new String[]{"garbage","trash","waste","dump","litter","sewage","smell","overflow"},
    "WATER", new String[]{"water","leak","pipe","flood","drain","waterlogging","supply"},
    "LIGHTING", new String[]{"light","lamp","dark","streetlight","bulb","wire","electric"},
    "SAFETY", new String[]{"unsafe","accident","fire","theft","harass","stray","dog","open manhole","manhole"});
  private static final String[] URGENT = {"accident","fire","manhole","flood","injury","exposed","collapse","child"};
  public Map<String, Object> classify(String text) {
    String t = text == null ? "" : text.toLowerCase();
    String best = "OTHER"; int top = 0;
    for (var e : KEYWORDS.entrySet()) {
      int s = 0; for (String k : e.getValue()) if (t.contains(k)) s++;
      if (s > top) { top = s; best = e.getKey(); }
    }
    int sev = 2; for (String u : URGENT) if (t.contains(u)) sev++;
    double conf = top == 0 ? 0.2 : Math.min(0.95, 0.5 + top * 0.2);
    return Map.of("category", best, "severity", Math.min(sev, 5), "confidence", conf);
  }
}
