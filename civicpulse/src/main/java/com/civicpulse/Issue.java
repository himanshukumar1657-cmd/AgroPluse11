package com.civicpulse;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
public class Issue {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  public String title;
  @Column(length = 2000) public String description;
  public String category;      // ROAD, WASTE, WATER, LIGHTING, SAFETY, OTHER
  public int severity;         // 1-5, estimated by the categorizer
  public String status = "OPEN"; // OPEN, VERIFIED, IN_PROGRESS, RESOLVED
  public double lat, lng;
  public String mediaUrl, mediaType;
  public String reporter;
  public int verifyCount;
  public Instant createdAt = Instant.now();
  public Instant resolvedAt;
}
