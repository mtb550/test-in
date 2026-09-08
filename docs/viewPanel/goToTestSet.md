[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-010

# UC-VIEW-PANEL-010: Go to the test set the test case lives in

**As a** tester, **I want** to open the test set holding the test case I am
reading, **so that** I can see the test cases around it.

The path at the top says where the test case lives. Its last step opens that
place.

There is no key for this. The path is at the top of the panel.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE. Its
  stripe reads **Testin**, exactly as the tree panel's does. The side they are
  on is the only thing that tells them apart.
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
- **Rule-VIEW-PANEL-041** — The path shows one step for each folder above the
  test case.
- **Rule-VIEW-PANEL-042** — Only the last step opens anything, and it opens the
  place it names: the test set, or the test run when the panel was opened from
  a run.
- **Rule-VIEW-PANEL-043** — Opening a test set that is already open brings it to
  the front.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Demo  >  Test Cases  >  Accounts  >  Login                               │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Every step** — gray, and underlined under the pointer.
2. **The last step** — the only one that opens anything. It opens the place it
   names.

## Main flow

1. The tester moves the pointer over the last step of the path.
2. The step turns to the link color and underlines itself.
3. The tester clicks it.
4. The test set's editor opens, or comes to the front if it was already open.

**When the panel was opened from a test run**, the path names that test run, and
its last step opens the test run's editor. The step always opens the place it
names, so what the tester reads is what they get.

## What Testin refuses

**If the tester clicks any step but the last** — nothing happens, and nothing is
said. The step still shows a hand pointer and still underlines itself. That is
difference 4 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

**If the panel was handed no path** — no steps are drawn at all.

**If the last step names something with no editor** — nothing happens, and
nothing is said. A package and the two containers are places in the tree, not
things to open.

---

[Documentation](../README.md) › [The view panel](main.md)
