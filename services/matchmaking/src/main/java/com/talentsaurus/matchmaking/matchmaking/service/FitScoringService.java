package com.talentsaurus.matchmaking.matchmaking.service;

import com.talentsaurus.matchmaking.matchmaking.dto.CandidateProfileSnapshot;
import com.talentsaurus.matchmaking.matchmaking.dto.FitResponse;
import com.talentsaurus.matchmaking.matchmaking.dto.PositionSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FitScoringService {

  public FitResponse score(CandidateProfileSnapshot candidate, PositionSnapshot position) {
    Set<String> candidateSkills = normalizeSet(candidate.skills().stream().map(CandidateProfileSnapshot.Skill::name).toList());
    Set<String> required = normalizeSet(position.requiredSkills());
    Set<String> preferred = normalizeSet(position.preferredSkills());

    List<String> matchedRequired = intersection(required, candidateSkills);
    List<String> missingRequired = difference(required, candidateSkills);
    List<String> matchedPreferred = intersection(preferred, candidateSkills);

    int requiredScore = ratioScore(matchedRequired.size(), required.size());
    int preferredScore = ratioScore(matchedPreferred.size(), preferred.size());

    int responsibilitiesScore = scoreResponsibilities(candidate, position);
    int experienceScore = scoreExperience(candidate, position);
    int educationScore = scoreEducation(candidate);

    int overall =
        weighted(
            requiredScore,
            preferredScore,
            responsibilitiesScore,
            experienceScore,
            educationScore);

    String recommendation = recommendation(overall, missingRequired.isEmpty(), required.isEmpty());

    List<String> explanations = new ArrayList<>();
    explanations.add("Matched required skills: " + matchedRequired.size() + "/" + required.size());
    explanations.add("Matched preferred skills: " + matchedPreferred.size() + "/" + preferred.size());
    if (!missingRequired.isEmpty()) {
      explanations.add("Missing required skills: " + String.join(", ", missingRequired));
    }

    return new FitResponse(
        candidate.id(),
        position.id(),
        overall,
        recommendation,
        new FitResponse.DimensionScores(
            requiredScore, preferredScore, responsibilitiesScore, experienceScore, educationScore),
        matchedRequired,
        missingRequired,
        matchedPreferred,
        explanations);
  }

  private int scoreResponsibilities(CandidateProfileSnapshot candidate, PositionSnapshot position) {
    String corpus =
        (candidate.summary() == null ? "" : candidate.summary())
            + " "
            + String.join(
                " ", candidate.experiences().stream().map(CandidateProfileSnapshot.Experience::description).filter(s -> s != null && !s.isBlank()).toList());

    List<String> responsibilities = position.responsibilities() == null ? List.of() : position.responsibilities();
    if (responsibilities.isEmpty()) return 60;

    int hits = 0;
    String normalizedCorpus = normalize(corpus);
    for (String resp : responsibilities) {
      if (resp == null || resp.isBlank()) continue;
      String[] words = normalize(resp).split("\\s+");
      int wordHits = 0;
      for (String w : words) {
        if (w.length() < 4) continue;
        if (normalizedCorpus.contains(w)) wordHits++;
      }
      if (wordHits >= Math.max(1, words.length / 4)) hits++;
    }
    return ratioScore(hits, responsibilities.size());
  }

  private int scoreExperience(CandidateProfileSnapshot candidate, PositionSnapshot position) {
    int expCount = candidate.experiences() == null ? 0 : candidate.experiences().size();
    int reqCount = position.requiredSkills() == null ? 0 : position.requiredSkills().size();
    if (expCount >= 3) return 100;
    if (expCount == 2) return reqCount >= 5 ? 75 : 85;
    if (expCount == 1) return reqCount >= 5 ? 55 : 70;
    return 40;
  }

  private int scoreEducation(CandidateProfileSnapshot candidate) {
    if (candidate.educations() == null || candidate.educations().isEmpty()) return 50;
    String combined =
        normalize(
            String.join(
                " ",
                candidate.educations().stream()
                    .flatMap(e -> java.util.stream.Stream.of(e.degree(), e.field()))
                    .filter(s -> s != null && !s.isBlank())
                    .toList()));
    if (combined.contains("master") || combined.contains("phd")) return 100;
    if (combined.contains("bachelor")) return 85;
    return 70;
  }

  private int weighted(int required, int preferred, int responsibilities, int experience, int education) {
    return (required * 50 + preferred * 20 + responsibilities * 20 + experience * 5 + education * 5) / 100;
  }

  private String recommendation(int overall, boolean noMissingRequired, boolean noRequiredList) {
    if (overall >= 80 && (noMissingRequired || noRequiredList)) return "strong_fit";
    if (overall >= 55) return "possible_fit";
    return "weak_fit";
  }

  private int ratioScore(int numerator, int denominator) {
    if (denominator <= 0) return 60;
    return Math.max(0, Math.min(100, (numerator * 100) / denominator));
  }

  private Set<String> normalizeSet(List<String> values) {
    Set<String> out = new HashSet<>();
    if (values == null) return out;
    for (String raw : values) {
      if (raw == null) continue;
      String n = normalize(raw);
      if (!n.isBlank()) out.add(n);
    }
    return out;
  }

  private String normalize(String s) {
    return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#\\s]", " ").replaceAll("\\s+", " ").trim();
  }

  private List<String> intersection(Set<String> expected, Set<String> actual) {
    return expected.stream().filter(actual::contains).sorted().toList();
  }

  private List<String> difference(Set<String> expected, Set<String> actual) {
    return expected.stream().filter(e -> !actual.contains(e)).sorted().toList();
  }
}
