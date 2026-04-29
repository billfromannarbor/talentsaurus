package com.talentsaurus.matchmaking.matchmaking.service;

import com.talentsaurus.matchmaking.matchmaking.client.CandidateProfileClient;
import com.talentsaurus.matchmaking.matchmaking.client.PositionClient;
import com.talentsaurus.matchmaking.matchmaking.dto.FitRequest;
import com.talentsaurus.matchmaking.matchmaking.dto.FitResponse;
import org.springframework.stereotype.Service;

@Service
public class MatchmakingService {

  private final CandidateProfileClient candidateProfileClient;
  private final PositionClient positionClient;
  private final FitScoringService fitScoringService;

  public MatchmakingService(
      CandidateProfileClient candidateProfileClient,
      PositionClient positionClient,
      FitScoringService fitScoringService) {
    this.candidateProfileClient = candidateProfileClient;
    this.positionClient = positionClient;
    this.fitScoringService = fitScoringService;
  }

  public FitResponse match(FitRequest request) {
    var candidate =
        candidateProfileClient
            .getCandidate(request.candidateId())
            .orElseThrow(() -> new NotFoundException("Candidate not found"));
    var position =
        positionClient
            .getPosition(request.positionId())
            .orElseThrow(() -> new NotFoundException("Position not found"));

    return fitScoringService.score(candidate, position);
  }
}
