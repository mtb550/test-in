[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-005

# UC-VIEW-PANEL-005: Read what a test run recorded

**As a** tester, **I want** the verdict and everything behind it beside the test
case itself, **so that** I can see what happened last time without opening the
test run.

These rows come from one test run. The rows under them come from the test case.

There is no key for this. The rows appear when the panel was opened from a test
run.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has three tabs, and all three are drawn
  every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing any Testin editor empties the panel.
- **Rule-VIEW-PANEL-029** — The run rows are drawn only when the panel was
  opened from a test run that holds this test case.
- **Rule-VIEW-PANEL-030** — The rows come from the test run itself, so the panel
  and a report on the same test run can never disagree.
- **Rule-VIEW-PANEL-031** — A run row with nothing in it is not drawn.
- **Rule-VIEW-PANEL-032** — Recording a pass clears the actual result, the
  stacktrace, the bug severity and the bug priority. Four of the six rows go
  with it.
- **Rule-VIEW-PANEL-033** — A test case the test run has not reached yet reads
  **Pending**.

## The screen

The run rows sit under the badges and above the test case's own rows.

```
┌──────────────────────────────────────────────────────────────────────────┐
│   ( P1 )  ( Smoke )  ( Failed )                                          │
│                                                                          │
│   Run Status          Failed                                             │
│   Duration            02:14                                              │
│   Actual Result       The session was dropped.                           │
│   Stacktrace          java.lang.AssertionError: expected [true]          │
│                         at org.testin.demo.LoginTest.valid               │
│                         at org.testng.internal.Invoker.invoke            │
│                       Show all 42 lines                                  │
│   Bug Severity        Blocker                                            │
│   Bug Priority        High                                               │
│                                                                          │
│   Expected Result:    The dashboard opens.                               │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The badges** — the last badge is the verdict.
2. **The six run rows** — what one test run recorded. Their captions have no
   colon after them.
3. **Show all 42 lines** — opens the whole error in a window. That is
   [UC-VIEW-PANEL-006](readStacktrace.md).
4. **The test case rows** — the test case's own fields, below. Their captions
   do have a colon.

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
test case is drawn on its own. It then looks like a test case nobody has run.
That is question 3 on [the view panel page](main.md#not-decided).

**If the test case passed** — four rows were cleared when the pass was
recorded: the actual result, the stacktrace, the bug severity and the bug
priority. Only **Run Status** and **Duration** are left.

**If nothing was timed** — the **Duration** row disappears. A verdict recorded
from the menu, or on several test cases at once, is never timed.

## Where the plugin breaks its own rules

The **Bug Severity** and **Bug Priority** rows can read **Blocker** and
**High** while the Open Bugs tab beside them reads *No bugs found for this test
case.* That is difference 5 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The view panel](main.md)
