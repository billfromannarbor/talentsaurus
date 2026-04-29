package com.talentsaurus.matchmaking.matchmaking.client;

import com.talentsaurus.matchmaking.matchmaking.dto.PositionSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface PositionClient {
  Optional<PositionSnapshot> getPosition(UUID id);
}
