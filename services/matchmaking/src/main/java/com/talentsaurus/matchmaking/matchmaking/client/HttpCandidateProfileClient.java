package com.talentsaurus.matchmaking.matchmaking.client;

import com.talentsaurus.matchmaking.matchmaking.dto.CandidateProfileSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpCandidateProfileClient implements CandidateProfileClient {

  private final RestClient restClient;

  public HttpCandidateProfileClient(
      @Value("${matchmaking.candidate-service-base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public Optional<CandidateProfileSnapshot> getCandidate(UUID id) {
    try {
      CandidateProfileSnapshot response =
          restClient
              .get()
              .uri("/api/v1/candidates/{id}", id)
              .retrieve()
              .body(CandidateProfileSnapshot.class);
      return Optional.ofNullable(response);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw ex;
    }
  }
}
