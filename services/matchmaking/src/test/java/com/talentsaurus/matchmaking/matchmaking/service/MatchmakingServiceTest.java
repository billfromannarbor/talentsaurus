package com.talentsaurus.matchmaking.matchmaking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.talentsaurus.matchmaking.matchmaking.client.CandidateProfileClient;
import com.talentsaurus.matchmaking.matchmaking.client.PositionClient;
import com.talentsaurus.matchmaking.matchmaking.dto.CandidateProfileSnapshot;
import com.talentsaurus.matchmaking.matchmaking.dto.FitRequest;
import com.talentsaurus.matchmaking.matchmaking.dto.PositionSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchmakingServiceTest {

  @Test
  void throwsWhenCandidateMissing() {
    CandidateProfileClient candidateClient = id -> Optional.empty();
    PositionClient positionClient = id -> Optional.of(samplePosition());

    MatchmakingService service =
        new MatchmakingService(candidateClient, positionClient, new FitScoringService());

    assertThatThrownBy(() -> service.match(new FitRequest(UUID.randomUUID(), UUID.randomUUID())))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Candidate not found");
  }

  @Test
  void returnsScoreWhenBothEntitiesExist() {
    CandidateProfileClient candidateClient = id -> Optional.of(sampleCandidate());
    PositionClient positionClient = id -> Optional.of(samplePosition());

    MatchmakingService service =
        new MatchmakingService(candidateClient, positionClient, new FitScoringService());

    var response = service.match(new FitRequest(sampleCandidate().id(), samplePosition().id()));

    assertThat(response.overallScore()).isGreaterThan(0);
    assertThat(response.recommendation()).isNotBlank();
  }

  private CandidateProfileSnapshot sampleCandidate() {
    return new CandidateProfileSnapshot(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "Jane",
        "Java engineer",
        List.of(new CandidateProfileSnapshot.Skill("Java")),
        List.of(new CandidateProfileSnapshot.Experience("Acme", "Engineer", "2020", "Present", "Built APIs")),
        List.of(new CandidateProfileSnapshot.Education("State", "Bachelor", "CS", "2016", "2020")));
  }

  private PositionSnapshot samplePosition() {
    return new PositionSnapshot(
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        "TopCo",
        "Engineer",
        List.of("Build APIs"),
        List.of("Java"),
        List.of("AWS"));
  }
}
