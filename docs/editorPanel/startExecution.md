[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-031

# UC-EDITOR-PANEL-031: Start executing by hand

**As a** tester, **I want** Testin to walk me through the test run one test case
at a time, **so that** I judge each one in turn and each one is timed.

There is no key for this. The button's tooltip reads **Start Manual Execution**.

## Rules

- **Rule 127** — The walk starts at the first test case that has no verdict yet.
- **Rule 128** — Starting marks the test run **In Progress**, and stamps when
  execution began. That stamp is set once and never overwritten.
- **Rule 129** — The clock starts on the test case the walk lands on, and ticks
  once a second.
- **Rule 130** — The button becomes **Stop Execution** while the walk is going.
- **Rule 131** — Reaching the end of the list marks the test run **Completed**.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester presses **Start Manual Execution**.
2. The test run becomes **In Progress**, and a message says so.
3. Testin finds the first test case with no verdict.
4. The editor turns to the page holding it and selects its row.
5. The clock starts, and the card and the run clock redraw once a second.
6. The tester judges it with `P`, `F` or `B`.
7. The walk moves to the next test case and times that one.

## What Testin refuses

**If a walk is already going** — the button is gray, and its tooltip reads
*Execution in progress*.

**If the test run is completed or closed** — the button is gray, and its tooltip
reads *Execution disabled — run status is*, then the status.

## Where the plugin breaks its own rules

**The walk follows the filter, and finishing it completes the whole test run.**
Filter to three test cases, press start, judge all three, and every other
pending test case in the test run becomes **Untested**. One message reads
*Completed* and nothing warns. That is difference 20 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

**A filter that matches nothing completes the test run at once.** So does a test
run holding no test cases, whose start button is not gray. That is difference
21.

**The walk offers test cases that already have a verdict.** It starts at the
first one without a verdict, then goes one row at a time from there. A test case
judged earlier is landed on, timed again, and judging it re-stamps who and when.
That is difference 22.

---

[Documentation](../README.md) › [The editor panel](main.md)
