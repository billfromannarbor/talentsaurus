package com.talentsaurus.resumeparser.resume.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentsaurus.resumeparser.support.TestPdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ResumeParserApiIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void parseReturnsCanonicalJsonForValidPdf() throws Exception {
    byte[] pdf = TestPdfFixtures.minimalCanonicalResumePdf();
    MockMultipartFile file =
        new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf);

    mockMvc
        .perform(multipart("/api/v1/resume/parse").file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profile.fullName").value("Jane Doe"))
        .andExpect(jsonPath("$.profile.email").value("jane@example.com"))
        .andExpect(jsonPath("$.skills.length()").value(2))
        .andExpect(jsonPath("$.experiences.length()").value(1))
        .andExpect(jsonPath("$.experiences[0].company").value("Acme Corp"))
        .andExpect(jsonPath("$.educations.length()").value(1))
        .andExpect(jsonPath("$.parseMeta.experienceCount").value(1))
        .andExpect(jsonPath("$.parseMeta.warnings.length()").value(0));
  }

  @Test
  void parseRejectsNonPdf() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

    mockMvc
        .perform(multipart("/api/v1/resume/parse").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Only PDF files are supported"));
  }

  @Test
  void parseRejectsEmptyFile() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

    mockMvc
        .perform(multipart("/api/v1/resume/parse").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("File is empty"));
  }
}
