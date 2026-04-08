# Private example resumes (local only)

Put PDFs you use for **manual testing** or **ad-hoc validation** here. The repo root `.gitignore` ignores everything under this directory except this `README.md`, so your files are never committed.

Suggested naming: `lastname-role.pdf` or whatever helps you, as long as it stays on your machine.

**Related**

- Committed, non-sensitive fixture: `apps/web/tests/fixtures/sample.pdf` and `services/resume-parser/src/test/resources/fixtures/web-sample.pdf` (synthetic content).
- Parser smoke test with your own file: from `apps/web`, run `npm run test:smoke:bill-resume -- /path/to/your.pdf`
