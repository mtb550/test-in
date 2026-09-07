[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-037

# UC-EDITOR-PANEL-037: Record a verdict out of order

**As a** tester, **I want** to judge a test case that is not the one the walk is
on, **so that** I can record something I happened to try while doing something
else.

`P`, `F` or `B` on any selected test case.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, and
  the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing. Paging, filtering,
  searching and opening the details panel are all silent. It changes nothing
  either: a test case the filter is hiding is reported, never brought into view
  by throwing the filter away.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-156** — A verdict can be recorded on any test case at any
  time. The walk does not have to be going.
- **Rule-EDITOR-PANEL-157** — A verdict recorded away from the walk is not
  timed. The test case keeps whatever duration it had.
- **Rule-EDITOR-PANEL-158** — Recording away from the walk does not stop the
  walk, and does not move it.

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
