package com.talentsaurus.matchmaking.matchmaking.dto;

import java.util.List;
import java.util.UUID;

public record CandidateProfileSnapshot(
    UUID id,
    String fullName,
    String summary,
    List<Skill> skills,
    List<Experience> experiences,
    List<Education> educations) {

  public record Skill(String name) {}

  public record Experience(String company, String title, String startDate, String endDate, String description) {}

  public record Education(String institution, String degree, String field, String startDate, String endDate) {}
}
