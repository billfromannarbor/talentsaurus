# Private example resumes (local only)

Put PDFs here for **your own** testing and validation. The repo root `.gitignore` ignores almost everything under this directory so **PDFs and golden JSON are not committed** (PII-safe).

**Tracked in git (for the team):** this `README.md` and [`NOTCURRENTLYPARSED.md`](NOTCURRENTLYPARSED.md).

## Golden files (expected parser output)

For each resume PDF, add a JSON file with the **same base name**:

| You add | You add |
|--------|--------|
| `JaneDoe.pdf` | `JaneDoe.json` |

The JSON must match the **canonical** shape produced by the Java resume parser (same structure as the `POST /api/v1/resume/parse` response). Use this as a template:

- [`../../services/resume-parser/src/test/resources/fixtures/golden-canonical.example.json`](../../services/resume-parser/src/test/resources/fixtures/golden-canonical.example.json)

**Capture an expected JSON quickly** (after you are happy with parser output):

```bash
cd services/resume-parser
./gradlew bootRun
# in another terminal:
curl -s -X POST "http://localhost:8081/api/v1/resume/parse" \
  -F "file=@../../examples/resumes-private/YourFile.pdf" | jq . > ../../examples/resumes-private/YourFile.json
```

Review and trim fields if you want a minimal golden; the test compares the **full** object tree.

## Run validation

From `services/resume-parser`:

```bash
./gradlew privateGoldenTests
```

This runs **only** when you opt in (it does **not** run on plain `./gradlew test`, so CI and other devs are not blocked by missing or mismatched private files).

If something in a resume is **not parsed well** (or not modeled in the canonical schema yet), document it in [`NOTCURRENTLYPARSED.md`](NOTCURRENTLYPARSED.md) instead of forcing a wrong golden.

## Synthetic fixtures (committed elsewhere)

- `apps/web/tests/fixtures/sample.pdf`
- `services/resume-parser/src/test/resources/fixtures/web-sample.pdf`
