package com.talentsaurus.matchmaking.matchmaking.dto;

import java.util.List;
import java.util.UUID;

public record FitResponse(
    UUID candidateId,
    UUID positionId,
    int overallScore,
    String recommendation,
    DimensionScores dimensionScores,
    List<String> matchedRequiredSkills,
    List<String> missingRequiredSkills,
    List<String> matchedPreferredSkills,
    List<String> explanations) {

  public record DimensionScores(
      int requiredSkillsScore,
      int preferredSkillsScore,
      int responsibilitiesScore,
      int experienceScore,
      int educationScore) {}
}
