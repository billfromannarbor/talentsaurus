# Talentsaurus

Talentsaurus helps job seekers build the career they want from their skills, experience, and education. This repository is a **monorepo**: the product UI lives under `apps/`, and backend-style services live under `services/`.

## Layout

| Path | Description |
|------|-------------|
| [`apps/web`](apps/web) | Next.js (App Router) web app: resume PDF upload, profile review, Prisma + PostgreSQL persistence. |
| [`services/resume-parser`](services/resume-parser) | Spring Boot (Java) REST service: accepts a resume PDF and returns **canonical resume JSON** (Gradle Kotlin DSL). |
| [`examples/resumes-private`](examples/resumes-private) | **Local-only** PDFs for your own testing (gitignored contents; may contain PII). |

## Prerequisites

- **Web app:** Node.js and npm, plus a running **PostgreSQL** instance.
- **Resume parser:** **Java 21** (for example Homebrew `openjdk@21`).

## Web app (`apps/web`)

```bash
cd apps/web
cp .env.example .env   # set DATABASE_URL to your Postgres connection string
npm install
npm run db:push
npm run dev
```

Then open [http://localhost:3000](http://localhost:3000). Upload flow: `/upload` → review at `/profile/review`.

Useful scripts (see `apps/web/package.json`):

- `npm run build` — production build
- `npm run lint` — ESLint
- `npm run test:smoke:bill-resume` — parser smoke test against a local PDF path (optional)

## Resume parser service (`services/resume-parser`)

```bash
cd services/resume-parser
./gradlew bootRun
```

Default port: **8081** (see `application.properties`).

**Parse a PDF:**

```bash
curl -X POST "http://localhost:8081/api/v1/resume/parse" \
  -F "file=@/path/to/resume.pdf"
```

Response is JSON: `profile`, `skills`, `experiences`, `educations`, and `parseMeta` (sections detected, counts, warnings).

Run tests:

```bash
cd services/resume-parser
./gradlew test
```

More detail: [`services/resume-parser/README.md`](services/resume-parser/README.md).

## Local example resumes (not in git)

Use [`examples/resumes-private`](examples/resumes-private) for real or sensitive PDFs. Only `README.md` and `.gitignore` are tracked; any PDFs you drop there stay on your machine.

Automated tests use small **synthetic** fixtures under `apps/web/tests/fixtures/` and `services/resume-parser/src/test/resources/fixtures/` instead.

## Direction

The web app currently parses PDFs in-process for the first vertical slice. The resume-parser service is the home for **shared, testable parsing** and future REST boundaries as you grow more services.
