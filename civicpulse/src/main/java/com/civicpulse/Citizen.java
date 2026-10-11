package com.civicpulse;
import jakarta.persistence.*;
@Entity
public class Citizen {
  @Id public String name;
  public int points;
  public int reports;
  public int verifications;
  public Citizen() {}
  public Citizen(String n) { name = n; }
  public String getBadge() { return points >= 150 ? "Ward Champion" : points >= 60 ? "Street Watcher" : points >= 20 ? "Neighbour" : "Newcomer"; }
}
