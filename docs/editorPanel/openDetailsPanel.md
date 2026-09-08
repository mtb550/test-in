[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-025

# UC-EDITOR-PANEL-025: Open the details panel

**As a** tester, **I want** the whole of one test case beside the list,
**so that** I can read its steps while the list stays where it is.

The panel opens beside the list, not on top of it. The list stays where it was.

`Enter` on the selection.

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
- **Rule-EDITOR-PANEL-112** — Opening the details panel says nothing.
- **Rule-EDITOR-PANEL-113** — Once the panel is open, moving the selection fills
  it again.

## What the tester sees

This opens no screen of its own. The view panel opens on the right of the IDE,
with the **Details** tab in front, and it shows the whole of the selected test
case.

The list is not moved and nothing is written. No message appears, because
nothing changed.

## The four ways in

| The tester does this | Where |
|---|---|
| Presses `Enter` | The cards |
| Double-clicks a card | The cards |
| Chooses **View Details** | Either menu |
| Presses `Enter`, or double-clicks, on the number column | The grid |

## Main flow

1. The tester selects a card and presses `Enter`.
2. The view panel opens on the right, if it was closed.
3. It shows the whole test case, with the Details tab in front.
4. The tester moves down the cards, and the panel follows.

The panel itself is a part of Testin of its own, and is
[the view panel](../viewPanel/main.md).

## What Testin refuses

**If nothing is selected** — **View Details** is gray and `Enter` does nothing.

**If `Enter` is pressed on any grid column but the number** — the cell opens for
editing instead, or nothing happens.

**If the Order field has been unticked** — no grid column is the number column,
so `Enter` and the double-click stop working there. Nothing says why. That is
difference 12 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

---

[Documentation](../README.md) › [The editor panel](main.md)
