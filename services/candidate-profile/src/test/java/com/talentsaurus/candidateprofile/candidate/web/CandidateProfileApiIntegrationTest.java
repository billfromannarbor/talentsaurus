package com.talentsaurus.candidateprofile.candidate.web;

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
class CandidateProfileApiIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void createAndRetrieveCandidateById() throws Exception {
    String body =
        """
        {
          "fullName": "Jane Candidate",
          "email": "jane@example.com",
          "phone": "(555) 000-1111",
          "location": "Ann Arbor, MI",
          "summary": "Full-stack engineer",
          "skills": [{"name": "Java"}, {"name": "Spring Boot"}],
          "experiences": [
            {
              "company": "Acme Corp",
              "title": "Software Engineer",
              "startDate": "January 2022",
              "endDate": "Present",
              "description": "Built APIs"
            }
          ],
          "educations": [
            {
              "institution": "State University",
              "degree": "Bachelor of Science",
              "field": "Computer Science",
              "startDate": "2018",
              "endDate": "2022"
            }
          ],
          "references": [
            {
              "fullName": "Alex Manager",
              "relationship": "Manager",
              "email": "alex@example.com",
              "phone": "(555) 000-2222"
            }
          ]
        }
        """;

    String response =
        mockMvc
            .perform(post("/api/v1/candidates").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = response.replaceAll(".*\"id\"\s*:\s*\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(get("/api/v1/candidates/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.fullName").value("Jane Candidate"))
        .andExpect(jsonPath("$.skills[0].name").value("Java"))
        .andExpect(jsonPath("$.experiences[0].company").value("Acme Corp"))
        .andExpect(jsonPath("$.educations[0].institution").value("State University"))
        .andExpect(jsonPath("$.references[0].fullName").value("Alex Manager"));
  }

  @Test
  void getMissingCandidateReturns404() throws Exception {
    mockMvc
        .perform(get("/api/v1/candidates/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Candidate not found"));
  }
}
