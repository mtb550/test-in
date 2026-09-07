[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-037

# UC-EDITOR-PANEL-037: Record a verdict out of order

**As a** tester, **I want** to judge a test case that is not the one the walk is
on, **so that** I can record something I happened to try while doing something
else.

`P`, `F` or `B` on any selected test case.

## Rules

- **Rule-EDITOR-PANEL-154** — A verdict can be recorded on any test case at any
  time. The walk does not have to be going.
- **Rule-EDITOR-PANEL-155** — A verdict recorded away from the walk is not
  timed. The test case keeps whatever duration it had.
- **Rule-EDITOR-PANEL-156** — Recording away from the walk does not stop the
  walk, and does not move it.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The walk is on test case 12.
2. The tester notices test case 40 is worth judging now.
3. The tester clicks test case 40 and presses `P`.
4. **Passed** is recorded against test case 40, with no duration.
5. A message reads *Passed*.
6. The walk is still on test case 12, still timing it.

## What Testin refuses

**If nothing is selected** — nothing happens.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the run keeps what it recorded.*

**If the test run does not cover this test case** — nothing is recorded, and
only the log says so.

## Why the duration is blank

The clock times the test case the walk is on. A verdict recorded anywhere else
was never timed, so nothing is written to the duration and the column stays
blank. A blank duration means nothing was measured, not that it took no time.

---

[Documentation](../README.md) › [The editor panel](main.md)
