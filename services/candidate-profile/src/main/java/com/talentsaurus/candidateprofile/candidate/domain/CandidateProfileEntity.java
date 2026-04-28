package com.talentsaurus.candidateprofile.candidate.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_profile")
public class CandidateProfileEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String fullName;

  private String email;
  private String phone;
  private String location;
  private String summary;

  @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<CandidateSkillEntity> skills = new ArrayList<>();

  @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<CandidateExperienceEntity> experiences = new ArrayList<>();

  @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<CandidateEducationEntity> educations = new ArrayList<>();

  @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<CandidateReferenceEntity> references = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public List<CandidateSkillEntity> getSkills() {
    return skills;
  }

  public void setSkills(List<CandidateSkillEntity> skills) {
    this.skills = skills;
  }

  public List<CandidateExperienceEntity> getExperiences() {
    return experiences;
  }

  public void setExperiences(List<CandidateExperienceEntity> experiences) {
    this.experiences = experiences;
  }

  public List<CandidateEducationEntity> getEducations() {
    return educations;
  }

  public void setEducations(List<CandidateEducationEntity> educations) {
    this.educations = educations;
  }

  public List<CandidateReferenceEntity> getReferences() {
    return references;
  }

  public void setReferences(List<CandidateReferenceEntity> references) {
    this.references = references;
  }
}
