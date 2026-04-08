package com.talentsaurus.resumeparser.resume.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResumeTextParserTest {

  private ResumeTextParser parser;

  @BeforeEach
  void setUp() {
    parser = new ResumeTextParser();
  }

  @Test
  void parsesStructuredResumeText() {
    String raw =
        """
        Jane Doe
        Ann Arbor, MI
        (734) 555-0100
        jane@example.com
        https://www.linkedin.com/in/janedoe
        Software Engineer
        Professional Summary
        Backend engineer with focus on APIs.
        Technical Skills
        Languages
        Java, Kotlin
        Professional Experience
        Senior Engineer
        Acme Corp
        June 2020 - Present
        • Built REST APIs
        Education
        State University
        Bachelor of Science in Computer Science
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.profile().fullName()).isEqualTo("Jane Doe");
    assertThat(r.profile().location()).isEqualTo("Ann Arbor, MI");
    assertThat(r.profile().email()).isEqualTo("jane@example.com");
    assertThat(r.profile().phone()).contains("734");
    assertThat(r.profile().linkedInUrl()).containsIgnoringCase("linkedin.com/in/janedoe");
    assertThat(r.profile().summary()).contains("Backend engineer");
    assertThat(r.profile().headline()).contains("Software Engineer");

    assertThat(r.skills()).extracting(CanonicalResume.Skill::name).contains("Java", "Kotlin");

    assertThat(r.experiences()).hasSize(1);
    CanonicalResume.Experience ex = r.experiences().getFirst();
    assertThat(ex.company()).isEqualTo("Acme Corp");
    assertThat(ex.title()).isEqualTo("Senior Engineer");
    assertThat(ex.startDate()).contains("June");
    assertThat(ex.endDate()).isEqualToIgnoringCase("Present");
    assertThat(ex.description()).contains("REST");

    assertThat(r.educations()).hasSize(1);
    assertThat(r.educations().getFirst().institution()).isEqualTo("State University");

    assertThat(r.parseMeta().sectionsDetected())
        .contains(
            "Professional Summary",
            "Technical Skills",
            "Professional Experience",
            "Education");
    assertThat(r.parseMeta().warnings()).isEmpty();
  }

  @Test
  void emptyTextProducesWarnings() {
    CanonicalResume r = parser.parse("   \n  ");
    assertThat(r.profile().fullName()).isNull();
    assertThat(r.parseMeta().warnings()).isNotEmpty();
  }
}
