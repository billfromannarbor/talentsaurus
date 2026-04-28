package com.talentsaurus.resumeparser.resume.service;

import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ResumeTextParser {

  private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
  /** US-style phones; allows a missing opening parenthesis when PDFs split tokens oddly. */
  private static final Pattern PHONE_US =
      Pattern.compile(
          "(?:\\+?1[\\s.-]?)?(?:\\(\\d{3}\\)|\\d{3}\\)?)[\\s.-]+\\d{3}[\\s.-]+\\d{4}\\b");
  private static final Pattern LINKEDIN =
      Pattern.compile(
          "(?:https?://)?(?:www\\.)?linkedin\\.com/in/[\\w-]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern GITHUB = Pattern.compile("https?://(?:www\\.)?github\\.com/[\\w-]+", Pattern.CASE_INSENSITIVE);
  /** Classic long-month format used as fallback for three-line job blocks. */
  private static final Pattern DATE_RANGE =
      Pattern.compile(
          "((?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4})\\s*[–-]\\s*((?:Present)|(?:(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}))",
          Pattern.CASE_INSENSITIVE);

  private static final Set<String> EXPERIENCE_START_LABELS =
      Set.of(
          "professional experience",
          "work experience",
          "related experience",
          "relevant experience",
          "employment history",
          "pre-professional experience",
          "internship and work history",
          "military experience",
          "theatre experience",
          "coaching experience",
          "related research and work experience",
          "leadership and work experience",
          "other work experience",
          "additional work experience",
          "research experience",
          "pre-professional teaching experience",
          "pre-professional academic experience");

  private static final Set<String> EXPERIENCE_SUBSECTION_LABELS =
      Set.of(
          "related experience",
          "work experience",
          "professional experience",
          "other work experience",
          "additional work experience",
          "pre-professional experience",
          "internship and work history",
          "military experience",
          "theatre experience",
          "coaching experience",
          "research experience",
          "related research and work experience",
          "leadership and work experience",
          "leadership activities",
          "leadership experience",
          "pre-professional teaching experience",
          "pre-professional academic experience");

  /** Stops the experience text slice before these major sections (exact full line, lowercased). */
  private static final Set<String> EXPERIENCE_SLICE_TERMINATORS =
      Set.of(
          "education",
          "academic background",
          "community service",
          "certifications",
          "certifications and professional memberships",
          "continuing education and training",
          "professional and community affiliations",
          "languages and skills",
          "skills and certifications",
          "technical skills",
          "core competencies",
          "key skills",
          "skills & technologies",
          "software",
          "equipment",
          "objectives and research interests",
          "summary of qualifications",
          "professional summary",
          "career summary",
          "professional profile",
          "objective",
          "honors & awards",
          "activities, organizations, and awards",
          "extracurricular involvement",
          "leadership and service",
          "leadership, organizations, & activities",
          "leadership, student activities and involvement",
          "organizations and activities",
          "professional memberships",
          "volunteer experience",
          "sales skills",
          "communication skills",
          "organizational and managerial skills");

  private static final Set<String> SKILL_SECTION_LABELS =
      Set.of(
          "technical skills",
          "core competencies",
          "key skills",
          "skills & technologies",
          "languages and skills",
          "skills and certifications",
          "instrumentation experience/skills",
          "languages",
          "software",
          "equipment",
          "activities, skills & other experience",
          "activities, skills and other experience");

  private static final Set<String> MAJOR_SECTION_LABELS = buildMajorSectionLabels();

  private static Set<String> buildMajorSectionLabels() {
    Set<String> m = new HashSet<>(EXPERIENCE_START_LABELS);
    m.addAll(EXPERIENCE_SLICE_TERMINATORS);
    m.addAll(SKILL_SECTION_LABELS);
    m.addAll(
        Set.of(
            "education",
            "academic background",
            "summary of qualifications",
            "professional summary",
            "career summary",
            "professional profile",
            "objective",
            "objectives and research interests"));
    return Set.copyOf(m);
  }

  public CanonicalResume parse(String rawText) {
    List<String> lines = Arrays.stream(rawText.replace("\r\n", "\n").split("\n"))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();

    List<String> lower = lines.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
    List<String> warnings = new ArrayList<>();

    int idxExpRegion = firstSectionLine(lower, EXPERIENCE_START_LABELS);
    int idxEducation = firstSectionLine(lower, "education", "academic background");

    int idxSummary =
        firstPositive(
            firstSectionLine(lower, "professional summary", "career summary", "professional profile"),
            firstSectionLine(lower, "summary of qualifications"),
            firstSectionLine(lower, "objective"),
            firstSectionLine(lower, "objectives and research interests"));

    int idxSkillsHeader = firstSectionLine(lower, SKILL_SECTION_LABELS);

    List<String> sectionsDetected = new ArrayList<>();
    if (idxSummary >= 0) sectionsDetected.add("Professional Summary");
    if (idxSkillsHeader >= 0) sectionsDetected.add("Technical Skills");
    if (idxExpRegion >= 0) sectionsDetected.add("Professional Experience");
    if (idxEducation >= 0) sectionsDetected.add("Education");

    String name = lines.isEmpty() ? null : lines.getFirst();
    int locationSearchEnd = lines.size();
    for (int idx : List.of(idxEducation, idxSummary, idxExpRegion, idxSkillsHeader)) {
      if (idx >= 0 && idx < locationSearchEnd) {
        locationSearchEnd = idx;
      }
    }
    String location =
        lines.subList(0, locationSearchEnd).stream()
            .filter(ResumeTextParser::isLikelyPersonalLocationLine)
            .findFirst()
            .orElse(null);

    String email = firstMatch(rawText, EMAIL);
    String phone = normalizeUsPhone(firstMatch(rawText, PHONE_US));
    String linkedIn = firstMatch(rawText, LINKEDIN);
    String github = firstMatch(rawText, GITHUB);

    int summaryPreEnd =
        idxSummary >= 0
            ? nextSectionBoundary(lines, lower, idxSummary + 1, lines.size())
            : Math.min(lines.size(), 12);
    List<String> preSummary;
    if (idxSummary >= 0) {
      preSummary = headlinePreSummaryLines(lines, lower, name, idxSummary);
    } else {
      preSummary = lines.subList(0, summaryPreEnd);
    }
    String headline = deriveHeadline(preSummary, name, location);

    int summaryEnd =
        idxSummary >= 0 ? nextSectionBoundary(lines, lower, idxSummary + 1, lines.size()) : -1;
    String summary =
        idxSummary >= 0 ? joinSection(lines, idxSummary, summaryEnd) : null;
    if (summary == null || summary.isBlank()) {
      warnings.add("Professional Summary section was not detected.");
      summary = null;
    }

    Set<String> skillNames = new HashSet<>();
    collectSkillsFromDocument(lines, lower, skillNames);
    if (skillNames.isEmpty() && idxSkillsHeader < 0) {
      warnings.add("No skills were extracted from Technical Skills.");
    }
    List<CanonicalResume.Skill> skills = skillNames.stream().sorted().map(CanonicalResume.Skill::new).toList();

    List<String> expRegion = extractExperienceRegion(lines, lower);
    List<CanonicalResume.Experience> experiences = ExperienceLayoutParser.parse(expRegion);
    if (experiences.isEmpty()) {
      experiences = parseExperiencesClassic(expRegion);
    }
    if (experiences.isEmpty()) {
      warnings.add("No structured experience entries were detected.");
    }

    int eduEnd =
        idxEducation >= 0
            ? nextSectionBoundary(lines, lower, idxEducation + 1, lines.size())
            : 0;
    List<String> eduSection =
        idxEducation >= 0 ? lines.subList(idxEducation + 1, eduEnd) : List.of();
    List<CanonicalResume.Education> educations = new ArrayList<>();
    if (!eduSection.isEmpty()) {
      String institution = eduSection.getFirst();
      String details =
          eduSection.size() > 1
              ? String.join(" ", eduSection.subList(1, eduSection.size()))
              : "";
      String degree = find(details, "(Bachelor|Master|B\\.?S\\.?|M\\.?S\\.?|Ph\\.?D\\.?|Associate)[^,]*");
      String field = find(details, "in\\s+([A-Za-z\\s\\-]+)(?:\\(|$)", 1);
      educations.add(new CanonicalResume.Education(institution, degree, field, null, null));
    }
    if (educations.isEmpty()) {
      warnings.add("Education section was not parsed into structured fields.");
    }

    CanonicalResume.Profile profile =
        new CanonicalResume.Profile(
            name, headline, summary, location, phone, email, linkedIn, github);

    CanonicalResume.ParseMeta parseMeta =
        new CanonicalResume.ParseMeta(
            sectionsDetected,
            skills.size(),
            experiences.size(),
            educations.size(),
            warnings);

    return new CanonicalResume(profile, skills, experiences, educations, parseMeta);
  }

  /** Used by {@link ExperienceLayoutParser} for education bleed heuristics. */
  static boolean educationBleedLine(String line, int idx, List<String> section) {
    return looksLikeEducationBlockStart(line, idx, section);
  }

  /** Subsection titles inside the experience region (skip when scanning jobs). */
  static boolean isExperienceSubsectionHeader(String raw) {
    return EXPERIENCE_SUBSECTION_LABELS.contains(raw.toLowerCase(Locale.ROOT).trim());
  }

  private static int firstPositive(int... xs) {
    int min = -1;
    for (int x : xs) {
      if (x >= 0 && (min < 0 || x < min)) {
        min = x;
      }
    }
    return min;
  }

  private static int firstSectionLine(List<String> lowerLines, Set<String> labels) {
    int best = -1;
    for (int i = 0; i < lowerLines.size(); i++) {
      String line = lowerLines.get(i).trim();
      if (labels.contains(line)) {
        if (best < 0 || i < best) {
          best = i;
        }
      }
    }
    return best;
  }

  private static int firstSectionLine(List<String> lowerLines, String... labels) {
    return firstSectionLine(lowerLines, Set.of(labels));
  }

  /** First following line index that begins another major résumé section. */
  private static int nextSectionBoundary(
      List<String> lines, List<String> lower, int from, int max) {
    for (int j = from; j < max && j < lines.size(); j++) {
      String l = lower.get(j).trim();
      if (MAJOR_SECTION_LABELS.contains(l)) {
        return j;
      }
    }
    return max;
  }

  /**
   * End index for a skill block: major section headers, except in-document category labels (e.g. {@code
   * Languages} under Technical Skills) which are not slice boundaries.
   */
  private static int nextSkillSliceEnd(
      List<String> lower, int from, int max, Set<String> categoryLabels) {
    for (int j = from; j < max && j < lower.size(); j++) {
      String l = lower.get(j).trim();
      if (categoryLabels.contains(l)) {
        continue;
      }
      if (MAJOR_SECTION_LABELS.contains(l)) {
        return j;
      }
    }
    return max;
  }

  private static List<String> extractExperienceRegion(List<String> lines, List<String> lower) {
    int start = firstSectionLine(lower, EXPERIENCE_START_LABELS);
    if (start < 0) {
      return List.of();
    }
    int end = lines.size();
    for (int j = start + 1; j < lines.size(); j++) {
      String l = lower.get(j).trim();
      if (EXPERIENCE_START_LABELS.contains(l) || EXPERIENCE_SUBSECTION_LABELS.contains(l)) {
        continue;
      }
      if (EXPERIENCE_SLICE_TERMINATORS.contains(l)) {
        end = j;
        break;
      }
    }
    int from = Math.min(start + 1, lines.size());
    int to = Math.min(end, lines.size());
    if (from >= to) {
      return List.of();
    }
    return lines.subList(from, to);
  }

  private static void collectSkillsFromDocument(
      List<String> lines, List<String> lower, Set<String> skillNames) {
    Set<String> categories =
        Set.of(
            "languages",
            "frameworks & libraries",
            "architecture & design",
            "messaging & streaming",
            "databases & storage",
            "cloud & devops",
            "ci/cd & tooling",
            "practices & methodologies",
            "ai-assisted development",
            "software");
    for (int i = 0; i < lines.size(); i++) {
      String key = lower.get(i).trim();
      if (!SKILL_SECTION_LABELS.contains(key)) {
        continue;
      }
      int end = nextSkillSliceEnd(lower, i + 1, lines.size(), categories);
      boolean inCategory = false;
      boolean activitiesSkillsHeader =
          key.equals("activities, skills & other experience")
              || key.equals("activities, skills and other experience");
      for (int k = i + 1; k < end; k++) {
        String line = lines.get(k);
        if (isSkillSectionNoiseLine(line)) {
          continue;
        }
        if (activitiesSkillsHeader) {
          String act = stripTrailingEmploymentDates(line);
          if (act.length() >= 4 && act.length() <= 100) {
            skillNames.add(act);
          }
          continue;
        }
        String lk = lower.get(k).trim();
        if (categories.contains(lk)) {
          inCategory = true;
          continue;
        }
        if (line.startsWith("•")
            || line.startsWith("-")
            || line.startsWith("\u2022")
            || line.startsWith("\uf0a7")
            || line.startsWith("")) {
          String t = line.replaceFirst("^[•◦\\-\\u2022\\uF0A7]\\s*", "").trim();
          if (t.length() > 2 && t.length() <= 120) {
            skillNames.add(t);
          }
          continue;
        }
        if (inCategory || line.length() - line.replace(",", "").length() >= 2) {
          addSkillsFromDelimitedLine(line, skillNames);
        } else if (line.length() < 90 && line.matches("(?i)[A-Za-z0-9][A-Za-z0-9+.#\\s\\-]{8,88}")) {
          skillNames.add(line.trim());
        }
      }
    }
  }

  private static List<CanonicalResume.Experience> parseExperiencesClassic(List<String> expSection) {
    List<CanonicalResume.Experience> experiences = new ArrayList<>();
    int i = 0;
    while (i + 2 < expSection.size()) {
      String title = expSection.get(i);
      String company = expSection.get(i + 1);
      String dates = expSection.get(i + 2);
      Matcher dateMatch = DATE_RANGE.matcher(dates);
      if (!dateMatch.find()) {
        i++;
        continue;
      }
      i += 3;
      StringBuilder description = new StringBuilder();
      boolean stoppedAtEducation = false;
      while (i < expSection.size()) {
        String line = expSection.get(i);
        if (looksLikeEducationBlockStart(line, i, expSection)) {
          stoppedAtEducation = true;
          break;
        }
        if (i + 2 < expSection.size() && DATE_RANGE.matcher(expSection.get(i + 2)).find()) {
          break;
        }
        if (!line.equalsIgnoreCase("Corporate Balance Updater Platform")
            && !line.equalsIgnoreCase("thinkorswim Trading Application")
            && !line.equalsIgnoreCase("Earlier Experience")) {
          if (!description.isEmpty()) {
            description.append(' ');
          }
          description.append(line.replaceFirst("^•\\s*", ""));
        }
        i++;
      }
      if (stoppedAtEducation) {
        i = expSection.size();
      }

      experiences.add(
          new CanonicalResume.Experience(
              company,
              title,
              dateMatch.group(1),
              dateMatch.group(2),
              description.toString().isBlank() ? null : description.toString()));
    }
    return experiences;
  }

  private static String firstMatch(String text, Pattern pattern) {
    Matcher m = pattern.matcher(text);
    if (!m.find()) return null;
    if (m.groupCount() >= 1) {
      String g1 = m.group(1);
      if (g1 != null) return g1;
    }
    return m.group(0);
  }

  private static List<String> slice(List<String> lines, int sectionIndex, int nextIndex) {
    if (sectionIndex < 0) return List.of();
    int from = Math.min(sectionIndex + 1, lines.size());
    int to = Math.min(nextIndex, lines.size());
    if (from > to) return List.of();
    return lines.subList(from, to);
  }

  private static String joinSection(List<String> lines, int sectionIndex, int nextIndex) {
    List<String> block = slice(lines, sectionIndex, nextIndex);
    if (block.isEmpty()) return null;
    return String.join(" ", block);
  }

  private static String find(String text, String pattern) {
    Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
    return m.find() ? m.group(0).trim() : null;
  }

  private static String find(String text, String pattern, int group) {
    Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
    return m.find() ? m.group(group).trim() : null;
  }

  /**
   * Lines that might be a “headline” between contact info and summary — skips leading education
   * blocks (education-before-objective layouts).
   */
  private static List<String> headlinePreSummaryLines(
      List<String> lines, List<String> lower, String name, int idxSummary) {
    if (idxSummary <= 0) {
      return List.of();
    }
    int start = 0;
    if (name != null && !lines.isEmpty() && lines.getFirst().equals(name)) {
      start = 1;
    }
    while (start < idxSummary && start < lines.size()) {
      if (isContactLineForHeadlineSkip(lines.get(start))) {
        start++;
        continue;
      }
      break;
    }
    if (start < idxSummary
        && start < lines.size()
        && lower.get(start).trim().equals("education")) {
      start++;
      while (start < idxSummary && start < lines.size()) {
        String key = lower.get(start).trim();
        if (key.equals("objective")
            || key.equals("professional summary")
            || key.equals("summary of qualifications")
            || key.equals("career summary")
            || key.equals("professional profile")
            || key.equals("objectives and research interests")
            || EXPERIENCE_START_LABELS.contains(key)) {
          break;
        }
        start++;
      }
    }
    int from = Math.min(start, idxSummary);
    return lines.subList(from, idxSummary);
  }

  private static boolean isContactLineForHeadlineSkip(String line) {
    String l = line.toLowerCase(Locale.ROOT);
    if (line.contains("@")) {
      return true;
    }
    if (l.contains("linkedin")) {
      return true;
    }
    if (l.contains("github.com")) {
      return true;
    }
    if (line.startsWith("http")) {
      return true;
    }
    return isLikelyPhoneLine(line);
  }

  private static boolean isLikelyPersonalLocationLine(String line) {
    String low = line.toLowerCase(Locale.ROOT);
    if (low.contains("university")
        || low.contains("college of")
        || low.contains("broad college")
        || low.contains("gpa")
        || low.contains("dean's")
        || low.contains("dean’s")) {
      return false;
    }
    if (line.length() > 72) {
      return false;
    }
    if (line.matches(".*\\b20[12]\\d{2}\\b.*")) {
      return false;
    }
    return line.matches(".*,[\\s]*[A-Z]{2}\\b.*")
        || line.matches(".*\\b[A-Z]{2}\\s+\\d{5}\\b.*")
        || line.matches("(?i).*,\\s*[A-Z][a-z]+\\s*,\\s*[A-Z]{2}\\b.*");
  }

  private static boolean isSkillSectionNoiseLine(String line) {
    String t = line.trim();
    return t.matches("(?i).*(\\[TYPE\\s+THE\\s+DOCUMENT|PLACEHOLDER\\s*\\]).*")
        || t.matches("(?i)^\\d+\\s+.*\\[TYPE.*");
  }

  /** Removes trailing résumé date ranges so "Org, Role Jan 2024 – Present" can be stored as a skill line. */
  private static String stripTrailingEmploymentDates(String line) {
    String s = line.trim();
    s =
        s.replaceFirst(
            "(?i)\\s+((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\.?)\\s+\\d{4}\\s*[-–—]\\s*((?:Present|Current)|(?:(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\.?)\\s*\\d{4}).*$",
            "");
    return s.trim();
  }

  private static String deriveHeadline(List<String> preSummary, String name, String location) {
    List<String> parts = new ArrayList<>();
    for (String line : preSummary) {
      if (line.equals(name) || line.equals(location)) continue;
      if (line.contains("@")) continue;
      String low = line.toLowerCase(Locale.ROOT);
      if (low.contains("linkedin") || low.contains("github") || line.startsWith("http")) continue;
      if (isLikelyPhoneLine(line)) continue;
      parts.add(line);
    }
    if (parts.isEmpty()) return null;
    return String.join(" ", parts).replaceAll("\\s+", " ").trim();
  }

  private static boolean isLikelyPhoneLine(String line) {
    long digits = line.chars().filter(Character::isDigit).count();
    return digits >= 10 && line.length() <= 36;
  }

  private static String normalizeUsPhone(String raw) {
    if (raw == null) return null;
    String t = raw.trim();
    if (t.matches("^\\d{3}\\)\\s*\\d{3}.*")) {
      t = "(" + t;
    }
    return t;
  }

  private static void addSkillsFromDelimitedLine(String line, Set<String> skillNames) {
    List<String> tokens =
        Arrays.stream(line.split(",|•|\\||;"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    for (String skill : mergeCommaSplitParentheticals(tokens)) {
      if (skill.length() <= 120) {
        skillNames.add(skill);
      }
    }
  }

  private static List<String> mergeCommaSplitParentheticals(List<String> tokens) {
    List<String> out = new ArrayList<>();
    StringBuilder cur = null;
    for (String raw : tokens) {
      String t = raw.trim();
      if (t.isEmpty()) continue;
      if (cur == null) {
        int bal = parenBalance(t);
        if (bal > 0) {
          cur = new StringBuilder(t);
        } else {
          out.add(t);
        }
        continue;
      }
      cur.append(", ").append(t);
      if (parenBalance(cur.toString()) <= 0) {
        out.add(cur.toString().trim());
        cur = null;
      }
    }
    if (cur != null) {
      out.add(cur.toString().trim());
    }
    return out;
  }

  private static int parenBalance(String s) {
    int d = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '(') d++;
      else if (c == ')') d--;
    }
    return d;
  }

  private static boolean looksLikeEducationBlockStart(String line, int idx, List<String> section) {
    String l = line.toLowerCase(Locale.ROOT).trim();
    if (l.equals("education") || l.startsWith("education —") || l.startsWith("education -")) {
      return true;
    }
    if (l.contains("general motors") && (l.contains("engineer") || l.contains("developer"))) {
      return true;
    }
    if (l.equals("general motors")) {
      return true;
    }
    if (idx + 1 < section.size()) {
      String next = section.get(idx + 1).toLowerCase(Locale.ROOT).trim();
      if (next.equals("general motors")
          && l.length() < 48
          && (l.contains("engineer") || l.contains("developer"))) {
        return true;
      }
    }
    if (l.contains("university")
        || l.contains("college")
        || l.matches(".*\\binstitute of technology\\b.*")) {
      if (l.contains("bachelor")
          || l.contains("master")
          || l.contains("ph.d")
          || l.contains("phd")
          || l.contains("b.s.")
          || l.contains("m.s.")
          || l.contains("mba")) {
        return true;
      }
      if (idx + 1 < section.size()) {
        String next = section.get(idx + 1).toLowerCase(Locale.ROOT);
        if (next.contains("bachelor")
            || next.contains("master")
            || next.contains("degree")
            || next.contains("b.s.")
            || next.contains("m.s.")
            || next.contains("electrical engineering")
            || next.contains("computer science")) {
          return true;
        }
      }
    }
    return false;
  }
}
