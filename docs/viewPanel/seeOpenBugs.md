[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-008

# UC-VIEW-PANEL-008: See the bugs still open on a test case

**As a** tester, **I want** the defects already raised against this test case,
**so that** I do not raise the same one twice.

This tab is not built yet. It shows one line saying so.

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
- **Rule-VIEW-PANEL-009** — Closing any Testin editor empties the panel.
- **Rule-VIEW-PANEL-038** — The Open Bugs tab is not built. It shows one line
  saying so, and never looks at the test case.

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
records nothing else about a bug. It has no link to a bug tracker either. What
this tab should show is question 2 on
[the view panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The view panel](main.md)
