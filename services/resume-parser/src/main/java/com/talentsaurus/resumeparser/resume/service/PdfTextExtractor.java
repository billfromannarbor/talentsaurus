package com.talentsaurus.resumeparser.resume.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

  /** Extracts all pages (1-based indexing is used only in {@link #extract(byte[], int, int)}). */
  public String extract(byte[] pdfBytes) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      return extractPages(document, 1, document.getNumberOfPages());
    }
  }

  /**
   * Extracts a page range for bundled PDFs (e.g. career-center booklets). Pages are {@code 1}-based,
   * inclusive on both ends.
   */
  public String extract(byte[] pdfBytes, int startPageOneBased, int endPageOneBased) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      int total = document.getNumberOfPages();
      if (total == 0) {
        return "";
      }
      int start = Math.clamp(startPageOneBased, 1, total);
      int end = Math.clamp(endPageOneBased, start, total);
      return extractPages(document, start, end);
    }
  }

  private static String extractPages(PDDocument document, int start, int end) throws IOException {
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);
    stripper.setStartPage(start);
    stripper.setEndPage(end);
    String text = stripper.getText(document);
    return text == null ? "" : new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
  }
}
