package com.talentsaurus.matchmaking.matchmaking.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FitRequest(@NotNull UUID candidateId, @NotNull UUID positionId) {}
