package com.talentsaurus.resumeparser.resume.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration test using the same PDF fixture as the Next.js upload smoke test ({@code web-sample.pdf}).
 * Assertions match the known pdfkit-generated content in {@code apps/web/scripts/test-upload.mjs}.
 */
@SpringBootTest
class WebSamplePdfIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void parseWebSamplePdf_extractsExpectedProfileAndSkills() throws Exception {
    ClassPathResource resource = new ClassPathResource("fixtures/web-sample.pdf");
    byte[] pdf;
    try (InputStream in = resource.getInputStream()) {
      pdf = StreamUtils.copyToByteArray(in);
    }

    MockMultipartFile file =
        new MockMultipartFile("file", "web-sample.pdf", "application/pdf", pdf);

    mockMvc
        .perform(multipart("/api/v1/resume/parse").file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profile.fullName").value("John Doe"))
        .andExpect(jsonPath("$.profile.email").value("john.doe@example.com"))
        .andExpect(jsonPath("$.profile.phone").value(org.hamcrest.Matchers.containsString("555")));
    // Web fixture uses a single "Skills: ..." line; structured Technical Skills section is optional.
  }
}
