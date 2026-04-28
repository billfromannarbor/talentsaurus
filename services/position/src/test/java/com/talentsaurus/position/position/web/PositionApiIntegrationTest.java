package com.talentsaurus.position.position.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PositionApiIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void createAndRetrievePositionById() throws Exception {
    String requestBody =
        """
        {
          "company": "Acme Corp",
          "title": "Senior Backend Engineer",
          "responsibilities": [
            "Design scalable APIs",
            "Mentor engineering team"
          ],
          "requiredSkills": [
            "Java",
            "Spring Boot",
            "SQL"
          ],
          "preferredSkills": [
            "Kubernetes",
            "AWS"
          ]
        }
        """;

    String response =
        mockMvc
            .perform(post("/api/v1/positions").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(get("/api/v1/positions/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.company").value("Acme Corp"))
        .andExpect(jsonPath("$.title").value("Senior Backend Engineer"))
        .andExpect(jsonPath("$.responsibilities[0]").value("Design scalable APIs"))
        .andExpect(jsonPath("$.requiredSkills[1]").value("Spring Boot"))
        .andExpect(jsonPath("$.preferredSkills[0]").value("Kubernetes"));
  }

  @Test
  void getMissingPositionReturns404() throws Exception {
    mockMvc
        .perform(get("/api/v1/positions/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Position not found"));
  }
}
