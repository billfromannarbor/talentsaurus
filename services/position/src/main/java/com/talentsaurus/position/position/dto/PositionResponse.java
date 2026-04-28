package com.talentsaurus.position.position.dto;

import java.util.List;
import java.util.UUID;

public record PositionResponse(
    UUID id,
    String company,
    String title,
    List<String> responsibilities,
    List<String> requiredSkills,
    List<String> preferredSkills) {}
