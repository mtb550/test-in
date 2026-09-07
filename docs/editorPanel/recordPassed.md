[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-032

# UC-EDITOR-PANEL-032: Record that a test case passed

**As a** tester, **I want** one key to say a test case worked,
**so that** walking a test run of 80 is 80 keystrokes and nothing else.

One key. Testin writes the verdict, the tester's name, the time and the
duration.

`P`.

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
- **Rule-EDITOR-PANEL-136** — A verdict records what it was, who recorded it,
  and when, to the second.
- **Rule-EDITOR-PANEL-137** — A verdict recorded on the test case the walk is
  timing also records how long it took.
- **Rule-EDITOR-PANEL-138** — Recording a pass clears the actual result, the
  error, the bug severity and the bug priority. A test case that passed has
  nothing to explain.
- **Rule-EDITOR-PANEL-139** — The walk then moves to the next test case and
  starts timing it.
- **Rule-EDITOR-PANEL-140** — One test case is one message. Several at once is
  one message with a count.

## What the tester sees

This opens no screen. The card's verdict badge turns to **Passed**, the run
status line follows, and the figures in the status bar move. The walk then
selects the next test case waiting for a verdict.

A small message appears at the bottom of the IDE and fades. It reads *Passed*.

## Main flow

1. The walk has selected a test case and is timing it.
2. The tester tries it in the application under test.
3. The tester presses `P`.
4. Testin records **Passed**, the tester's name, the time, and how long it took.
5. The test run is written to disk at once.
6. A message reads *Passed*.
7. The walk moves to the next test case.

## What Testin refuses

**If nothing is selected** — nothing happens, and nothing is said.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the run keeps what it recorded.* Nothing is written.

**If the test case already holds failure detail** — a confirmation opens first,
because passing it clears four things. That is
[UC-EDITOR-PANEL-038](correctVerdict.md).

**If a grid cell is open for editing** — the key belongs to the cell, and does
nothing else.

## Where the plugin breaks its own rules

**A signed off test run still records verdicts.** The status bar's own tooltip
says a completed or closed test run records no more verdicts. `P` still records
one, still saves it, and still says *Passed*. That is difference 19 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

**An automated pass destroys a tester's notes without asking.** The confirmation
in Rule-EDITOR-PANEL-138 is only on the keyboard path. A test case is failed and
written up by hand. Automation re-runs it later and it passes. The actual
result, the error, the severity and the priority all go, with no dialog. That is
difference 26.

---

[Documentation](../README.md) › [The editor panel](main.md)
