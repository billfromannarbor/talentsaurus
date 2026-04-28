package com.talentsaurus.resumeparser.support;

import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import com.talentsaurus.resumeparser.resume.service.PdfTextExtractor;
import com.talentsaurus.resumeparser.resume.service.ResumeTextParser;
import java.nio.file.Files;
import java.nio.file.Path;

/** Gradle {@code dumpPrivateResumeText} — inspect PDF text + parser output locally. */
public final class PrivateResumeTextDump {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: PrivateResumeTextDump <path-to.pdf>");
      System.exit(1);
    }
    Path pdf = Path.of(args[0]);
    byte[] bytes = Files.readAllBytes(pdf);
    PdfTextExtractor extractor = new PdfTextExtractor();
    String text = extractor.extract(bytes);
    System.out.println("===== EXTRACTED TEXT =====\n" + text + "\n");

    ResumeTextParser parser = new ResumeTextParser();
    CanonicalResume r = parser.parse(text);
    System.out.println("===== PROFILE =====");
    System.out.println(r.profile());
    System.out.println("===== SKILLS (" + r.skills().size() + ") =====");
    r.skills().forEach(s -> System.out.println("  - " + s.name()));
    System.out.println("===== EXPERIENCES (" + r.experiences().size() + ") =====");
    r.experiences()
        .forEach(
            e ->
                System.out.println(
                    "  - "
                        + e.title()
                        + " @ "
                        + e.company()
                        + " | "
                        + e.startDate()
                        + " - "
                        + e.endDate()));
    System.out.println("===== EDUCATIONS (" + r.educations().size() + ") =====");
    r.educations().forEach(e -> System.out.println("  - " + e.institution()));
    System.out.println("===== WARNINGS =====");
    r.parseMeta().warnings().forEach(w -> System.out.println("  - " + w));
  }
}
