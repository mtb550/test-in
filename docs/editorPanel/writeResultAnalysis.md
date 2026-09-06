[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-045

# UC-EDITOR-PANEL-045: Write the result analysis

**As a** tester, **I want** to say what the run as a whole showed,
**so that** the report carries my judgment and not only the figures.

There is no key for this. The button's tooltip reads **Result Analysis**.

## Rules

- **Rule 185** — The analysis can be written only once the test run is
  **Completed**.
- **Rule 186** — There are four sections, one for each verdict, each carrying
  its own count in the heading.
- **Rule 187** — A section left blank is not saved, and does not appear in the
  report.
- **Rule 188** — `Enter` in a box makes a new paragraph. **Save** is a button.
- **Rule 189** — The analysis appears in the PDF, Word and web reports. The
  spreadsheet report leaves it out.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Result Analysis                                             │
├──────────────────────────────────────────────────────────────┤
│  Passed (10)                                                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the passed cases say about this run...            │  │
│  └────────────────────────────────────────────────────────┘  │
│  Failed (2)                                                  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the failed cases say about this run...            │  │
│  └────────────────────────────────────────────────────────┘  │
│  Blocked (0)                                                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the blocked cases say about this run...           │  │
│  └────────────────────────────────────────────────────────┘  │
│  Untested (0)                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ what the untested cases say about this run...          │  │
│  └────────────────────────────────────────────────────────┘  │
│                                          [ Save ]            │
├──────────────────────────────────────────────────────────────┤
│  [k]  Tab Navigate       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **Each heading** — the verdict and how many test cases carry it.
2. **Each box** — three lines tall, with its own gray hint.
3. **Save** — a button, because `Enter` makes a paragraph here.

## Main flow

1. The tester finishes executing, and the test run becomes **Completed**.
2. The tester presses the **Result Analysis** button.
3. The dialog opens with four headings carrying the live counts.
4. The tester writes under **Failed**, explaining that both failures are the
   same defect.
5. The tester presses **Save**.
6. The test run is written to disk.
7. A message reads *Saved*.
8. The next report on this test run carries the section.

## What Testin refuses

**If the test run is not exactly Completed** — the button is gray. Its tooltip
reads *Result Analysis is written once the run is completed — it is*, then the
status. A **Closed** test run is refused too.

**If a section is left blank** — it is dropped, and the report leaves that
heading out.

---

[Documentation](../README.md) › [The editor panel](main.md)
