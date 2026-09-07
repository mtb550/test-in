[Documentation](../README.md) › [Reports](main.md) › UC-REPORT-002

# UC-REPORT-002: Open the report that was just made

**As a** tester, **I want** to see the document straight away,
**so that** I can check it before sending it, without hunting through a folder.

There is no key for this. The link is on the message.

## Rules

- **Rule-REPORT-010** — The link is on the message that says the report was
  written, and the message stays in the IDE's notification list.
- **Rule-REPORT-011** — Clicking the link makes the message go.
- **Rule-REPORT-012** — The file is handed to whatever application on this
  machine claims it.

Rule-REPORT-001 to Rule-REPORT-005 hold everywhere. They are on
[the reports page](main.md#rules-that-hold-everywhere).

## Main flow

1. Testin writes the report and shows the message.
2. The tester clicks **Open report**.
3. The message goes.
4. The machine opens the file in whatever application claims that kind of file.

## What Testin refuses

**If the file is no longer there** — a message titled **Open Error** reads *The
file does not exist.*

**If this machine cannot open files that way** — a message titled **System
Error** reads *Opening a file is not supported on this system.*

**If the open fails** — a message titled **Execution Error** reads *Failed to
open the file:* and then the reason.

## Where the plugin breaks its own rules

**A web page report opens in an application, not in a browser.** The same kind
of file made as an export opens in the browser. One kind of file, two answers.
That is difference 6 on
[the reports page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Reports](main.md)
