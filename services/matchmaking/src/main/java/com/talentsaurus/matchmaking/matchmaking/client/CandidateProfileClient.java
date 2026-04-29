package com.talentsaurus.matchmaking.matchmaking.client;

import com.talentsaurus.matchmaking.matchmaking.dto.CandidateProfileSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileClient {
  Optional<CandidateProfileSnapshot> getCandidate(UUID id);
}
