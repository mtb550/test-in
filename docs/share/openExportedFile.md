[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-004

# UC-SHARE-004: Open the exported file, or copy its path

**As a** tester, **I want** to check the file straight away,
**so that** I do not send somebody a spreadsheet with a column missing.

Every export ends with a message. The message can open the new file, or put
its full path on the clipboard.

There is no key for this. The links are on the message.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. A Git step that
  only reads can be canceled; one that is writing to the repository or to the
  remote cannot, because a push or a rebase stopped half way leaves the
  repository in a state nobody asked for. The export, import and report ones
  cannot be canceled either.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-022** — A web page export opens in the browser. Every other
  format is handed to whatever application claims it.
- **Rule-SHARE-023** — Clicking either link makes the message go.

## What the tester sees

No window opens. A message appears at the bottom right of the IDE, titled
**Exported** and carrying the file's name. Two links sit under it, **Open
file** and **Copy path**, and the message waits in the IDE's notification list
until one of them is clicked.

## Main flow

1. Testin writes the file.
2. A message titled **Exported** names the file.
3. The tester clicks **Open file**.
4. The spreadsheet opens in whatever application claims that kind of file.

**Copy path** puts the whole path on the clipboard instead.

## What Testin refuses

**If the file is not there** — a message titled **Open Error** reads *The file
does not exist.*

**If this machine cannot open files that way** — a message titled **System
Error** reads *Opening a file is not supported on this system.*

**If the open fails** — a message titled **Execution Error** reads *Failed to
open the file:* and then the reason.

## Where the plugin breaks its own rules

A web page made as an export opens in the browser. The same kind of file made
as a report is handed to an application instead. One kind of file, two answers.
That is difference 6 on
[the reports page](../report/main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
