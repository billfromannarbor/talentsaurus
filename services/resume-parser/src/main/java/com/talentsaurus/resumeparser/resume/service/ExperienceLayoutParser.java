package com.talentsaurus.resumeparser.resume.service;

import com.talentsaurus.resumeparser.resume.dto.CanonicalResume;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts jobs from heterogeneous résumé layouts (company/title/date permutations and inline dates).
 */
final class ExperienceLayoutParser {

  private static final String MO =
      "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\.?";

  /** Month–month year (e.g. September-December 2021). */
  private static final Pattern MONTH_SPAN_ONE_YEAR =
      Pattern.compile(
          "(?i)\\b(" + MO + "\\s*[-–—]\\s*" + MO + "\\s+\\d{4})\\b");

  /** Standard start – end ranges including Present/Current and season tokens. */
  private static final Pattern MONTH_YEAR_RANGE =
      Pattern.compile(
          "(?i)\\b("
              + MO
              + "\\s+\\d{4}"
              + "|(?:Spring|Summer|Fall|Winter|Summers?)\\s+\\d{4}(?:\\s*-\\s*\\d{4})?"
              + ")\\s*[-–—]\\s*("
              + "Present|Current"
              + "|"
              + MO
              + "\\s+\\d{4}"
              + "|(?:Spring|Summer|Fall|Winter|Summers?)\\s+\\d{4}(?:\\s*-\\s*\\d{4})?"
              + ")");

  private ExperienceLayoutParser() {}

  static List<CanonicalResume.Experience> parse(List<String> lines) {
    List<CanonicalResume.Experience> out = new ArrayList<>();
    int n = lines.size();
    int i = 0;
    while (i < n) {
      String line = lines.get(i);
      if (isTemplateBoilerplate(line)) {
        i++;
        continue;
      }
      if (isLikelySectionHeader(line) || ResumeTextParser.isExperienceSubsectionHeader(line)) {
        i++;
        continue;
      }

      DateHit hit = findPrimaryDateHit(line);
      if (hit == null) {
        i++;
        continue;
      }

      String before = line.substring(0, hit.start).trim();
      boolean dateOnlyLine = before.isEmpty() && isMostlyJustDateLine(line, hit);

      String title;
      String company;
      int descFrom;

      if (dateOnlyLine) {
        if (i < 2) {
          i++;
          continue;
        }
        title = lines.get(i - 2).trim();
        company = lines.get(i - 1).trim();
        if (isBulletLine(title)
            || isLikelySectionHeader(title)
            || ResumeTextParser.isExperienceSubsectionHeader(title)) {
          i++;
          continue;
        }
        descFrom = i + 1;
      } else {
        String next = i + 1 < n ? lines.get(i + 1).trim() : "";
        if (isOrgCityStateLineBeforeDates(before) && isPlausibleStandaloneTitleLine(next)) {
          company = before.trim();
          title = next;
          descFrom = i + 2;
        } else {
          TitleCompanySplit split = splitTitleAndCompanyFromRoleLine(before);
          title = split.title();
          if (title.isEmpty()) {
            i++;
            continue;
          }
          String prev = i > 0 ? lines.get(i - 1).trim() : "";
          boolean prevOrg = looksLikeOrganizationLine(prev);
          boolean nextOrg = looksLikeOrganizationLine(next) && !isBulletLine(next);
          if (split.companyTail() != null) {
            company = split.companyTail();
            if (nextOrg && next.length() < 55 && !prevOrg) {
              company = company + ", " + next;
              descFrom = i + 2;
            } else {
              descFrom = i + 1;
            }
          } else if (prevOrg && !prev.isEmpty()) {
            company = prev;
            descFrom = i + 1;
          } else if (nextOrg) {
            company = next;
            descFrom = i + 2;
          } else {
            company = prev.isEmpty() ? "" : prev;
            descFrom = i + 1;
          }
        }
      }

      StringBuilder description = new StringBuilder();
      int j = descFrom;
      while (j < n) {
        String lj = lines.get(j);
        if (ResumeTextParser.educationBleedLine(lj, j, lines)) {
          break;
        }
        if (isLikelySectionHeader(lj)) {
          break;
        }
        DateHit nextHit = findPrimaryDateHit(lj);
        if (nextHit != null) {
          String nb = lj.substring(0, nextHit.start).trim();
          if (!nb.isEmpty() && !isBulletLine(lj)) {
            break;
          }
        }
        if (!description.isEmpty()) {
          description.append(' ');
        }
        description.append(stripLeadingBullet(lj));
        j++;
      }

      out.add(
          new CanonicalResume.Experience(
              company.isBlank() ? "" : company,
              title,
              hit.startDate,
              hit.endDate,
              description.toString().isBlank() ? null : description.toString()));
      i = j;
    }
    return out;
  }

  private record DateHit(String startDate, String endDate, int start, int end) {}

  private record TitleCompanySplit(String title, String companyTail) {}

