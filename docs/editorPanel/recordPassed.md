[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-032

# UC-EDITOR-PANEL-032: Record that a test case passed

**As a** tester, **I want** one key to say a test case worked,
**so that** walking a test run of 80 is 80 keystrokes and nothing else.

`P`.

## Rules

- **Rule-EDITOR-PANEL-134** — A verdict records what it was, who recorded it,
  and when, to the second.
- **Rule-EDITOR-PANEL-135** — A verdict recorded on the test case the walk is
  timing also records how long it took.
- **Rule-EDITOR-PANEL-136** — Recording a pass clears the actual result, the
  error, the bug severity and the bug priority. A test case that passed has
  nothing to explain.
- **Rule-EDITOR-PANEL-137** — The walk then moves to the next test case and
  starts timing it.
- **Rule-EDITOR-PANEL-138** — One test case is one message. Several at once is
  one message with a count.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
in Rule-EDITOR-PANEL-136 is only on the keyboard path. A test case failed and
written up by hand, then re-run by automation and passing, loses the actual
result, the error, the severity and the priority with no dialog. That is
difference 26.

---

[Documentation](../README.md) › [The editor panel](main.md)
