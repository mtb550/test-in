[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-042

# UC-EDITOR-PANEL-042: Watch how the run is going

**As a** tester, **I want** to see how many have passed and how long I have
been at it, **so that** I can say when the run will be finished.

There is no key for this. The figures are in the status bar.

## Rules

- **Rule-EDITOR-PANEL-171** — A verdict no test case carries is not drawn at
  all.
- **Rule-EDITOR-PANEL-172** — The untouched test cases read **Pending** while
  the test run is open, and **Untested** once it is signed off.
- **Rule-EDITOR-PANEL-173** — The figures are worked out from what the test run
  holds now, not from disk.
- **Rule-EDITOR-PANEL-174** — A test run that has measured nothing shows a blank
  clock, not a row of zeros.
- **Rule-EDITOR-PANEL-175** — The three run labels are hidden, not blank, when
  there is nothing to say. A test set editor never shows them.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  4 of 12 test cases   |< < 1 of 1 > >|   In Progress                       │
│                       Passed 10 - Failed 2 - Pending 4    00:14:22  [ 50 ] │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The run status** — with the same icon the tree draws. Its tooltip reads
   *This run's status. A completed or closed run records no more verdicts*.
2. **The figures** — one for each verdict any test case carries, each in that
   verdict's own color. Their tooltip reads *How this run is going*.
3. **The clock** — how long this test run has been executing. It ticks once a
   second while a test case is being timed. Its tooltip reads *Time spent
   executing this run*.
4. **The page size** — how many test cases a page holds.

## Main flow

1. The tester starts executing.
2. The status becomes **In Progress**, and the clock starts.
3. Each verdict recorded moves one figure up and another down.
4. A verdict nobody has recorded yet is not drawn.
5. When the walk finishes, the status becomes **Completed** and the clock stops.

## What Testin refuses

Nothing. The status bar only reports.

## Where the plugin breaks its own rules

**The tooltip is not true.** It says a completed or closed test run records no
more verdicts. `P`, `F` and `B` still record one on a signed off test run. That
is difference 19 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

**A finished test run can still read In Progress.** Judging the last pending
test case from the menu does not mark the test run completed. Only the
automation does. That is difference 23.

---

[Documentation](../README.md) › [The editor panel](main.md)
