package com.talentsaurus.candidateprofile.candidate.repository;

import com.talentsaurus.candidateprofile.candidate.domain.CandidateProfileEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfileEntity, UUID> {}
