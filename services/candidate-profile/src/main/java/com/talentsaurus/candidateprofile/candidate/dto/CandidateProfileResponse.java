package com.talentsaurus.candidateprofile.candidate.dto;

import java.util.List;
import java.util.UUID;

public record CandidateProfileResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    String location,
    String summary,
    List<Skill> skills,
    List<Experience> experiences,
    List<Education> educations,
    List<Reference> references) {

  public record Skill(String name) {}

  public record Experience(
      String company,
      String title,
      String startDate,
      String endDate,
      String description) {}

  public record Education(
      String institution,
      String degree,
      String field,
      String startDate,
      String endDate) {}

  public record Reference(String fullName, String relationship, String email, String phone) {}
}