  /**
   * Handles "Role, Employer Month YYYY – …" (common in college résumés): last comma-separated segment
   * before the dates is often the employer name.
   */
  private static TitleCompanySplit splitTitleAndCompanyFromRoleLine(String before) {
    String b = before.trim();
    int lastComma = b.lastIndexOf(',');
    if (lastComma <= 0 || lastComma >= b.length() - 1) {
      return new TitleCompanySplit(sanitizeTitleFragment(b), null);
    }
    String tail = b.substring(lastComma + 1).trim();
    String head = b.substring(0, lastComma).trim();
    if (head.length() < 4 || tail.isEmpty()) {
      return new TitleCompanySplit(sanitizeTitleFragment(b), null);
    }
    int tailWords = tail.split("\\s+").length;
    if (tail.length() > 48 || tailWords > 6) {
      return new TitleCompanySplit(sanitizeTitleFragment(b), null);
    }
    if (!tail.chars().anyMatch(Character::isLetter)) {
      return new TitleCompanySplit(sanitizeTitleFragment(b), null);
    }
    // "…, East Lansing, MI" → tail "MI" is a state, not an employer
    if (tail.matches("(?i)^[A-Z]{2}$")) {
      return new TitleCompanySplit(sanitizeTitleFragment(b), null);
    }
    return new TitleCompanySplit(sanitizeTitleFragment(head), tail);
  }

  /**
   * "Employer, City, ST … dates" with the job title on the following line (common student résumé
   * layout).
   */
  private static boolean isOrgCityStateLineBeforeDates(String before) {
    String b = before.trim();
    long commas = b.chars().filter(ch -> ch == ',').count();
    if (commas < 2) {
      return false;
    }
    return b.matches(
        "(?i).+,\\s*[A-Za-z][A-Za-z\\s'.-]{0,72},\\s*[A-Z]{2}\\s*$");
  }

  private static boolean isPlausibleStandaloneTitleLine(String line) {
    if (line.isEmpty() || isBulletLine(line) || line.length() > 90) {
      return false;
    }
    DateHit dh = findPrimaryDateHit(line);
    if (dh != null && line.substring(0, dh.start).trim().length() > 35) {
      return false;
    }
    return true;
  }

  private static boolean isTemplateBoilerplate(String line) {
    String t = line.trim();
    return t.matches("(?i).*(\\[TYPE\\s+THE\\s+DOCUMENT|PLACEHOLDER\\s*\\]).*")
        || t.matches("(?i)^\\d+\\s+.*\\[TYPE.*");
  }

  private static DateHit findPrimaryDateHit(String line) {
    Matcher m1 = MONTH_YEAR_RANGE.matcher(line);
    if (m1.find()) {
      return new DateHit(m1.group(1).trim(), m1.group(2).trim(), m1.start(), m1.end());
    }
    Matcher m2 = MONTH_SPAN_ONE_YEAR.matcher(line);
    if (m2.find()) {
      String span = m2.group(1).trim();
      return new DateHit(span, span, m2.start(), m2.end());
    }
    return null;
  }

  private static boolean isMostlyJustDateLine(String line, DateHit hit) {
    String rest = (line.substring(0, hit.start) + line.substring(hit.end)).trim();
    return rest.isEmpty() || rest.matches("^[|•\\s,.-]+$");
  }

  private static String sanitizeTitleFragment(String before) {
    return before.replaceAll("[|,]+$", "").trim();
  }

  private static boolean looksLikeOrganizationLine(String line) {
    if (line.isEmpty() || isBulletLine(line)) {
      return false;
    }
    if (line.length() > 120) {
      return false;
    }
    if (line.contains(",")) {
      return true;
    }
    return line.matches("(?i).+\\b(IL|IN|WI|MI|OH|CA|NY|TX|FL|GA|PA|WA|CO|AZ|MN|MO|IA|KS|KY|TN|NC|SC|AL|MS|LA|AR|OK|NE|ND|SD|MT|WY|UT|NV|NM|ID|OR|ME|NH|VT|MA|RI|CT|NJ|DE|MD|DC|VA|WV|HI|AK|WY|USA)\\b.*");
  }

  private static boolean isBulletLine(String s) {
    if (s.isEmpty()) {
      return false;
    }
    String t = s.trim();
    char c = t.charAt(0);
    return c == '•'
        || c == '◦'
        || c == '-'
        || c == '\u2022'
        || c == '\uf0a7'
        || t.startsWith("");
  }

  private static String stripLeadingBullet(String s) {
    return s.replaceFirst("^[•◦\\-\\u2022\\uF0A7]\\s*", "").trim();
  }

  private static boolean isLikelySectionHeader(String line) {
    String t = line.trim();
    if (t.length() < 3 || t.length() > 64) {
      return false;
    }
    if (!t.equals(t.toUpperCase(Locale.ROOT))) {
      return false;
    }
    return t.chars().anyMatch(Character::isLetter);
  }
}
