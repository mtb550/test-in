[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-043

# UC-EDITOR-PANEL-043: Run one test case's automation

**As a** tester, **I want** to run the generated method for a test case and have
its verdict land in this test run, **so that** I do not have to judge by hand
what the machine can judge.

The machine runs the test and writes the verdict. The tester does not press `P`
or `F`.

`F5` on the selection.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-180** — This editor claims each test case first, so the
  verdict comes back to this test run and not to another editor showing the same
  test case.
- **Rule-EDITOR-PANEL-181** — Claiming marks the test run **In Progress** if it
  is not already.
- **Rule-EDITOR-PANEL-182** — A verdict from the automation is written the same
  way a keyboard verdict is.
- **Rule-EDITOR-PANEL-183** — The framework's own timing replaces whatever the
  clock counted.

## What the tester sees

This opens no screen of Testin's own. The selected cards turn to running at
once, and the IDE's own run window opens below the editor.

Each result comes back on its own and its card takes the new verdict. Every one
raises its own small message at the bottom of the IDE, reading *Passed* or
*Failed*.

## Main flow

1. The tester selects three test cases in the test run and presses `F5`.
2. Testin claims all three for this editor.
3. The cards turn to running at once.
4. The three are handed to TestNG as one configuration.
5. A message reads *Running 3*.
6. Each result comes back and is written into the test run.

Everything about how the run is built and named is on
[UC-CODEGEN-008](../codegen/runAutomation.md).

## What is recorded

| Written | From |
|---|---|
| The verdict | Whether the framework said it passed or failed |
| The duration | The framework's own timing |
| The actual result and the error | The framework's message and stacktrace |
| Who ran it, and when | The name on the settings page, and now |

A result with no message and no error does not clear what the tester wrote by
hand.

## What Testin refuses

**If the test run is completed or closed** — the result is ignored. This is the
one path that respects a signed off test run.

**If the test case is not covered by this test run** — the result is ignored.

**If a test case has no generated method** — it is dropped and a message says
so. The others still run.

## Where the plugin breaks its own rules

**One message for each test case.** An automated test run of 50 raises 50
messages reading *Passed* or *Failed*. Every other bulk gesture in Testin raises
one message with a count. That is difference 25 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

**An automated pass still clears the tester's notes.** It now says so
afterwards, in a message titled **Failure detail cleared** that names what went
and stays in the notification list. The dialog that asks first is still on the
keyboard path only. That is difference 26.

**Closing the tab stops the automation this editor started.** It is the same
gesture as pressing **Stop Execution**, so the results of tests still running
are lost, exactly as a stop loses them.

---

[Documentation](../README.md) › [The editor panel](main.md)
