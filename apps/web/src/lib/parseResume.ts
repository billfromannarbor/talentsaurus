export type ParsedResumeDraft = {
  profile: {
    fullName?: string | null;
    headline?: string | null;
    summary?: string | null;
    location?: string | null;
    phone?: string | null;
    email?: string | null;
    linkedInUrl?: string | null;
  };
  skills: { name: string }[];
  experiences: { company: string; title?: string | null; startDate?: string | null; endDate?: string | null; description?: string | null }[];
  educations: { institution: string; degree?: string | null; field?: string | null; startDate?: string | null; endDate?: string | null }[];
  parseMeta?: {
    sectionsDetected: string[];
    skillCount: number;
    experienceCount: number;
    educationCount: number;
    warnings: string[];
  };
};

export function parseResumeText(raw: string): ParsedResumeDraft {
  const lines = raw
    .replace(/\r\n/g, "\n")
    .split("\n")
    .map((l) => l.trim())
    .filter(Boolean);

  const lowerLines = lines.map((l) => l.toLowerCase());
  const warnings: string[] = [];

  const email = raw.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-z]{2,}/i)?.[0] ?? null;
  const phone = raw.match(/(\+?\d[\d\s().-]{8,}\d)/)?.[1] ?? null;
  const linkedIn = raw.match(/https?:\/\/(?:www\.)?linkedin\.com\/in\/[\w-]+/i)?.[0] ?? null;
  const github = raw.match(/https?:\/\/(?:www\.)?github\.com\/[\w-]+/i)?.[0] ?? null;

  const idxSummary = lowerLines.findIndex((l) => l === "professional summary");
  const idxSkills = lowerLines.findIndex((l) => l === "technical skills");
  const idxExperience = lowerLines.findIndex((l) => l === "professional experience");
  const idxEducation = lowerLines.findIndex((l) => l === "education");

  const sectionsDetected = [
    idxSummary >= 0 ? "Professional Summary" : null,
    idxSkills >= 0 ? "Technical Skills" : null,
    idxExperience >= 0 ? "Professional Experience" : null,
    idxEducation >= 0 ? "Education" : null,
  ].filter(Boolean) as string[];

  const name = lines[0] ?? null;
  const location =
    lines.find((l) => /,\s*[A-Z]{2}$/.test(l) || /\b[A-Z][a-z]+,\s*[A-Z]{2}\b/.test(l)) ??
    null;
  const preSummary = idxSummary > 0 ? lines.slice(0, idxSummary) : lines.slice(0, 12);
  const nonContactTop = preSummary.filter(
    (l) =>
      l !== name &&
      l !== location &&
      !/@/.test(l) &&
      !/https?:\/\//i.test(l) &&
      !/linkedin|github/i.test(l) &&
      !/\(\d{3}\)/.test(l),
  );
  let headline: string | null = null;
  if (nonContactTop.length > 0) {
    const last = nonContactTop[nonContactTop.length - 1];
    const prev = nonContactTop[nonContactTop.length - 2];
    if (prev && prev.includes("(") && !prev.includes(")") && last.includes(")")) {
      headline = `${prev} ${last}`.replace(/\s+/g, " ").trim();
    } else if (last && /engineer|developer|lead|architect|manager/i.test(last)) {
      headline = last;
    } else {
      headline = last;
    }
  }

  const summaryLines =
    idxSummary >= 0
      ? lines.slice(
          idxSummary + 1,
          [idxSkills, idxExperience, idxEducation].filter((i) => i > idxSummary).sort((a, b) => a - b)[0] ?? lines.length,
        )
      : [];
  const summary = summaryLines.join(" ").trim() || null;
  if (!summary) warnings.push("Professional Summary section was not detected.");

  // Technical skills parsing by category lines + wrapped values.
  const skillSection =
    idxSkills >= 0
      ? lines.slice(
          idxSkills + 1,
          [idxExperience, idxEducation].filter((i) => i > idxSkills).sort((a, b) => a - b)[0] ?? lines.length,
        )
      : [];

  const categoryNames = new Set([
    "languages",
    "frameworks & libraries",
    "architecture & design",
    "messaging & streaming",
    "databases & storage",
    "cloud & devops",
    "ci/cd & tooling",
    "practices & methodologies",
    "ai-assisted development",
  ]);
  const skillChunks: string[] = [];
  let currentCategory: string | null = null;
  for (const line of skillSection) {
    const key = line.toLowerCase();
    if (categoryNames.has(key)) {
      currentCategory = key;
      continue;
    }
    if (currentCategory) {
      skillChunks.push(line);
    }
  }
  const skillSet = new Set<string>();
  for (const chunk of skillChunks) {
    for (const piece of chunk.split(/,|•|\||;/)) {
      const s = piece.trim();
      if (!s) continue;
      if (s.length > 60) continue;
      skillSet.add(s);
    }
  }
  if (github) skillSet.add("GitHub");
  const skills = Array.from(skillSet).map((name) => ({ name }));
  if (skills.length === 0) warnings.push("No skills were extracted from Technical Skills.");

  // Experience parsing: Title, Company, Date line, then bullet lines.
  const expSection =
    idxExperience >= 0
      ? lines.slice(
          idxExperience + 1,
          [idxEducation].filter((i) => i > idxExperience).sort((a, b) => a - b)[0] ?? lines.length,
        )
      : [];
  const experiences: ParsedResumeDraft["experiences"] = [];
  const isDateLine = (l: string) =>
    /(?:january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{4}\s*[–-]\s*(?:present|(?:january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{4})/i.test(
      l,
    );
  const looksLikeSectionHeader = (l: string) =>
    /^(corporate balance updater platform|thinkorswim trading application|earlier experience)$/i.test(
      l,
    );

  let i = 0;
  while (i < expSection.length) {
    const title = expSection[i];
    if (!title || looksLikeSectionHeader(title)) {
      i += 1;
      continue;
    }
    const company = expSection[i + 1] ?? "";
    const dates = expSection[i + 2] ?? "";
    if (!company || !isDateLine(dates)) {
      i += 1;
      continue;
    }
    const dateMatch =
      dates.match(
        /((?:january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{4})\s*[–-]\s*((?:present)|(?:(?:january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{4}))/i,
      ) ?? [];
    const bullets: string[] = [];
    i += 3;
    while (i < expSection.length) {
      const line = expSection[i];
      if (!line) {
        i += 1;
        continue;
      }
      if (looksLikeSectionHeader(line)) {
        i += 1;
        continue;
      }
      if (
        i + 2 < expSection.length &&
        expSection[i + 1] &&
        isDateLine(expSection[i + 2])
      ) {
        break;
      }
      bullets.push(line.replace(/^•\s*/, "").trim());
      i += 1;
    }

    experiences.push({
      title,
      company,
      startDate: dateMatch[1] ?? null,
      endDate: dateMatch[2] ?? null,
      description: bullets.join(" "),
    });
  }
  if (experiences.length === 0) warnings.push("No structured experience entries were detected.");

  // Education parsing.
  const eduSection = idxEducation >= 0 ? lines.slice(idxEducation + 1) : [];
  const educations: ParsedResumeDraft["educations"] = [];
  if (eduSection.length > 0) {
    const institution = eduSection[0] ?? "";
    const details = eduSection.slice(1).join(" ");
    const degree =
      details.match(/(Bachelor|Master|B\.?S\.?|M\.?S\.?|Ph\.?D\.?)[^,]*/i)?.[0] ??
      null;
    const field =
      details.match(/in\s+([A-Za-z\s\-]+)(?:\(|$)/i)?.[1]?.trim() ??
      null;
    if (institution) {
      educations.push({
        institution,
        degree,
        field,
        startDate: null,
        endDate: null,
      });
    }
  }
  if (educations.length === 0) warnings.push("Education section was not parsed into structured fields.");

  return {
    profile: {
      fullName: name,
      headline,
      summary,
      location,
      phone,
      email,
      linkedInUrl: linkedIn ?? null,
    },
    skills,
    experiences,
    educations,
    parseMeta: {
      sectionsDetected,
      skillCount: skills.length,
      experienceCount: experiences.length,
      educationCount: educations.length,
      warnings,
    },
  };
}
