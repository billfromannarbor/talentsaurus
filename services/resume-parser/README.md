# Resume Parser Service

Java Spring Boot REST service that accepts a resume PDF and returns a canonical resume JSON document.

## Stack
- Java 21
- Spring Boot
- Gradle Kotlin DSL (`build.gradle.kts`)
- Apache PDFBox (PDF text extraction)

## Run
```bash
cd services/resume-parser
./gradlew bootRun
```

Service runs on `http://localhost:8081`.

## Parse Endpoint
`POST /api/v1/resume/parse`

Multipart form field:
- `file`: PDF file

Example:
```bash
curl -X POST "http://localhost:8081/api/v1/resume/parse" \
  -F "file=@/Users/bill/Desktop/Job/BillHeitzegResume.pdf"
```

## Response
Returns canonical resume JSON with:
- `profile`
- `skills`
- `experiences`
- `educations`
- `parseMeta` (detected sections, counts, warnings)

## Tests

```bash
cd services/resume-parser
./gradlew test
```

- **Unit:** `ResumeTextParserTest` — parses structured plain text (no PDF).
- **Unit:** `PdfTextExtractorTest` — extracts text from an in-memory PDF built by `TestPdfFixtures`.
- **Integration:** `ResumeParserApiIntegrationTest` — `POST /api/v1/resume/parse` with a generated PDF; asserts JSON shape and error responses for bad uploads.
- **Integration:** `WebSamplePdfIntegrationTest` — same endpoint with `src/test/resources/fixtures/web-sample.pdf` (same asset as `apps/web/tests/fixtures/sample.pdf`); asserts known profile fields from that fixture.
