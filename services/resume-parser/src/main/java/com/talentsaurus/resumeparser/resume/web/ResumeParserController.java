package com.talentsaurus.resumeparser.resume.web;

import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import com.talentsaurus.resumeparser.resume.service.ResumeParsingService;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resume")
@Validated
public class ResumeParserController {

  private final ResumeParsingService resumeParsingService;

  public ResumeParserController(ResumeParsingService resumeParsingService) {
    this.resumeParsingService = resumeParsingService;
  }

  @PostMapping(path = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> parse(@RequestPart("file") @NotNull MultipartFile file) throws IOException {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
    }

    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    boolean looksLikePdf = contentType.contains("pdf") || filename.endsWith(".pdf");
    if (!looksLikePdf) {
      return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are supported"));
    }

    CanonicalResume resume = resumeParsingService.parse(file);
    return ResponseEntity.ok(resume);
  }
}
