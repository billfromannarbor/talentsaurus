package com.talentsaurus.resumeparser.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import com.talentsaurus.resumeparser.resume.service.ResumeParsingService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;

/**
 * For each {@code name.pdf} in {@code examples/resumes-private}, expects {@code name.json} with the
 * exact canonical parser output. Directory is configured via {@code talentsaurus.resumes-private}
 * (set from Gradle to the monorepo path).
 */
@SpringBootTest
@EnabledIfSystemProperty(
    named = "talentsaurus.run-private-goldens",
    matches = "true",
    disabledReason =
        "Set -Dtalentsaurus.run-private-goldens=true or run the privateGoldenTests Gradle task")
class PrivateResumeGoldenFilesTest {

  @Autowired private ResumeParsingService parsingService;

  /** Local mapper for reading golden JSON files (no need to match Spring HTTP bean). */
  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @TestFactory
  Stream<DynamicNode> privateResumeMatchesGoldenJson() throws IOException {
    String raw = System.getProperty("talentsaurus.resumes-private", "");
    if (!StringUtils.hasText(raw)) {
      return Stream.of(
          dynamicTest(
              "skipped — talentsaurus.resumes-private not set",
              () -> Assumptions.abort("System property talentsaurus.resumes-private not set")));
    }

    Path dir = Path.of(raw).toAbsolutePath().normalize();
    if (!Files.isDirectory(dir)) {
      return Stream.of(
          dynamicTest(
              "skipped — private resumes directory missing",
              () -> Assumptions.abort("Directory does not exist: " + dir)));
    }

    List<Path> pdfs =
        Files.list(dir)
            .filter(p -> Files.isRegularFile(p))
            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
            .sorted()
            .toList();

    if (pdfs.isEmpty()) {
      return Stream.of(
          dynamicTest(
              "skipped — no private PDFs",
              () ->
                  Assumptions.abort(
                      "No .pdf files in " + dir + " — add pairs of name.pdf + name.json to run goldens")));
    }

    List<DynamicNode> nodes = new ArrayList<>();
    for (Path pdf : pdfs) {
      String fileName = pdf.getFileName().toString();
      String base = fileName.substring(0, fileName.length() - ".pdf".length());
      Path golden = dir.resolve(base + ".json");
      nodes.add(
          dynamicTest(
              fileName + " matches " + base + ".json",
              () -> assertPdfMatchesGolden(pdf, golden, base)));
    }
    return nodes.stream();
  }

  private void assertPdfMatchesGolden(Path pdfPath, Path goldenPath, String base) throws Exception {
    assertThat(goldenPath)
        .withFailMessage(
            "Missing golden file for `%s`. Create `%s.json` in the same folder with the expected"
                + " parser output (see services/resume-parser/src/test/resources/fixtures/golden-canonical.example.json).",
            pdfPath.getFileName(),
            base)
        .exists();

    byte[] pdfBytes = Files.readAllBytes(pdfPath);
    MockMultipartFile multipart =
        new MockMultipartFile(
            "file",
            pdfPath.getFileName().toString(),
            "application/pdf",
            pdfBytes);

    CanonicalResume actual = parsingService.parse(multipart);

    CanonicalResume expected =
        objectMapper.readValue(goldenPath.toFile(), CanonicalResume.class);

    assertThat(actual)
        .usingRecursiveComparison()
        .withFailMessage(
            "Parser output differs from golden file. Update the parser or adjust `%s.json` if the"
                + " resume changed. Gaps in parser coverage should be noted in"
                + " examples/resumes-private/NOTCURRENTLYPARSED.md",
            base)
        .isEqualTo(expected);
  }
}
