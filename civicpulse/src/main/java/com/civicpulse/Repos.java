package com.civicpulse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface IssueRepo extends JpaRepository<Issue, Long> { List<Issue> findAllByOrderByCreatedAtDesc(); }
interface CitizenRepo extends JpaRepository<Citizen, String> { List<Citizen> findTop10ByOrderByPointsDesc(); }
