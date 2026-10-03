[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-012

# UC-SETTING-012: Put my company's logo on reports

**As a** tester, **I want** the reports I hand over to carry my company's logo,
**so that** they read as my team's document, not a tool's printout.

There is no key for this. It is the **Company logo** row.

## Rules

- **Rule-SETTING-001** — One page for the whole IDE. Every code project open in
  it reads the same values.
- **Rule-SETTING-004** — Only a changed Testin folder makes Testin read the disk
  again. Every other setting is read where it is used, when it is used.
- **Rule-SETTING-005** — A password is never on this page. Testin asks for none:
  a Git remote's credentials are kept by Git's own credential helper.
- **Rule-SETTING-006** — Nothing on this page has a key of its own.
- **Rule-SETTING-043** — The web page, PDF and Word reports print the logo in
  their top-left corner, above the title, 30 points tall and in its own
  proportions. The spreadsheet carries none.
- **Rule-SETTING-044** — With no file set, a missing file or one that is not a
  picture, a report has no logo and says nothing about it. The report is never
  refused for the logo's sake.

## The screen

The row sits under **Default download folder**.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Company logo:  [ C:\Users\muteb\Pictures\logo.png    ] [...]            │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The box** — the picture file the reports print. Empty means no logo.
2. **The browse button** — opens a file chooser.

## Main flow

1. The tester presses the browse button on the **Company logo** row.
2. The file chooser opens.
3. The tester picks a picture and confirms.
4. The tester presses **Apply**.
5. The next report made as a web page, a PDF or a Word document opens with the
   logo in its top-left corner, above the title.

## What Testin refuses

Nothing. A file that is missing, or is not a picture, is stored as typed, and
the reports leave the logo out.

---

[Documentation](../README.md) › [The settings page](main.md)
