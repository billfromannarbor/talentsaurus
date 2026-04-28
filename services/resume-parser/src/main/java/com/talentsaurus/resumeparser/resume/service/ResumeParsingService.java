package com.talentsaurus.resumeparser.resume.service;

import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeParsingService {

  private final PdfTextExtractor pdfTextExtractor;
  private final ResumeTextParser resumeTextParser;

  public ResumeParsingService(PdfTextExtractor pdfTextExtractor, ResumeTextParser resumeTextParser) {
    this.pdfTextExtractor = pdfTextExtractor;
    this.resumeTextParser = resumeTextParser;
  }

  public CanonicalResume parse(MultipartFile file) throws IOException {
    return parse(file.getBytes(), null, null);
  }

  /**
   * @param startPageOneBased inclusive first page, or {@code null} for full document
   * @param endPageOneBased inclusive last page, or {@code null} for full document
   */
  public CanonicalResume parse(byte[] pdfBytes, Integer startPageOneBased, Integer endPageOneBased)
      throws IOException {
    if ((startPageOneBased == null) != (endPageOneBased == null)) {
      throw new IllegalArgumentException("startPage and endPage must both be set or both null");
    }
    String text =
        startPageOneBased == null
            ? pdfTextExtractor.extract(pdfBytes)
            : pdfTextExtractor.extract(pdfBytes, startPageOneBased, endPageOneBased);
    return resumeTextParser.parse(text);
  }
}
