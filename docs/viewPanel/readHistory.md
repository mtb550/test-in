[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-007

# UC-VIEW-PANEL-007: Read a test case's history

**As a** tester, **I want** to see what changed on a test case and when,
**so that** I can tell whether a failure follows a change somebody made.

This tab is not built yet. It shows one line saying so.

There is no key for this. The tab is called **History**.

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
- **Rule-VIEW-PANEL-037** — The History tab is not built. It shows one line
  saying so, rather than showing invented data.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Details    | History |   Open Bugs                                       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│                                                                            │
│                        No history available yet                            │
│                                                                            │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The line** — gray, in the middle of the tab. It is the whole tab.

## Main flow

1. The tester clicks **History**.
2. The tab shows one gray line reading *No history available yet*.

## What Testin refuses

**Always.** The tab shows the same line whether a test case is selected or not.
It never looks at the test case.

Two fields of the test case carry part of this. **Updated By** and **Updated
At** are on the Details tab. They say who last changed the test case, and when.
They do not say what changed.

## Not decided

Nobody has decided what a test case's history should hold. Nobody has decided
where it would be read from either. Testin keeps no record of what a field used
to say. That is question 1 on [the view panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The view panel](main.md)
