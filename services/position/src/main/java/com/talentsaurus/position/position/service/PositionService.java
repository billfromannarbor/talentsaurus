package com.talentsaurus.position.position.service;

import com.talentsaurus.position.position.domain.PositionEntity;
import com.talentsaurus.position.position.dto.PositionRequest;
import com.talentsaurus.position.position.dto.PositionResponse;
import com.talentsaurus.position.position.repository.PositionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PositionService {

  private final PositionRepository repository;

  public PositionService(PositionRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public UUID create(PositionRequest request) {
    PositionEntity entity = new PositionEntity();
    entity.setId(UUID.randomUUID());
    entity.setCompany(request.company());
    entity.setTitle(request.title());
    entity.setResponsibilities(defaultList(request.responsibilities()));
    entity.setRequiredSkills(defaultList(request.requiredSkills()));
    entity.setPreferredSkills(defaultList(request.preferredSkills()));
    return repository.save(entity).getId();
  }

  @Transactional(readOnly = true)
  public Optional<PositionResponse> get(UUID id) {
    return repository.findById(id).map(this::toResponse);
  }

  private PositionResponse toResponse(PositionEntity entity) {
    entity.getResponsibilities().size();
    entity.getRequiredSkills().size();
    entity.getPreferredSkills().size();

    return new PositionResponse(
        entity.getId(),
        entity.getCompany(),
        entity.getTitle(),
        List.copyOf(entity.getResponsibilities()),
        List.copyOf(entity.getRequiredSkills()),
        List.copyOf(entity.getPreferredSkills()));
  }

  private List<String> defaultList(List<String> input) {
    return input == null ? List.of() : input;
  }
}
