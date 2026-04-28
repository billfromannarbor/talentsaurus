# Not currently parsed (or weakly parsed)

Track resume patterns that **do not parse well yet**, or fields we **do not extract** into the canonical model. When you add a private golden test and notice gaps, record them here so we can extend `ResumeTextParser` deliberately.

## Section headers / layout

- (Add bullet when a real resume uses a heading we do not recognize.)

## Dates and employment

- (e.g. numeric-only ranges, seasons, “to present” variants, overlapping roles.)

## Skills

- (e.g. inline “Skills:” line without a **Technical Skills** block, tables, icons-only PDFs.)

## Education

- (e.g. multiple degrees, expected graduation, non-US formats.)

## Contact / links

- (e.g. portfolio URLs, multiple emails, obscured contact lines.)

## PDF / text extraction

- (e.g. scanned images, two-column layout merging order, password-protected files.)

---

When you fix something in the parser, **move or delete** the corresponding bullet and update the golden `.json` for that resume.
