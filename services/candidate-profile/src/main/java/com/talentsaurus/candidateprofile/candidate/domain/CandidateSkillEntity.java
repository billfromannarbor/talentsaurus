package com.talentsaurus.candidateprofile.candidate.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "candidate_skill")
public class CandidateSkillEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "candidate_id", nullable = false)
  private CandidateProfileEntity candidate;

  private String name;

  public Long getId() {
    return id;
  }

  public CandidateProfileEntity getCandidate() {
    return candidate;
  }

  public void setCandidate(CandidateProfileEntity candidate) {
    this.candidate = candidate;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
