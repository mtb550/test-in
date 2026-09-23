[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-005

# UC-VIEW-PANEL-005: Read what a test run recorded

**As a** tester, **I want** the verdict and everything behind it beside the test
case itself, **so that** I can see what happened last time without opening the
test run.

This band comes from one test run. The band under it comes from the test case.

There is no key for this. The band appears when the panel was opened from a
test run.

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
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-029** — The run band is drawn only when the panel was
  opened from a test run that holds this test case.
- **Rule-VIEW-PANEL-030** — The band comes from the test run itself, so the
  panel and a report on the same test run can never disagree.
- **Rule-VIEW-PANEL-031** — A run value with nothing in it is not drawn.
- **Rule-VIEW-PANEL-032** — Recording a pass clears the actual result, the
  stacktrace, the bug severity, the bug priority and the bug issue link. The
  band is left holding its line of pills.
- **Rule-VIEW-PANEL-033** — A test case the test run has not reached yet reads **Pending**.

## The screen

What this run recorded is **Execution result**, a band of its own under the
identity line and above **Test case details** (Rule-VIEW-PANEL-085).

```
┌──────────────────────────────────────────────────────────────────────────┐
│   Demo > Test Cases > Accounts > Login                                   │
│                                                                          │
│   Log in with a valid user                                               │
│                                                                          │
│   ( P1 ) ( Smoke )        [ go to code ] [ run ] [ tc ]                  │
│                                                                          │
│   EXECUTION RESULT ────────────────────────────────────────────────────  │
│   ( Failed ) ( 02:14 ) ( Blocker / High )  mtb550/x#123  Report a bug    │
│   ACTUAL RESULT                                                          │
│   The session was dropped.                                               │
│   Exception                                                              │
│   EXECUTED BY                                                            │
│   muteb on 7 January 10:05                                               │
│                                                                          │
│   ▸ TEST CASE DETAILS ─────────────────────────────────────────────────  │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The band's name** — **Execution result**, in the caption font, with a
   hairline running from it to the panel's edge, level with the middle of the
   words (Rule-VIEW-PANEL-085).
2. **The summary line** — everything the run recorded that fits in a word or
   two: the verdict in its own color, the duration, the bug, and the links that
   open the issue or raise one (Rule-VIEW-PANEL-086).
3. **The duration** — a clock and the time, in a plain frame with nothing
   filled in. It is a measurement, not a verdict, and the frame says so.
4. **The bug** — one chip carrying the severity and the priority together, in
   the severity's color, then the issue as `owner/repo#123` and **Report a
   bug**.
5. **Actual result** — the one sentence the band holds, its caption on a line of
   its own above it (Rule-VIEW-PANEL-082).
6. **Exception** — the application's own error, which the panel never shows. The
   link opens it in a window, beside a thumbnail of each screenshot. That is
   [UC-VIEW-PANEL-006](readStacktrace.md) and Rule-VIEW-PANEL-034.
7. **Executed by** — last in the band, because who ran it is read after what
   happened. One row, in the words **Created** uses on the test case's own band
   (Rule-VIEW-PANEL-061).
8. **Test case details** — folded, because a tester reading a failure is
   reading the failure. Clicking its name opens every field the test case has
   (Rule-VIEW-PANEL-087).

## The band, in order

| Where it is       | What it holds                                                                                                                                                                                                                       |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The first badge   | The verdict: **Passed**, **Failed**, **Blocked**, **Pending**, **Untested** or **Removed**, in the verdict's own color                                                                                                               |
| The second badge  | How long the test case took, with a clock, in a plain frame with nothing filled in                                                                                                                                                   |
| The bug chip      | The severity and the priority in one chip, as `Blocker / High`, in the severity's color                                                                                                                                              |
| After the chip    | The GitHub issue the failure was reported as, as `owner/repo#123`, and the **Report a bug** link that files one. That is [UC-VIEW-PANEL-016](reportBug.md). Drawn for a failed test case, and for any test case that has been reported |
| **Actual Result** | What the tester says actually happened                                                                                                                                                                                              |
| **Exception**     | A link, never the error itself, and a thumbnail of each screenshot pasted with the failure. That is [UC-VIEW-PANEL-006](readStacktrace.md)                                                                                            |
| **Executed By**   | Who ran it and when, as `muteb on 7 January 10:05`, last in the band. One row, not two — the date has no caption of its own (Rule-VIEW-PANEL-061)                                                                                    |

## Main flow

1. The tester opens a test run and selects a test case in it.
2. The tester presses `Enter`.
3. Testin looks for that test case in that test run's recorded results.
4. **Execution result** is drawn above what the test case says.
5. Below it, the test case's own band is drawn as usual.

## What Testin refuses

**If the panel was opened from a test set rather than a test run** — the run
band is not drawn at all, not even its name. There is no test run to read from.

**If the test run does not hold this test case** — the run band is not drawn.
The test case is drawn on its own. It then looks like a test case nobody has run.
That is question 3 on [the view panel page](main.md#not-decided).

**If the test case passed** — five values were cleared when the pass was
recorded: the actual result, the stacktrace, the bug severity, the bug priority
and the bug issue link. The band is left holding the verdict and the duration.

**If nothing was timed** — the duration badge disappears and what is beside it
closes the gap. A verdict recorded from the menu, or on several test cases at
once, is never timed.

**If nobody is named** — the **Executed By** row holds the date on its own, and
with neither a name nor a date the row is not drawn at all
(Rule-VIEW-PANEL-031).

## The same bug, on the tab beside this one

The **Bug Severity** and **Bug Priority** rows say what *this* run recorded. The
[Open Bugs tab](seeOpenBugs.md) says what every run recorded, so a test case
that has failed in more than one cycle shows one bug here and all of them
there.

The two used to disagree rather than differ: this panel read **Blocker** and **High** while the tab beside it said no
bugs were found, because that tab never
looked at the test case (#229).

---

[Documentation](../README.md) › [The view panel](main.md)
