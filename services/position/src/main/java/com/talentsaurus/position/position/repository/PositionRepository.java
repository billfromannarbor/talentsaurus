package com.talentsaurus.position.position.repository;

import com.talentsaurus.position.position.domain.PositionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<PositionEntity, UUID> {}
