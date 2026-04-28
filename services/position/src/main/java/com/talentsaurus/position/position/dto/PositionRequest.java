package com.talentsaurus.position.position.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PositionRequest(
    @NotBlank String company,
    @NotBlank String title,
    List<String> responsibilities,
    List<String> requiredSkills,
    List<String> preferredSkills) {}
