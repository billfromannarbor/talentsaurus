package com.talentsaurus.matchmaking.matchmaking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentsaurus.matchmaking.matchmaking.dto.CandidateProfileSnapshot;
import com.talentsaurus.matchmaking.matchmaking.dto.PositionSnapshot;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FitScoringServiceFixtureTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private FitScoringService fitScoringService;

  @BeforeEach
  void setUp() {
    fitScoringService = new FitScoringService();
  }

  @Test
  void strongFixtureProducesStrongFit() throws Exception {
    CandidateProfileSnapshot candidate = read("fixtures/candidate-jane.json", CandidateProfileSnapshot.class);
    PositionSnapshot position = read("fixtures/position-strong.json", PositionSnapshot.class);

    var response = fitScoringService.score(candidate, position);

    assertThat(response.recommendation()).isEqualTo("strong_fit");
    assertThat(response.overallScore()).isGreaterThanOrEqualTo(85);
    assertThat(response.missingRequiredSkills()).isEmpty();
  }

  @Test
  void possibleFixtureProducesPossibleFit() throws Exception {
    CandidateProfileSnapshot candidate = read("fixtures/candidate-jane.json", CandidateProfileSnapshot.class);
    PositionSnapshot position = read("fixtures/position-possible.json", PositionSnapshot.class);

    var response = fitScoringService.score(candidate, position);

    assertThat(response.recommendation()).isEqualTo("possible_fit");
    assertThat(response.overallScore()).isBetween(55, 79);
    assertThat(response.missingRequiredSkills()).contains("kotlin");
  }

  @Test
  void weakFixtureProducesWeakFit() throws Exception {
    CandidateProfileSnapshot candidate = read("fixtures/candidate-jane.json", CandidateProfileSnapshot.class);
    PositionSnapshot position = read("fixtures/position-weak.json", PositionSnapshot.class);

    var response = fitScoringService.score(candidate, position);

    assertThat(response.recommendation()).isEqualTo("weak_fit");
    assertThat(response.overallScore()).isLessThan(55);
    assertThat(response.matchedRequiredSkills()).isEmpty();
  }

  private <T> T read(String path, Class<T> type) throws Exception {
    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    assertThat(in).isNotNull();
    return objectMapper.readValue(in, type);
  }
}
