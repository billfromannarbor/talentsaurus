package com.talentsaurus.candidateprofile.candidate.service;

import com.talentsaurus.candidateprofile.candidate.domain.CandidateEducationEntity;
import com.talentsaurus.candidateprofile.candidate.domain.CandidateExperienceEntity;
import com.talentsaurus.candidateprofile.candidate.domain.CandidateProfileEntity;
import com.talentsaurus.candidateprofile.candidate.domain.CandidateReferenceEntity;
import com.talentsaurus.candidateprofile.candidate.domain.CandidateSkillEntity;
import com.talentsaurus.candidateprofile.candidate.dto.CandidateProfileRequest;
import com.talentsaurus.candidateprofile.candidate.dto.CandidateProfileResponse;
import com.talentsaurus.candidateprofile.candidate.repository.CandidateProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateProfileService {

  private final CandidateProfileRepository repository;

  public CandidateProfileService(CandidateProfileRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public UUID create(CandidateProfileRequest request) {
    CandidateProfileEntity entity = new CandidateProfileEntity();
    entity.setId(UUID.randomUUID());
    entity.setFullName(request.fullName());
    entity.setEmail(request.email());
    entity.setPhone(request.phone());
    entity.setLocation(request.location());
    entity.setSummary(request.summary());

    if (request.skills() != null) {
      request.skills().forEach(s -> entity.getSkills().add(toSkillEntity(entity, s)));
    }
    if (request.experiences() != null) {
      request.experiences().forEach(e -> entity.getExperiences().add(toExperienceEntity(entity, e)));
    }
    if (request.educations() != null) {
      request.educations().forEach(e -> entity.getEducations().add(toEducationEntity(entity, e)));
    }
    if (request.references() != null) {
      request.references().forEach(r -> entity.getReferences().add(toReferenceEntity(entity, r)));
    }

    return repository.save(entity).getId();
  }

  @Transactional(readOnly = true)
  public Optional<CandidateProfileResponse> get(UUID id) {
    return repository.findById(id).map(entity -> {
      entity.getSkills().size();
      entity.getExperiences().size();
      entity.getEducations().size();
      entity.getReferences().size();
      return toResponse(entity);
    });
  }

  private CandidateSkillEntity toSkillEntity(
      CandidateProfileEntity candidate, CandidateProfileRequest.Skill skill) {
    CandidateSkillEntity entity = new CandidateSkillEntity();
    entity.setCandidate(candidate);
    entity.setName(skill.name());
    return entity;
  }

  private CandidateExperienceEntity toExperienceEntity(
      CandidateProfileEntity candidate, CandidateProfileRequest.Experience experience) {
    CandidateExperienceEntity entity = new CandidateExperienceEntity();
    entity.setCandidate(candidate);
    entity.setCompany(experience.company());
    entity.setTitle(experience.title());
    entity.setStartDate(experience.startDate());
    entity.setEndDate(experience.endDate());
    entity.setDescription(experience.description());
    return entity;
  }

  private CandidateEducationEntity toEducationEntity(
      CandidateProfileEntity candidate, CandidateProfileRequest.Education education) {
    CandidateEducationEntity entity = new CandidateEducationEntity();
    entity.setCandidate(candidate);
    entity.setInstitution(education.institution());
    entity.setDegree(education.degree());
    entity.setField(education.field());
    entity.setStartDate(education.startDate());
    entity.setEndDate(education.endDate());
    return entity;
  }

  private CandidateReferenceEntity toReferenceEntity(
      CandidateProfileEntity candidate, CandidateProfileRequest.Reference reference) {
    CandidateReferenceEntity entity = new CandidateReferenceEntity();
    entity.setCandidate(candidate);
    entity.setFullName(reference.fullName());
    entity.setRelationship(reference.relationship());
    entity.setEmail(reference.email());
    entity.setPhone(reference.phone());
    return entity;
  }

  private CandidateProfileResponse toResponse(CandidateProfileEntity entity) {
    List<CandidateProfileResponse.Skill> skills =
        entity.getSkills().stream().map(s -> new CandidateProfileResponse.Skill(s.getName())).toList();

    List<CandidateProfileResponse.Experience> experiences =
        entity.getExperiences().stream()
            .map(
                e ->
                    new CandidateProfileResponse.Experience(
                        e.getCompany(),
                        e.getTitle(),
                        e.getStartDate(),
                        e.getEndDate(),
                        e.getDescription()))
            .toList();

    List<CandidateProfileResponse.Education> educations =
        entity.getEducations().stream()
            .map(
                e ->
                    new CandidateProfileResponse.Education(
                        e.getInstitution(),
                        e.getDegree(),
                        e.getField(),
                        e.getStartDate(),
                        e.getEndDate()))
            .toList();

    List<CandidateProfileResponse.Reference> references =
        entity.getReferences().stream()
            .map(
                r ->
                    new CandidateProfileResponse.Reference(
                        r.getFullName(), r.getRelationship(), r.getEmail(), r.getPhone()))
            .toList();

    return new CandidateProfileResponse(
        entity.getId(),
        entity.getFullName(),
        entity.getEmail(),
        entity.getPhone(),
        entity.getLocation(),
        entity.getSummary(),
        skills,
        experiences,
        educations,
        references);
  }
}
