[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-043

# UC-EDITOR-PANEL-043: Run one test case's automation

**As a** tester, **I want** to run the generated method for a test case and have
its verdict land in this test run, **so that** I do not have to judge by hand
what the machine can judge.

`F5` on the selection.

## Rules

- **Rule 176** — This editor claims each test case first, so the verdict comes
  back to this test run and not to another editor showing the same test case.
- **Rule 177** — Claiming marks the test run **In Progress** if it is not
  already.
- **Rule 178** — A verdict from the automation is written the same way a
  keyboard verdict is.
- **Rule 179** — The framework's own timing replaces whatever the clock counted.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
one with a count. That is difference 25 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

**An automated pass clears the tester's notes with no warning.** That is
difference 26.

**Pressing `F5` to stop marks the test run In Progress.** Every selected test
case is claimed before Testin asks whether this is a run or a stop, and claiming
starts the test run. That is difference 27.

**Closing the tab while automation is running leaves it running.** The process
keeps going, the cards keep changing, and every verdict it produces is written
into no test run at all. That is difference 28.

---

[Documentation](../README.md) › [The editor panel](main.md)
