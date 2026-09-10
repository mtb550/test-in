[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-008

# UC-VIEW-PANEL-008: See the bugs still open on a test case

**As a** tester, **I want** the defects already raised against this test case,
**so that** I do not raise the same one twice.

A bug in Testin is not a thing of its own. It is what a test run's row records
about a failure — how bad it is and how soon it must be fixed — so a test case's
bugs are found by looking through the runs it has been in.

That is why the same test case can carry a **Blocker** from cycle 5 and nothing
at all from cycle 14, and why both are worth seeing at once.

There is no key for this. The tab is called **Open Bugs**.

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
- **Rule-VIEW-PANEL-038** — With no test case shown, the Open Bugs tab says to
  select one. It does not describe a test case that is not there.
- **Rule-VIEW-PANEL-064** — The Open Bugs tab lists every bug the test case has
  recorded, and which test run recorded it. A bug is what a run row says about a
  failure - how bad it is and how soon it must be fixed - so a case that has
  never failed has none, and the same case can carry a different bug in every
  cycle.
- **Rule-VIEW-PANEL-065** — The bugs are read from the test runs the indexer
  already holds, so the tab costs a walk over what is in memory and reads
  nothing from disk.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Details      History    | Open Bugs |                                    │
├────────────────────────────────────────────────────────────────────────────┤
│  cycle 5                                                                   │
│  Blocker / High                                                            │
│  The session was dropped after the second factor.                          │
│                                                                            │
│  cycle14                                                                   │
│  Major / Medium                                                            │
│  Timed out waiting for the dashboard.                                      │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The run's name** — which cycle found this bug. It is the only thing that
   says when, so it is the heading rather than a detail.
2. **The severity and the priority** — in the severity's own color, the same one
   the run grid and every report paint it.
3. **What happened** — the actual result the tester wrote, when they wrote one.
   Left out when they did not, like every other empty field in this panel.

The newest run comes first.

## Main flow

1. The tester clicks **Open Bugs**.
2. Testin looks through every test run for this test case's rows.
3. Every row that recorded a bug is drawn, newest first.

## What Testin refuses

**If the test case has never failed** — the tab reads *No bugs recorded for this
test case in any test run*. That is an answer, not an apology: a case with no
bugs is the ordinary case.

**If a row records a failure but no bug** — it is not drawn. An actual result
without a severity or a priority is somebody saying what happened, not somebody
filing a defect.

**If no test case is shown** — the tab reads *Select a test case to view its
bugs*, and says nothing about any test case (Rule-VIEW-PANEL-038).

## Not decided

Testin records a bug severity, a bug priority and what happened. It records
nothing else about a bug, and has no link to a bug tracker. Whether a bug should
be a thing of its own — raised once, carried across cycles, closed — is question
2 on [the view panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The view panel](main.md)
