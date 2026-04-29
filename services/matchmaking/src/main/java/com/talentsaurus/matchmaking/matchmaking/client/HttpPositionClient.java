package com.talentsaurus.matchmaking.matchmaking.client;

import com.talentsaurus.matchmaking.matchmaking.dto.PositionSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpPositionClient implements PositionClient {

  private final RestClient restClient;

  public HttpPositionClient(@Value("${matchmaking.position-service-base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public Optional<PositionSnapshot> getPosition(UUID id) {
    try {
      PositionSnapshot response =
          restClient.get().uri("/api/v1/positions/{id}", id).retrieve().body(PositionSnapshot.class);
      return Optional.ofNullable(response);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw ex;
    }
  }
}
