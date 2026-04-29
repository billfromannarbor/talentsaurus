package com.talentsaurus.matchmaking.matchmaking.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentsaurus.matchmaking.matchmaking.client.CandidateProfileClient;
import com.talentsaurus.matchmaking.matchmaking.client.PositionClient;
import com.talentsaurus.matchmaking.matchmaking.dto.CandidateProfileSnapshot;
import com.talentsaurus.matchmaking.matchmaking.dto.PositionSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class MatchmakingControllerIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void matchEndpointReturnsFitResponse() throws Exception {
    String body =
        """
        {
          "candidateId": "11111111-1111-1111-1111-111111111111",
          "positionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        }
        """;

    mockMvc
        .perform(post("/api/v1/matchmaking/match").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.overallScore").isNumber())
        .andExpect(jsonPath("$.recommendation").isString());
  }

  @TestConfiguration
  static class Stubs {
    @Bean
    @Primary
    CandidateProfileClient candidateProfileClient() {
      return id ->
          Optional.of(
              new CandidateProfileSnapshot(
                  UUID.fromString("11111111-1111-1111-1111-111111111111"),
                  "Jane",
                  "Java engineer",
                  List.of(new CandidateProfileSnapshot.Skill("Java")),
                  List.of(
                      new CandidateProfileSnapshot.Experience(
                          "Acme", "Engineer", "2020", "Present", "Built APIs")),
                  List.of(new CandidateProfileSnapshot.Education("State", "Bachelor", "CS", "2016", "2020"))));
    }

    @Bean
    @Primary
    PositionClient positionClient() {
      return id ->
          Optional.of(
              new PositionSnapshot(
                  UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                  "TopCo",
                  "Engineer",
                  List.of("Build APIs"),
                  List.of("Java"),
                  List.of("AWS")));
    }
  }
}
