package com.talentsaurus.resumeparser.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end parse of Illinois State career-center sample booklet pages (see {@code
 * examples/resumes/resume-samples-1.pdf}).
 */
@SpringBootTest
class ResumeSamplesPdfParserTest {

  @Autowired private ResumeParsingService parsingService;

  private static Path samplePdf() {
    return Path.of("")
        .toAbsolutePath()
        .resolve("../../examples/resumes/resume-samples-1.pdf")
        .normalize();
  }

  @Test
  void page3_undergraduateResume_extractsExperienceEducationAndName() throws Exception {
    Path pdf = samplePdf();
    assumeTrue(Files.exists(pdf), "Expected sample PDF at " + pdf);

    CanonicalResume r = parsingService.parse(Files.readAllBytes(pdf), 3, 3);

    assertThat(r.profile().fullName()).containsIgnoringCase("Reggie");
    assertThat(r.educations()).isNotEmpty();
    assertThat(r.experiences())
        .extracting(CanonicalResume.Experience::title)
        .anyMatch(t -> t.toLowerCase().contains("strength"));
    assertThat(r.experiences())
        .anyMatch(
            ex ->
                ex.description() != null
                    && ex.description().toLowerCase().contains("conditional"));
  }

  @Test
  void page4_companyFirstProfessionalLayout_parsesRoles() throws Exception {
    Path pdf = samplePdf();
    assumeTrue(Files.exists(pdf));

    CanonicalResume r = parsingService.parse(Files.readAllBytes(pdf), 4, 4);

    assertThat(r.profile().fullName()).containsIgnoringCase("Regina");
    assertThat(r.experiences())
        .anyMatch(ex -> ex.title().toLowerCase().contains("human resources"));
    assertThat(r.experiences())
        .anyMatch(ex -> ex.company().toLowerCase().contains("abc company"));
  }
}
