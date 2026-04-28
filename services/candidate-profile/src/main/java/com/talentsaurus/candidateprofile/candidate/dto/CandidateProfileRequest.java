package com.talentsaurus.candidateprofile.candidate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CandidateProfileRequest(
    @NotBlank String fullName,
    String email,
    String phone,
    String location,
    String summary,
    List<@Valid Skill> skills,
    List<@Valid Experience> experiences,
    List<@Valid Education> educations,
    List<@Valid Reference> references) {

  public record Skill(@NotBlank String name) {}

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
