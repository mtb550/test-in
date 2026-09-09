[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-020

# UC-EDITOR-PANEL-020: Filter the test cases

**As a** tester, **I want** to show only the test cases in one group or one
module, **so that** I can work through the smoke tests without the rest in the
way.

A filter hides rows. It never changes or deletes a test case.

There is no key for this. The button's tooltip reads **Filter**.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-094** — Four things can be filtered on: the priority, the
  group, the module and, in a test run, the run status.
- **Rule-EDITOR-PANEL-095** — The module list is built from the test cases in
  this test set, so it only ever offers modules that exist.
- **Rule-EDITOR-PANEL-096** — Choosing nothing in a filter means every test case
  matches it.
- **Rule-EDITOR-PANEL-097** — Filtering goes back to the first page.
- **Rule-EDITOR-PANEL-098** — The button says how many filters are on.

## The screen

```
┌──────────────────────────────────────┐
│  Reset Filters                       │
│  ──────────────────────────────────  │
│  Priority                          > │
│  Group                             > │
│  Module                            > │
│  Status                            > │
└──────────────────────────────────────┘
```

1. **Reset Filters** — drawn only while a filter is on. It is
   [UC-EDITOR-PANEL-021](clearFilters.md).
2. **Priority** — **P1**, **P2** and **P3**, each with its own color.
3. **Group** — every group Testin knows, with **No Group** first.
4. **Module** — the modules the test cases in this test set actually carry.
5. **Status** — the run statuses. It is on a test run only.

While a filter is on, the button shows the count in brackets. Its tooltip
becomes **Filter**, then the count, then **active**.

## Main flow

1. The tester presses the filter button.
2. The menu opens under it.
3. The tester opens **Group** and ticks **Smoke**.
4. The list narrows at once, and goes back to the first page.
5. The button reads **(1)**.
6. The status bar says how many test cases are showing, and how many were
   filtered out.

## What Testin refuses

**If nothing matches** — the middle reads *No test cases match the search*.

**If the module has never been used** — it is not offered. The list is built
from what is there.

---

[Documentation](../README.md) › [The editor panel](main.md)
