[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-005

# UC-VIEW-PANEL-005: Read what a test run recorded

**As a** tester, **I want** the verdict and everything behind it beside the test
case itself, **so that** I can see what happened last time without opening the
test run.

There is no key for this. The rows appear when the panel was opened from a test
run.

## Rules

- **Rule 29** — The run rows are drawn only when the panel was opened from a
  test run that holds this test case.
- **Rule 30** — The rows come from the test run itself, so the panel and a
  report on the same test run can never disagree.
- **Rule 31** — A run row with nothing in it is not drawn.
- **Rule 32** — Recording a pass clears the actual result, the stacktrace, the
  bug severity and the bug priority. Four of the six rows go with it.
- **Rule 33** — A test case the test run has not reached yet reads **Pending**.

Rules 1 to 9 hold everywhere in the panel. They are on
[the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The rows, in order

| Caption | What it holds |
|---|---|
| **Run Status** | The verdict: **Passed**, **Failed**, **Blocked**, **Pending**, **Untested** or **Removed** |
| **Duration** | How long the test case took |
| **Actual Result** | What the tester says actually happened |
| **Stacktrace** | The first three lines of the error, and a link to the rest |
| **Bug Severity** | **Blocker**, **Major**, **Minor** or **Enhancement** |
| **Bug Priority** | **High**, **Medium** or **Low** |

## Main flow

1. The tester opens a test run and selects a test case in it.
2. The tester presses `Enter`.
3. Testin looks for that test case in that test run's recorded results.
4. The six run rows are drawn above the test case's own rows.
5. Below them, the test case's own fields are drawn as usual.

## What Testin refuses

**If the panel was opened from a test set rather than a test run** — no run rows
are drawn. There is no test run to read from.

**If the test run does not hold this test case** — no run rows are drawn. The
test case is drawn on its own, which reads as a test case nobody has run. That
is question 3 on [the view panel page](main.md#not-decided).

**If the test case passed** — the actual result, the stacktrace, the bug
severity and the bug priority were cleared when the pass was recorded. Only
**Run Status** and **Duration** remain.

**If nothing was timed** — the **Duration** row disappears. A verdict recorded
from the menu, or on several test cases at once, is never timed.

## Where the plugin breaks its own rules

The **Bug Severity** and **Bug Priority** rows can read **Blocker** and
**High** while the Open Bugs tab beside them reads *No bugs found for this test
case.* That is difference 5 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The view panel](main.md)
