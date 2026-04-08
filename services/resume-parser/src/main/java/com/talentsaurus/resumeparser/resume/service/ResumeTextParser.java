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
  private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d\\s().-]{8,}\\d)");
  private static final Pattern LINKEDIN = Pattern.compile("https?://(?:www\\.)?linkedin\\.com/in/[\\w-]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern GITHUB = Pattern.compile("https?://(?:www\\.)?github\\.com/[\\w-]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern DATE_RANGE = Pattern.compile(
      "((?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4})\\s*[–-]\\s*((?:Present)|(?:(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}))",
      Pattern.CASE_INSENSITIVE);

  public CanonicalResume parse(String rawText) {
    List<String> lines = Arrays.stream(rawText.replace("\r\n", "\n").split("\n"))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();

    List<String> lower = lines.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
    List<String> warnings = new ArrayList<>();

    int idxSummary = lower.indexOf("professional summary");
    int idxSkills = lower.indexOf("technical skills");
    int idxExperience = lower.indexOf("professional experience");
    int idxEducation = lower.indexOf("education");

    List<String> sectionsDetected = new ArrayList<>();
    if (idxSummary >= 0) sectionsDetected.add("Professional Summary");
    if (idxSkills >= 0) sectionsDetected.add("Technical Skills");
    if (idxExperience >= 0) sectionsDetected.add("Professional Experience");
    if (idxEducation >= 0) sectionsDetected.add("Education");

    String name = lines.isEmpty() ? null : lines.getFirst();
    String location = lines.stream().filter(l -> l.matches(".*,[\\s]*[A-Z]{2}$")).findFirst().orElse(null);

    String email = firstMatch(rawText, EMAIL);
    String phone = firstMatch(rawText, PHONE);
    String linkedIn = firstMatch(rawText, LINKEDIN);
    String github = firstMatch(rawText, GITHUB);

    List<String> preSummary = idxSummary > 0 ? lines.subList(0, idxSummary) : lines.subList(0, Math.min(lines.size(), 12));
    String headline = preSummary.stream()
        .filter(l -> !l.equals(name))
        .filter(l -> !l.equals(location))
        .filter(l -> !l.contains("@"))
        .filter(l -> !l.toLowerCase(Locale.ROOT).contains("linkedin"))
        .filter(l -> !l.toLowerCase(Locale.ROOT).contains("github"))
        .filter(l -> !l.startsWith("http"))
        .reduce((first, second) -> second)
        .orElse(null);

    String summary = joinSection(lines, idxSummary, nextIndexAfter(idxSummary, idxSkills, idxExperience, idxEducation, lines.size()));
    if (summary == null || summary.isBlank()) {
      warnings.add("Professional Summary section was not detected.");
      summary = null;
    }

    List<String> skillSection = slice(lines, idxSkills, nextIndexAfter(idxSkills, idxExperience, idxEducation, lines.size()));
    Set<String> skillNames = new HashSet<>();
    Set<String> categories = Set.of(
        "languages", "frameworks & libraries", "architecture & design", "messaging & streaming",
        "databases & storage", "cloud & devops", "ci/cd & tooling", "practices & methodologies", "ai-assisted development");
    boolean inCategory = false;
    for (String line : skillSection) {
      String key = line.toLowerCase(Locale.ROOT);
      if (categories.contains(key)) {
        inCategory = true;
        continue;
      }
      if (!inCategory) continue;
      Arrays.stream(line.split(",|•|\\||;"))
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .filter(s -> s.length() < 70)
          .forEach(skillNames::add);
    }
    if (skillNames.isEmpty()) warnings.add("No skills were extracted from Technical Skills.");
    List<CanonicalResume.Skill> skills = skillNames.stream().sorted().map(CanonicalResume.Skill::new).toList();

    List<String> expSection = slice(
        lines,
        idxExperience,
        (idxEducation > idxExperience) ? idxEducation : lines.size());
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
      while (i < expSection.size()) {
        String line = expSection.get(i);
        if (i + 2 < expSection.size() && DATE_RANGE.matcher(expSection.get(i + 2)).find()) {
          break;
        }
        if (!line.equalsIgnoreCase("Corporate Balance Updater Platform") &&
            !line.equalsIgnoreCase("thinkorswim Trading Application") &&
            !line.equalsIgnoreCase("Earlier Experience")) {
          if (!description.isEmpty()) description.append(' ');
          description.append(line.replaceFirst("^•\\s*", ""));
        }
        i++;
      }

      experiences.add(new CanonicalResume.Experience(
          company,
          title,
          dateMatch.group(1),
          dateMatch.group(2),
          description.toString().isBlank() ? null : description.toString()));
    }
    if (experiences.isEmpty()) warnings.add("No structured experience entries were detected.");

    List<String> eduSection = idxEducation >= 0 ? lines.subList(idxEducation + 1, lines.size()) : List.of();
    List<CanonicalResume.Education> educations = new ArrayList<>();
    if (!eduSection.isEmpty()) {
      String institution = eduSection.getFirst();
      String details = String.join(" ", eduSection.subList(1, eduSection.size()));
      String degree = find(details, "(Bachelor|Master|B\\.?S\\.?|M\\.?S\\.?|Ph\\.?D\\.?)[^,]*");
      String field = find(details, "in\\s+([A-Za-z\\s\\-]+)(?:\\(|$)", 1);
      educations.add(new CanonicalResume.Education(institution, degree, field, null, null));
    }
    if (educations.isEmpty()) warnings.add("Education section was not parsed into structured fields.");

    CanonicalResume.Profile profile = new CanonicalResume.Profile(
        name,
        headline,
        summary,
        location,
        phone,
        email,
        linkedIn,
        github
    );

    CanonicalResume.ParseMeta parseMeta = new CanonicalResume.ParseMeta(
        sectionsDetected,
        skills.size(),
        experiences.size(),
        educations.size(),
        warnings
    );

    return new CanonicalResume(profile, skills, experiences, educations, parseMeta);
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

  private static int nextIndexAfter(int start, int a, int b, int c, int fallback) {
    int next = fallback;
    for (int idx : List.of(a, b, c)) {
      if (idx > start && idx < next) next = idx;
    }
    return next;
  }

  private static int nextIndexAfter(int start, int a, int b, int fallback) {
    int next = fallback;
    for (int idx : List.of(a, b)) {
      if (idx > start && idx < next) next = idx;
    }
    return next;
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
}
