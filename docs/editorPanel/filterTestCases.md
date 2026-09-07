[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-020

# UC-EDITOR-PANEL-020: Filter the test cases

**As a** tester, **I want** to show only the test cases in one group or one
module, **so that** I can work through the smoke tests without the rest in the
way.

There is no key for this. The button's tooltip reads **Filter**.

## Rules

- **Rule-EDITOR-PANEL-091** — Four things can be filtered on: the priority, the
  group, the module and, in a test run, the run status.
- **Rule-EDITOR-PANEL-092** — The module list is built from the test cases in
  this test set, so it only ever offers modules that exist.
- **Rule-EDITOR-PANEL-093** — Choosing nothing in a filter means every test case
  matches it.
- **Rule-EDITOR-PANEL-094** — Filtering goes back to the first page.
- **Rule-EDITOR-PANEL-095** — The button says how many filters are on.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

While a filter is on, the button gains the count in brackets and its tooltip
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

## Where the plugin breaks its own rules

**Four groups can be filtered on and never assigned.** The filter offers
Security, UI, Functional and Validation. The create dialog offers only
Regression, Smoke and Sanity. That is difference 4 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

**A filter can be thrown away without a word.** Selecting a test case the filter
is hiding drops every filter. It happens after creating a test case, after a
drag, and after choosing a search result. That is difference 10.

---

[Documentation](../README.md) › [The editor panel](main.md)
