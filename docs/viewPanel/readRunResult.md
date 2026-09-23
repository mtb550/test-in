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

What this run recorded is a band of its own, under the identity line and above
what the test case says (Rule-VIEW-PANEL-085).

```
┌──────────────────────────────────────────────────────────────────────────┐
│   Demo > Test Cases > Accounts > Login   ( 3f2a05c1-...-9c1b ) [copy]    │
│                                                                          │
│   Log in with a valid user                                               │
│                                                                          │
│   ( P1 ) ( Smoke )        [ go to code ] [ run ] [ tc ]                  │
│                                                                          │
│   THIS RUN ────────────────────────────────────────────────────────      │
│   ( Failed )  ( 02:14 )  ( muteb · 7 January 10:05 )                     │
│   ACTUAL RESULT                                                          │
│   The session was dropped.                                               │
│   STACKTRACE                                                             │
│   java.lang.AssertionError: expected [true]                              │
│     at org.testin.demo.LoginTest.valid                                   │
│   Show all 42 lines                                                      │
│   ( Blocker / High )   Report a bug                                      │
│                                                                          │
│   ▸ THE TEST CASE ────────────────────────────────────────────────────   │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The band's name** — **This run**, in the caption font, with a hairline to
   the panel's edge (Rule-VIEW-PANEL-085).
2. **The line of pills** — the verdict in its own color, how long it took, and
   who ran it and when (Rule-VIEW-PANEL-086).
3. **The two rows that hold sentences** — the actual result and the stacktrace,
   each caption on a line of its own above its value (Rule-VIEW-PANEL-082).
4. **Show all 42 lines** — opens the whole error in a window. That is
   [UC-VIEW-PANEL-006](readStacktrace.md).
5. **The bug** — one chip carrying the severity and the priority together, in
   the severity's color, beside **Report a bug** (Rule-VIEW-PANEL-086).
6. **The test case's band** — folded, because a tester reading a failure is
   reading the failure. Clicking its name opens every field the test case has
   (Rule-VIEW-PANEL-087).

## The band, in order

| Where it is       | What it holds                                                                                                                                                                                                                       |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The first pill    | The verdict: **Passed**, **Failed**, **Blocked**, **Pending**, **Untested** or **Removed**, in the verdict's own color                                                                                                               |
| The second pill   | How long the test case took                                                                                                                                                                                                         |
| The third pill    | Who ran it and when, as `muteb · 7 January 10:05`                                                                                                                                                                                   |
| **Actual Result** | What the tester says actually happened                                                                                                                                                                                              |
| **Stacktrace**    | The first three lines of the error, a link to the rest, and a thumbnail of each screenshot pasted with the failure. That is [UC-VIEW-PANEL-006](readStacktrace.md)                                                                    |
| The bug chip      | The severity and the priority in one chip, as `Blocker / High`, in the severity's color                                                                                                                                              |
| Beside the chip   | The GitHub issue the failure was reported as, as `owner/repo#123`, and the **Report a bug** link that files one. That is [UC-VIEW-PANEL-016](reportBug.md). Drawn for a failed test case, and for any test case that has been reported |

## Main flow

1. The tester opens a test run and selects a test case in it.
2. The tester presses `Enter`.
3. Testin looks for that test case in that test run's recorded results.
4. **This run** is drawn above what the test case says.
5. Below it, the test case's own band is drawn as usual.

## What Testin refuses

**If the panel was opened from a test set rather than a test run** — the run
band is not drawn at all, not even its name. There is no test run to read from.

**If the test run does not hold this test case** — the run band is not drawn.
The test case is drawn on its own. It then looks like a test case nobody has run.
That is question 3 on [the view panel page](main.md#not-decided).

**If the test case passed** — five values were cleared when the pass was
recorded: the actual result, the stacktrace, the bug severity, the bug priority
and the bug issue link. The band is left holding its line of pills.

**If nothing was timed** — the duration pill disappears and the two beside it
close the gap. A verdict recorded from the menu, or on several test cases at
once, is never timed.

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
