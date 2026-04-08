package com.talentsaurus.resumeparser.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Deterministic PDF bytes for integration tests (text extraction + parsing end-to-end).
 */
public final class TestPdfFixtures {

  private TestPdfFixtures() {}

  /**
   * Minimal resume layout matching {@code ResumeTextParser} section headers and experience pattern.
   */
  public static byte[] minimalCanonicalResumePdf() throws IOException {
    String[] lines = {
        "Jane Doe",
        "Ann Arbor, MI",
        "(734) 555-0100",
        "jane@example.com",
        "https://www.linkedin.com/in/janedoe",
        "Software Engineer",
        "Professional Summary",
        "Backend engineer with focus on APIs and reliability.",
        "Technical Skills",
        "Languages",
        "Java, Kotlin",
        "Professional Experience",
        "Senior Engineer",
        "Acme Corp",
        "June 2020 - Present",
        "• Built REST APIs",
        "Education",
        "State University",
        "Bachelor of Science in Computer Science"
    };

    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage();
      doc.addPage(page);
      PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      float x = 50;
      float y = 750;
      float leading = 14;
      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(x, y);
        for (String line : lines) {
          cs.showText(line);
          cs.newLineAtOffset(0, -leading);
        }
        cs.endText();
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      doc.save(baos);
      return baos.toByteArray();
    }
  }
}
