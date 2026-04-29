package com.talentsaurus.matchmaking.matchmaking.dto;

import java.util.List;
import java.util.UUID;

public record PositionSnapshot(
    UUID id,
    String company,
    String title,
    List<String> responsibilities,
    List<String> requiredSkills,
    List<String> preferredSkills) {}
