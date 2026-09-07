[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-008

# UC-VIEW-PANEL-008: See the bugs still open on a test case

**As a** tester, **I want** the defects already raised against this test case,
**so that** I do not raise the same one twice.

There is no key for this. The tab is called **Open Bugs**.

## Rules

- **Rule-VIEW-PANEL-038** — The Open Bugs tab is not built. It shows one line
  saying so, and never looks at the test case.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Details      History    | Open Bugs |                                    │
├────────────────────────────────────────────────────────────────────────────┤
│  No bugs found for this test case.                                         │
│                                                                            │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The line** — at the top left of the tab. It is the whole tab.

## Main flow

1. The tester clicks **Open Bugs**.
2. The tab shows one line reading *No bugs found for this test case.*

## What Testin refuses

**Always.** The line is the same whatever the test case says, and the same when
there is no test case at all.

## Where the plugin breaks its own rules

**The tab contradicts the tab beside it.** A failed test case can show
**Blocker** and **High** on the Details tab while this tab says no bugs were
found. That is difference 5 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

**The tab names a test case that is not there.** With nothing selected, Details
reads *Select a test case to view details* and this tab still says *for this
test case*. That is difference 6.

## Not decided

Testin records a bug severity and a bug priority against a failed verdict. It
records nothing else about a bug, and it has no link to a bug tracker. What this
tab should show is question 2 on
[the view panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The view panel](main.md)
