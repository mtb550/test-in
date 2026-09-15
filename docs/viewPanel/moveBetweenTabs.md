[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-017

# UC-VIEW-PANEL-017: Move between the panel's tabs

**As a** tester, **I want** to reach the **History** and **Open Bugs** tabs from
the keyboard, **so that** I can read them without taking my hands off it.

The panel has three tabs: **Details**, **History** and **Open Bugs**. `Tab`
brings the next one to the front, and `Shift+Tab` the one before.

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
- **Rule-VIEW-PANEL-079** — `Tab` brings the next tab to the front, and
  `Shift+Tab` the one before. After **Open Bugs** comes **Details** again, and
  before **Details** comes **Open Bugs**.
- **Rule-VIEW-PANEL-080** — The keyboard is in the tab in front: after `Tab`,
  after `Shift+Tab`, after a click on a tab's name and after a click inside a
  tab. A link inside a tab takes no keyboard, so `Tab` always moves between
  tabs.

## What the tester sees

This use case opens no screen of its own. The tab that comes to the front is
marked, and its contents fill the panel.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Testin View                                              ( < )  ( > )   │
├──────────────────────────────────────────────────────────────────────────┤
│    Details    | History |    Open Bugs                                   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   No history yet                                                         │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The tab in front** — the one the keyboard is in.
2. **The two arrows** — still move between the test cases the panel was handed,
   whichever tab is in front.

No message appears. Nothing is written.

## Main flow

1. The tester presses `Enter` on a test case. The panel opens on **Details**,
   with the keyboard in it.
2. The tester presses `Tab`. **History** comes to the front, and the keyboard is
   in it.
3. The tester presses `Tab` again. **Open Bugs** comes to the front.
4. The tester presses `Tab` again. **Details** comes back, and `F2` works there
   again.
5. `Shift+Tab` goes the same way backwards.

A click does the same as the key: a click on a tab's name, or anywhere inside a
tab, puts the keyboard in that tab.

## What Testin refuses

**If the keyboard is not in the panel** — `Tab` does whatever the place holding
the keyboard does with it. In a dialog it moves between fields.

**If the tester wants a link inside Details from the keyboard** — `Tab` does not
reach it. The bug issue link, **Report Bug** and **Show all** are clicked.

**If the panel was handed several test cases** — nothing changes about them.
`Ctrl+Right` and `Ctrl+Left` still move between them, whichever tab is in front.

---

[Documentation](../README.md) › [The view panel](main.md)
