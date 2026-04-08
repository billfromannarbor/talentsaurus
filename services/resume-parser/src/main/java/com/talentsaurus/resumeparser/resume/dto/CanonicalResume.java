package com.talentsaurus.resumeparser.resume.dto;

import java.util.List;

public record CanonicalResume(
    Profile profile,
    List<Skill> skills,
    List<Experience> experiences,
    List<Education> educations,
    ParseMeta parseMeta
) {
  public record Profile(
      String fullName,
      String headline,
      String summary,
      String location,
      String phone,
      String email,
      String linkedInUrl,
      String githubUrl
  ) {}

  public record Skill(String name) {}

  public record Experience(
      String company,
      String title,
      String startDate,
      String endDate,
      String description
  ) {}

  public record Education(
      String institution,
      String degree,
      String field,
      String startDate,
      String endDate
  ) {}

  public record ParseMeta(
      List<String> sectionsDetected,
      int skillCount,
      int experienceCount,
      int educationCount,
      List<String> warnings
  ) {}
}
