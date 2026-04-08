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
    String text = pdfTextExtractor.extract(file.getBytes());
    return resumeTextParser.parse(text);
  }
}
