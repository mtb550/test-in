[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-031

# UC-EDITOR-PANEL-031: Start executing by hand

**As a** tester, **I want** Testin to walk me through the test run one test case
at a time, **so that** I judge each one in turn and each one is timed.

There is no key for this. The button's tooltip reads **Start Manual Execution**.

## Rules

- **Rule-EDITOR-PANEL-128** — The walk only ever lands on a test case waiting
  for a verdict. It starts at the first one and moves to the next, passing over
  any that has been judged, whoever judged it.
- **Rule-EDITOR-PANEL-129** — Starting marks the test run **In Progress**, and
  stamps when execution began. That stamp is set once and never overwritten.
- **Rule-EDITOR-PANEL-130** — The clock starts on the test case the walk lands
  on, and ticks once a second.
- **Rule-EDITOR-PANEL-131** — The button becomes **Stop Execution** while the
  walk is going.
- **Rule-EDITOR-PANEL-132** — Reaching the end of the list ends the walk, and
  nothing more. The test run is marked **Completed** only when every test case
  in it has been judged, which is asked of the test run and not of the walk.
- **Rule-EDITOR-PANEL-133** — Start is offered only when there is something to
  walk. A test run holding no test cases, a filter that matches nothing, and a
  list whose test cases have all been judged are the same thing to the walk: all
  three gray the button, and a press that reaches Testin anyway is refused and
  says so.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester presses **Start Manual Execution**.
2. The test run becomes **In Progress**, and a message says so.
3. Testin finds the first test case with no verdict.
4. The editor turns to the page holding it and selects its row.
5. The clock starts, and the card and the run clock redraw once a second.
6. The tester judges it with `P`, `F` or `B`.
7. The walk moves to the next test case waiting for a verdict, passing over
   any that already has one, and times that one.
8. When the walk runs out of test cases it stops, and the test run is written.
9. If every test case in the test run now has a verdict, the test run is marked
   **Completed**.

## What Testin refuses

**If a walk is already going** — the button is gray, and its tooltip reads
*Execution in progress*.

**If the test run is completed or closed** — the button is gray, and its tooltip
reads *Execution disabled — run status is*, then the status.

**If nothing is waiting for a verdict** — the button is gray, and its tooltip
reads *Nothing to execute — no test case is waiting for a verdict*. That is a
test run holding no test cases, a filter that matches nothing, and a list whose
test cases have all been judged. [Light mode](lightMode.md) grays no button, so
its start refuses instead, in a message that fades.

---

[Documentation](../README.md) › [The editor panel](main.md)
