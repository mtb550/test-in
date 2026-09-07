[Documentation](../README.md) › [Reports](main.md) › UC-REPORT-003

# UC-REPORT-003: Copy the report's path

**As a** tester, **I want** the full path of the report on my clipboard,
**so that** I can paste it into a ticket or a chat message.

There is no key for this. The link is on the message.

## Rules

- **Rule-REPORT-001** — A report is about exactly one test run.
- **Rule-REPORT-002** — Every format reads the same figures, so two reports on
  one test run cannot disagree.
- **Rule-REPORT-003** — A report is written after the dialog closes, under a
  progress bar that cannot be canceled.
- **Rule-REPORT-004** — A report is written where the tester chose. It never
  lands under the Testin folder.
- **Rule-REPORT-005** — The project named in a report is the test project, not
  the code project the IDE has open.
- **Rule-REPORT-013** — The whole path is copied, not the file name.
- **Rule-REPORT-014** — Clicking the link makes the message go.
- **Rule-REPORT-015** — The same link is offered on every message about a file
  Testin wrote, so the gesture is the same for a report and for an export.

## Main flow

1. Testin writes the report and shows the message.
2. The tester clicks **Copy path**.
3. The whole path goes on the clipboard.
4. The message goes.

## What Testin refuses

Nothing. There is no way for it to fail, and no confirmation is raised.

## If the message has already gone

The message stays in the IDE's notification list, so it can be found again
there. If it has been cleared, the path can be read from the folder the report
was written to, which is the one the report dialog named.

---

[Documentation](../README.md) › [Reports](main.md)
