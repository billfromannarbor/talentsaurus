package com.talentsaurus.candidateprofile.candidate.web;

import com.talentsaurus.candidateprofile.candidate.dto.CandidateCreateResponse;
import com.talentsaurus.candidateprofile.candidate.dto.CandidateProfileRequest;
import com.talentsaurus.candidateprofile.candidate.dto.CandidateProfileResponse;
import com.talentsaurus.candidateprofile.candidate.service.CandidateProfileService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateProfileController {

  private final CandidateProfileService candidateProfileService;

  public CandidateProfileController(CandidateProfileService candidateProfileService) {
    this.candidateProfileService = candidateProfileService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CandidateCreateResponse> create(@Valid @RequestBody CandidateProfileRequest request) {
    UUID id = candidateProfileService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new CandidateCreateResponse(id));
  }

  @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getById(@PathVariable UUID id) {
    return candidateProfileService
        .get(id)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Candidate not found")));
  }
}
