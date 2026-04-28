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

  @Test
  void normalizesPhoneWhenOpeningParenMissingFromPdf() {
    String raw =
        """
        Jane Doe
        Ann Arbor, MI
        734) 555-0100
        jane@example.com
        Professional Summary
        Short summary.
        Technical Skills
        Languages
        Java
        Professional Experience
        Engineer
        Acme
        June 2020 - Present
        • Work
        Education
        State U
        Bachelor of Science
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.profile().phone()).startsWith("(");
    assertThat(r.profile().phone()).contains("734");
  }

  @Test
  void joinsMultiLineHeadlineBeforeSummary() {
    String raw =
        """
        Jane Doe
        Ann Arbor, MI
        (734) 555-0100
        jane@example.com
        Senior Software Engineer
        (Java, Kotlin, Microservices)
        Professional Summary
        Backend engineer.
        Technical Skills
        Languages
        Java
        Professional Experience
        Engineer
        Acme
        June 2020 - Present
        • Work
        Education
        State U
        Bachelor of Science
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.profile().headline()).contains("Senior Software Engineer");
    assertThat(r.profile().headline()).contains("Microservices");
  }

  @Test
  void mergesSkillsSplitAcrossParentheticalCommas() {
    String raw =
        """
        Jane Doe
        Professional Summary
        Summary.
        Technical Skills
        Cloud & DevOps
        AWS (EC2, ELB, S3, CloudFormation), Docker
        Professional Experience
        Engineer
        Acme
        June 2020 - Present
        • Work
        Education
        State U
        Bachelor of Science
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.skills())
        .extracting(CanonicalResume.Skill::name)
        .anyMatch(s -> s.contains("EC2") && s.contains("CloudFormation"));
    assertThat(r.skills()).extracting(CanonicalResume.Skill::name).contains("Docker");
  }

  @Test
  void parsesEmployerCityStateOnLineWithTitleBelowAndActivitiesSkillSection() {
    String raw =
        """
        Grey Rupert
        • (248) 555-0100 • grey@msu.edu
        EDUCATION
        Michigan State University, East Lansing, MI May 2026
        Bachelor of Arts, Marketing
        OBJECTIVE
        • Marketing graduate.
        PROFESSIONAL EXPERIENCE
        Big Ten Network, East Lansing, MI Aug 2025-May 2026
        Brand Ambassador
        • Ran events.
        ACTIVITIES, SKILLS & OTHER EXPERIENCE
        MSU Club, Member Jan 2024 – Present
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.profile().headline()).isNull();
    assertThat(r.profile().location()).isNull();
    assertThat(r.experiences().getFirst().title()).isEqualTo("Brand Ambassador");
    assertThat(r.experiences().getFirst().company()).contains("Big Ten Network");
    assertThat(r.skills()).extracting(CanonicalResume.Skill::name).anyMatch(s -> s.contains("MSU Club"));
    assertThat(r.parseMeta().warnings()).isEmpty();
  }

  @Test
  void parsesCompanyLineBeforeTitleWithAbbrevMonthDates() {
    String raw =
        """
        Regina R. Redbird
        SUMMARY OF QUALIFICATIONS
        • Bullet one.
        PROFESSIONAL EXPERIENCE
        ABC Company, Schaumburg IL
        Human Resources Associate Director Nov 2019 – Present
        • Manage personnel.
        EDUCATION
        Illinois State University, Normal IL
        Bachelor of Science in Business Administration
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.experiences()).isNotEmpty();
    CanonicalResume.Experience ex = r.experiences().getFirst();
    assertThat(ex.company()).containsIgnoringCase("ABC Company");
    assertThat(ex.title().toLowerCase()).contains("human resources");
    assertThat(ex.startDate().toLowerCase()).contains("nov");
  }

  @Test
  void parsesTitleCommaEmployerWithDatesOnSameLine() {
    String raw =
        """
        Maggie Redbird
        mm@example.com
        Education
        Illinois State University, Normal IL
        Bachelor of Science
        Related Experience
        Human Resources Assistant Intern, Amazon May 2021 - July 2021
        Waukegan, IL
        • Led new hire orientation.
        EDUCATION
        Illinois State University, Normal IL
        Bachelor of Science
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.experiences()).hasSize(1);
    assertThat(r.experiences().getFirst().title().toLowerCase()).contains("intern");
    assertThat(r.experiences().getFirst().company().toLowerCase()).contains("amazon");
    assertThat(r.experiences().getFirst().description()).containsIgnoringCase("orientation");
  }

  @Test
  void dropsCaptionBleedWhenEngineerLinePrecedesGeneralMotorsLine() {
    String raw =
        """
        Jane Doe
        Professional Summary
        Summary.
        Technical Skills
        Languages
        Java
        Professional Experience
        Founder
        Co
        June 2000 - January 2008
        • Consulting work.
        Electrical Engineer
        General Motors
        Education
        State U
        Bachelor of Science
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.experiences().getFirst().description()).doesNotContain("General Motors");
    assertThat(r.experiences().getFirst().description()).doesNotContain("Electrical Engineer");
  }

  @Test
  void stopsExperienceDescriptionWhenEducationBlockAppearsInStreamOrder() {
    String raw =
        """
        Jane Doe
        Professional Summary
        Summary.
        Technical Skills
        Languages
        Java
        Professional Experience
        Founder
        Emerald Software
        June 2000 - January 2008
        • Consulting work.
        Kettering University
        Bachelor of Science in Electrical Engineering
        Education
        Kettering University
        Bachelor of Science in Electrical Engineering
        """;

    CanonicalResume r = parser.parse(raw);

    assertThat(r.experiences()).hasSize(1);
    assertThat(r.experiences().getFirst().description()).doesNotContain("Kettering");
    assertThat(r.experiences().getFirst().description()).doesNotContain("Bachelor");
    assertThat(r.educations().getFirst().institution()).contains("Kettering");
  }
}
