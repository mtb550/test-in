[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-007

# UC-VIEW-PANEL-007: Read a test case's history

**As a** tester, **I want** to see what changed on a test case and when,
**so that** I can tell whether a failure follows a change somebody made.

There is no key for this. The tab is called **History**.

## Rules

- **Rule-VIEW-PANEL-037** — The History tab is not built. It shows one line
  saying so, rather than showing invented data.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

Two of the test case's own fields do carry some of this. **Updated By** and
**Updated At** are on the Details tab, and say who last changed the test case
and when. They do not say what changed.

## Not decided

Nobody has decided what a test case's history should hold, or where it would be
read from. Testin keeps no record of what a field used to say. That is question
1 on [the view panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The view panel](main.md)
