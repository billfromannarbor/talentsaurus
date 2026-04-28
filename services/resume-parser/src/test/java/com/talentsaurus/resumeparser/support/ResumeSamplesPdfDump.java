package com.talentsaurus.resumeparser.support;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** Local utility — run main() to inspect resume-samples-1.pdf structure. */
public final class ResumeSamplesPdfDump {

  public static void main(String[] args) throws Exception {
    Path pdf =
        Path.of("../../examples/resumes/resume-samples-1.pdf")
            .toAbsolutePath()
            .normalize();
    if (!Files.exists(pdf)) {
      pdf = Path.of("examples/resumes/resume-samples-1.pdf").toAbsolutePath().normalize();
    }
    try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
      int total = doc.getNumberOfPages();
      System.out.println("Pages: " + total + " path=" + pdf);
      PDFTextStripper strip = new PDFTextStripper();
      strip.setSortByPosition(true);
      for (int p = 1; p <= total; p++) {
        strip.setStartPage(p);
        strip.setEndPage(p);
        String text = strip.getText(doc);
        String oneLine = text.replace('\n', '↵').trim();
        if (oneLine.length() > 200) {
          oneLine = oneLine.substring(0, 200) + "…";
        }
        System.out.println("--- PAGE " + p + " ---");
        System.out.println(text);
        System.out.println();
      }
    }
  }
}
