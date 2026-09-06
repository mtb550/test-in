[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-040

# UC-EDITOR-PANEL-040: Change the failure details on their own

**As a** tester, **I want** to add to what I wrote about a failure,
**so that** I can paste the error in after I have found it, without touching the
verdict.

`F2` on the failed test case.

## Rules

- **Rule 163** — The verdict is not touched. Only the four fields are written.
- **Rule 164** — The entry works on exactly one test case, and only when its
  verdict is **Failed**.
- **Rule 165** — The message comes after the test run is written, so an edit
  that was dropped never reports itself as saved.
- **Rule 166** — The dialog is the same one `F` opens, filled in with what is
  there.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. A test case is recorded as **Failed**.
2. The tester finds the exception in the log and copies it.
3. The tester selects the test case and presses `F2`.
4. The **Failed Test Case Details** dialog opens with what was already written.
5. The tester pastes the error into the big box and presses `Enter`.
6. The four fields are written. The verdict stays **Failed**.
7. The test run is written to disk.
8. A message reads *Details updated*.

## What Testin refuses

**If nothing is selected, or more than one thing is** — the entry is gray.

**If the selected test case is not failed** — the entry is gray. A passed test
case has nothing to explain.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the run keeps what it recorded.*

**If the test run is being read again at that moment** — nothing is written, and
nothing is said. Only the log records it.

## Why the message comes last

The message is raised after the test run is written, not before. An edit that
was dropped therefore never says *Details updated*. Nothing at all is said, and
that is difference 12 on
[the view panel page](../viewPanel/main.md#where-the-plugin-breaks-its-own-rules)
in its own form: a silent drop is still a silent drop.

---

[Documentation](../README.md) › [The editor panel](main.md)
