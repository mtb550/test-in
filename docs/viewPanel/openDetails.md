[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-001

# UC-VIEW-PANEL-001: Open a test case's details

**As a** tester, **I want** the whole of one test case in front of me,
**so that** I can read its steps and its expected result without leaving the
screen I am working in.

A card shows only a title. This opens the whole test case beside it.

`Enter` opens it on whatever is selected. Five gestures open it in all.

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
- **Rule-VIEW-PANEL-010** — Five gestures open the panel. Each of them opens the
  tool window if it was closed.
- **Rule-VIEW-PANEL-011** — Four of the five move the keyboard into the panel.
  Choosing a search result does not.
- **Rule-VIEW-PANEL-012** — The panel opens on the **Details** tab, whichever
  tab was in front last time.
- **Rule-VIEW-PANEL-013** — Two gestures can hand over more than one test case.
  The panel shows the first, and the rest are reached with the paging keys.
- **Rule-VIEW-PANEL-014** — The panel is handed the folder the test case was
  opened from. That folder decides whether the run rows are drawn.

## The screen

The panel opens on the right of the IDE, beside the editor.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Testin                                                   ( < )  ( > )   │
├──────────────────────────────────────────────────────────────────────────┤
│  | Details |    History     Open Bugs                                    │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Demo  >  Test Cases  >  Accounts  >  Login                             │
│                                                                          │
│   ( 3f2a05c1-...-9c1b )  [copy]                                          │
│                                                                          │
│   Log in with a valid user                                               │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The stripe** — reads **Testin**, the same word the tree panel's stripe
   reads. The side they sit on is the only thing that tells them apart.
2. **The two arrows** — move to the previous and the next test case. They are
   gray when the panel was handed only one.
3. **The tabs** — the panel always opens on **Details**, whichever tab was in
   front last time.
4. **The test case** — drawn in full below. Every part of it is numbered on
   [the view panel page](main.md#the-panel).

## The five ways in

| The tester does this | Test cases handed over | Keyboard moves |
|---|---|---|
| Double-clicks a card in an editor | One | Yes |
| Presses `Enter`, or chooses **View Details** | Every one selected | Yes |
| Presses `Enter` on the number column of a grid row, or double-clicks it | One | Yes |
| Clicks the mark beside a generated test method | One | Yes |
| Chooses a test case in the search | One | No |

## Main flow

1. The tester selects one or more test cases in an editor.
2. The tester presses `Enter`, or chooses **View Details** from the menu.
3. The view panel opens on the right, if it was closed.
4. The **Details** tab comes to the front.
5. The panel draws the first test case in full.
6. The keyboard moves into the panel, so `F2` works straight away.

## What Testin refuses

**If nothing is selected** — **View Details** is gray, and `Enter` does nothing.

**If the double-click lands between two cards** — nothing opens, and nothing is
said.

**If the tester presses `Enter` on any grid column but the number** — the panel
does not open. The cell opens for editing instead, or nothing happens.

**If the tester clicks the mark beside a test method whose test case was
removed** — nothing opens, and nothing is said. Only the log records it.

**If the search result is a package, a folder or a test project** — there is no
test case, so the panel is never touched.

---

[Documentation](../README.md) › [The view panel](main.md)
