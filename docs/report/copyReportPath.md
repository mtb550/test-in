[Documentation](../README.md) › [Reports](main.md) › UC-REPORT-003

# UC-REPORT-003: Copy the report's path

**As a** tester, **I want** the full path of the report on my clipboard,
**so that** I can paste it into a ticket or a chat message.

There is no key for this. The link is on the message.

## Rules

- **Rule 13** — The whole path is copied, not the file name.
- **Rule 14** — Clicking the link makes the message go.
- **Rule 15** — The same link is offered on every message about a file Testin
  wrote, so the gesture is the same for a report and for an export.

Rules 1 to 5 hold everywhere. They are on
[the reports page](main.md#rules-that-hold-everywhere).

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
