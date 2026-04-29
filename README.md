# Talentsaurus

Talentsaurus helps job seekers build the career they want from their skills, experience, and education. This repository is a **monorepo**: the product UI lives under `apps/`, and backend-style services live under `services/`.

## Layout

| Path | Description |
|------|-------------|
| [`apps/web`](apps/web) | Next.js (App Router) web app: resume PDF upload, profile review, Prisma + PostgreSQL persistence. |
| [`services/resume-parser`](services/resume-parser) | Spring Boot (Java) REST service: accepts a resume PDF and returns **canonical resume JSON** (Gradle Kotlin DSL). |
| [`services/candidate-profile`](services/candidate-profile) | Spring Boot (Java) REST service: stores and retrieves candidate profiles in SQL by unique ID. |
| [`services/position`](services/position) | Spring Boot (Java) REST service: stores and retrieves job/position descriptions by unique ID. |
| [`services/matchmaking`](services/matchmaking) | Spring Boot (Java) REST service: fetches candidate + position and returns a fit score/explanation. |
| [`examples/resumes-private`](examples/resumes-private) | **Local-only** PDFs for your own testing (gitignored contents; may contain PII). |

## Prerequisites

- **Web app:** Node.js and npm, plus a running **PostgreSQL** instance.
- **Resume parser:** **Java 21** (for example Homebrew `openjdk@21`).
- **Candidate profile service:** **Java 21** (uses H2 SQL DB by default).
- **Position service:** **Java 21** (uses H2 SQL DB by default).
- **Matchmaking service:** **Java 21** (calls candidate-profile + position services).

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

Optional page-range parsing for bundled PDFs (1-based, inclusive):

```bash
curl -X POST "http://localhost:8081/api/v1/resume/parse?startPage=3&endPage=3" \
  -F "file=@/path/to/bundled-resumes.pdf"
```

Response is JSON: `profile`, `skills`, `experiences`, `educations`, and `parseMeta` (sections detected, counts, warnings).

Run tests:

```bash
cd services/resume-parser
./gradlew test
```

More detail: [`services/resume-parser/README.md`](services/resume-parser/README.md).

## Candidate profile service (`services/candidate-profile`)

```bash
cd services/candidate-profile
./gradlew bootRun
```

Default port: **8082** (see `application.properties`).

Endpoints:
- `POST /api/v1/candidates` — create candidate profile; returns unique `id`.
- `GET /api/v1/candidates/{id}` — retrieve candidate profile by ID.

Create candidate example:

```bash
curl -X POST "http://localhost:8082/api/v1/candidates" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Candidate",
    "email": "jane@example.com",
    "phone": "(555) 000-1111",
    "location": "Ann Arbor, MI",
    "summary": "Full-stack engineer",
    "skills": [{"name": "Java"}, {"name": "Spring Boot"}],
    "experiences": [{
      "company": "Acme Corp",
      "title": "Software Engineer",
      "startDate": "January 2022",
      "endDate": "Present",
      "description": "Built APIs"
    }],
    "educations": [{
      "institution": "State University",
      "degree": "Bachelor of Science",
      "field": "Computer Science",
      "startDate": "2018",
      "endDate": "2022"
    }],
    "references": [{
      "fullName": "Alex Manager",
      "relationship": "Manager",
      "email": "alex@example.com",
      "phone": "(555) 000-2222"
    }]
  }'
```

Retrieve candidate by ID:

```bash
curl "http://localhost:8082/api/v1/candidates/<candidate-id>"
```

Run tests:

```bash
cd services/candidate-profile
./gradlew test
```

## Position service (`services/position`)

```bash
cd services/position
./gradlew bootRun
```

Default port: **8083** (see `application.properties`).

Endpoints:
- `POST /api/v1/positions` — create a position description; returns unique `id`.
- `GET /api/v1/positions/{id}` — retrieve position description by ID.

Create position example:

```bash
curl -X POST "http://localhost:8083/api/v1/positions" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "Acme Corp",
    "title": "Senior Backend Engineer",
    "responsibilities": [
      "Design scalable APIs",
      "Mentor engineering team"
    ],
    "requiredSkills": [
      "Java",
      "Spring Boot",
      "SQL"
    ],
    "preferredSkills": [
      "Kubernetes",
      "AWS"
    ]
  }'
```

Retrieve position by ID:

```bash
curl "http://localhost:8083/api/v1/positions/<position-id>"
```

Run tests:

```bash
cd services/position
./gradlew test
```

## Matchmaking service (`services/matchmaking`)

```bash
cd services/matchmaking
./gradlew bootRun
```

Default port: **8084** (see `application.properties`).

Depends on:
- `candidate-profile` at `http://localhost:8082`
- `position` at `http://localhost:8083`

Endpoint:
- `POST /api/v1/matchmaking/match` — computes fit score from candidate + position IDs.

Match example:

```bash
curl -X POST "http://localhost:8084/api/v1/matchmaking/match" \
  -H "Content-Type: application/json" \
  -d '{
    "candidateId": "11111111-1111-1111-1111-111111111111",
    "positionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
  }'
```

Run tests:

```bash
cd services/matchmaking
./gradlew test
```

## Manual testing checklist

### Resume parser (`8081`)
- Start service: `cd services/resume-parser && ./gradlew bootRun`
- Happy path: upload a valid PDF to `POST /api/v1/resume/parse`.
- Page-range path: call with `?startPage=N&endPage=M`.
- Error path:
  - send non-PDF file -> expect `400` + `"Only PDF files are supported"`
  - send empty PDF part -> expect `400` + `"File is empty"`
  - send only one of `startPage` / `endPage` -> expect `400`.

### Candidate profile (`8082`)
- Start service: `cd services/candidate-profile && ./gradlew bootRun`
- Happy path:
  - `POST /api/v1/candidates` with JSON body -> expect `201` and `id`.
  - `GET /api/v1/candidates/{id}` -> expect stored payload.
- Error path:
  - random/nonexistent UUID -> expect `404` + `"Candidate not found"`.

### Position service (`8083`)
- Start service: `cd services/position && ./gradlew bootRun`
- Happy path:
  - `POST /api/v1/positions` with JSON body -> expect `201` and `id`.
  - `GET /api/v1/positions/{id}` -> expect stored payload.
- Error path:
  - random/nonexistent UUID -> expect `404` + `"Position not found"`.

### Matchmaking service (`8084`)
- Start dependencies first:
  - `cd services/candidate-profile && ./gradlew bootRun`
  - `cd services/position && ./gradlew bootRun`
  - `cd services/matchmaking && ./gradlew bootRun`
- Happy path:
  - `POST /api/v1/matchmaking/match` with candidate + position IDs -> expect `200` + score/recommendation.
- Error path:
  - missing candidate ID -> expect `404` + `"Candidate not found"`.
  - missing position ID -> expect `404` + `"Position not found"`.

## Local example resumes (not in git)

Use [`examples/resumes-private`](examples/resumes-private) for real or sensitive PDFs. Only `README.md` and `.gitignore` are tracked; any PDFs you drop there stay on your machine.

Automated tests use small **synthetic** fixtures under `apps/web/tests/fixtures/` and `services/resume-parser/src/test/resources/fixtures/` instead.

To validate your own PDF + expected JSON pairs locally, use `./gradlew privateGoldenTests` from `services/resume-parser` (see `examples/resumes-private/README.md`).

## Direction

The web app currently parses PDFs in-process for the first vertical slice. The resume-parser service is the home for **shared, testable parsing** and future REST boundaries as you grow more services.
