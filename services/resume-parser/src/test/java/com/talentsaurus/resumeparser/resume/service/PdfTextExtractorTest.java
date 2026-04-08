package com.talentsaurus.resumeparser.resume.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.talentsaurus.resumeparser.support.TestPdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfTextExtractorTest {

  private PdfTextExtractor extractor;

  @BeforeEach
  void setUp() {
    extractor = new PdfTextExtractor();
  }

  @Test
  void extractsTextFromGeneratedPdf() throws Exception {
    byte[] pdf = TestPdfFixtures.minimalCanonicalResumePdf();
    String text = extractor.extract(pdf);

    assertThat(text).contains("Jane Doe");
    assertThat(text).contains("Professional Summary");
    assertThat(text).contains("jane@example.com");
  }
}
