[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-004

# UC-SHARE-004: Open the exported file, or copy its path

**As a** tester, **I want** to check the file straight away,
**so that** I do not send somebody a spreadsheet with a column missing.

There is no key for this. The links are on the message.

## Rules

- **Rule 21** — A web page export opens in the browser. Every other format is
  handed to whatever application claims it.
- **Rule 22** — Clicking either link makes the message go.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. Testin writes the file.
2. A message titled **Exported** names the file.
3. The tester clicks **Open file**.
4. The spreadsheet opens in whatever application claims it.

**Copy path** puts the whole path on the clipboard instead.

## What Testin refuses

**If the file is not there** — a message titled **Open Error** reads *The file
does not exist.*

**If this machine cannot open files that way** — a message titled **System
Error** reads *Opening a file is not supported on this system.*

**If the open fails** — a message titled **Execution Error** reads *Failed to
open the file:* and then the reason.

## Where the plugin breaks its own rules

A web page made as an export opens in the browser. The same kind of file made as
a report opens in an application instead. One kind of file, two answers. That is
difference 6 on [the reports page](../report/main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
