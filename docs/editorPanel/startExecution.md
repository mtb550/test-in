[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-031

# UC-EDITOR-PANEL-031: Start executing by hand

**As a** tester, **I want** Testin to walk me through the test run one test case
at a time, **so that** I judge each one in turn and each one is timed.

There is no key for this. The button's tooltip reads **Start Manual Execution**.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing. Paging, filtering,
  searching and opening the details panel are all silent. It changes nothing
  either: a test case the filter is hiding is reported, never brought into view
  by throwing the filter away.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-130** — The walk only ever lands on a test case waiting
  for a verdict. It starts at the first one and moves to the next, passing over
  any that has been judged, whoever judged it.
- **Rule-EDITOR-PANEL-131** — Starting marks the test run **In Progress**, and
  stamps when execution began. That stamp is set once and never overwritten.
- **Rule-EDITOR-PANEL-132** — The clock starts on the test case the walk lands
  on, and ticks once a second.
- **Rule-EDITOR-PANEL-133** — The button becomes **Stop Execution** while the
  walk is going.
- **Rule-EDITOR-PANEL-134** — Reaching the end of the list ends the walk, and
  nothing more. The test run is marked **Completed** only when every test case
  in it has been judged, which is asked of the test run and not of the walk -
  and asked wherever a verdict is recorded, whichever way the last one arrived.
- **Rule-EDITOR-PANEL-135** — Start is offered only when there is something to
  walk. A test run holding no test cases, a filter that matches nothing, and a
  list whose test cases have all been judged are the same thing to the walk: all
  three gray the button, and a press that reaches Testin anyway is refused and
  says so.

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
