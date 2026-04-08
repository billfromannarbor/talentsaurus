import fs from "node:fs";
import path from "node:path";
import pdfParse from "pdf-parse";
import { parseResumeText } from "../src/lib/parseResume.ts";

function fail(message: string): never {
  throw new Error(message);
}

function assert(condition: unknown, message: string): void {
  if (!condition) fail(message);
}

async function main() {
  const inputArg = process.argv[2];
  const resumePath =
    inputArg && inputArg.trim().length > 0
      ? path.resolve(inputArg)
      : "/Users/bill/Desktop/Job/BillHeitzegResume.pdf";

  if (!fs.existsSync(resumePath)) {
    fail(`Resume file not found: ${resumePath}`);
  }

  const fileBuffer = fs.readFileSync(resumePath);
  const pdf = await pdfParse(fileBuffer);
  const rawText = (pdf.text ?? "").trim();
  assert(rawText.length > 1000, "Extracted PDF text is unexpectedly short.");

  const parsed = parseResumeText(rawText);
  const skills = parsed.skills.map((s) => s.name.toLowerCase());
  const companies = parsed.experiences.map((e) => e.company.toLowerCase());
  const institution = parsed.educations[0]?.institution?.toLowerCase() ?? "";

  assert(
    (parsed.profile.fullName ?? "").toLowerCase().includes("bill heitzeg"),
    `Expected fullName to contain "Bill Heitzeg", got: ${parsed.profile.fullName ?? "<empty>"}`,
  );
  assert(
    (parsed.profile.email ?? "").toLowerCase() === "bill.heitzeg@gmail.com",
    `Expected email bill.heitzeg@gmail.com, got: ${parsed.profile.email ?? "<empty>"}`,
  );
  assert(
    (parsed.profile.phone ?? "").includes("734"),
    `Expected phone to include area code 734, got: ${parsed.profile.phone ?? "<empty>"}`,
  );
  assert(
    (parsed.profile.linkedInUrl ?? "").toLowerCase().includes("linkedin.com/in/billheitzeg"),
    `Expected LinkedIn URL with /in/billheitzeg, got: ${parsed.profile.linkedInUrl ?? "<empty>"}`,
  );
  assert(
    (parsed.profile.headline ?? "").toLowerCase().includes("software engineer"),
    `Expected headline to include "software engineer", got: ${parsed.profile.headline ?? "<empty>"}`,
  );

  assert(parsed.skills.length >= 20, `Expected >= 20 skills, got ${parsed.skills.length}`);
  assert(skills.some((s) => s.includes("java")), "Expected a Java skill.");
  assert(skills.some((s) => s.includes("spring boot")), "Expected a Spring Boot skill.");
  assert(skills.some((s) => s.includes("postgresql")), "Expected a PostgreSQL skill.");
  assert(skills.some((s) => s.includes("aws")), "Expected an AWS skill.");

  assert(
    parsed.experiences.length >= 5,
    `Expected >= 5 experiences, got ${parsed.experiences.length}`,
  );
  assert(
    companies.some((c) => c.includes("charles schwab")),
    "Expected Charles Schwab / TD Ameritrade experience.",
  );
  assert(
    companies.some((c) => c.includes("domino")),
    "Expected Domino's Pizza experience.",
  );

  assert(parsed.educations.length >= 1, "Expected at least 1 education entry.");
  assert(
    institution.includes("kettering"),
    `Expected Kettering education, got: ${parsed.educations[0]?.institution ?? "<empty>"}`,
  );

  const sections = parsed.parseMeta?.sectionsDetected ?? [];
  assert(sections.includes("Professional Summary"), "Expected Professional Summary section.");
  assert(sections.includes("Technical Skills"), "Expected Technical Skills section.");
  assert(sections.includes("Professional Experience"), "Expected Professional Experience section.");
  assert(sections.includes("Education"), "Expected Education section.");

  console.log("PASS: Bill resume smoke test");
  console.log(
    JSON.stringify(
      {
        fullName: parsed.profile.fullName,
        skills: parsed.skills.length,
        experiences: parsed.experiences.length,
        educations: parsed.educations.length,
        warnings: parsed.parseMeta?.warnings ?? [],
      },
      null,
      2,
    ),
  );
}

main().catch((err) => {
  console.error(`FAIL: ${err instanceof Error ? err.message : String(err)}`);
  process.exit(1);
});
