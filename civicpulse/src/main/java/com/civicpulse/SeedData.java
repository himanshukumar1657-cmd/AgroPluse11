package com.civicpulse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
@Configuration
public class SeedData {
  @Bean CommandLineRunner seed(IssueRepo repo, IssueService svc, CitizenRepo citizens) {
    return a -> {
      if (repo.count() > 0) return;
      Object[][] d = {
        {"Deep pothole near market","Pothole on the main road, two scooters skidded",28.6139,77.2090,"Aarav",1},
        {"Garbage not collected for a week","Overflowing trash and bad smell by the park gate",28.6129,77.2295,"Meera",2},
        {"Water pipe leaking","Pipe leak flooding the lane every morning",28.6304,77.2177,"Kabir",3},
        {"Streetlight out on bridge","Whole stretch is dark after 7pm, feels unsafe",28.6562,77.2410,"Diya",9},
        {"Open manhole on footpath","Exposed manhole, children walk here",28.6145,77.2102,"Aarav",4},
        {"Cracked road after rain","Large crack spreading across asphalt",28.6150,77.2085,"Ishaan",10},
        {"Garbage dump at corner","Waste dumped near the school wall",28.6135,77.2290,"Meera",12}};
      for (Object[] r : d) {
        Issue i = new Issue(); i.title = (String) r[0]; i.description = (String) r[1]; i.lat = (double) r[2]; i.lng = (double) r[3];
        i.reporter = (String) r[4]; i = svc.create(i); i.createdAt = Instant.now().minus((int) r[5], ChronoUnit.DAYS); repo.save(i);
      }
      svc.verify(1L, "Meera"); svc.verify(1L, "Kabir"); svc.verify(1L, "Diya"); svc.setStatus(4L, "RESOLVED");
    };
  }
}
