package com.talentsaurus.position.position.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "position_description")
public class PositionEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String company;

  @Column(nullable = false)
  private String title;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "position_responsibility", joinColumns = @JoinColumn(name = "position_id"))
  @Column(name = "item_value")
  private List<String> responsibilities = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "position_required_skill", joinColumns = @JoinColumn(name = "position_id"))
  @Column(name = "item_value")
  private List<String> requiredSkills = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "position_preferred_skill", joinColumns = @JoinColumn(name = "position_id"))
  @Column(name = "item_value")
  private List<String> preferredSkills = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<String> getResponsibilities() {
    return responsibilities;
  }

  public void setResponsibilities(List<String> responsibilities) {
    this.responsibilities = responsibilities;
  }

  public List<String> getRequiredSkills() {
    return requiredSkills;
  }

  public void setRequiredSkills(List<String> requiredSkills) {
    this.requiredSkills = requiredSkills;
  }

  public List<String> getPreferredSkills() {
    return preferredSkills;
  }

  public void setPreferredSkills(List<String> preferredSkills) {
    this.preferredSkills = preferredSkills;
  }
}
