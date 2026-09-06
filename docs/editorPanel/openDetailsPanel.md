[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-025

# UC-EDITOR-PANEL-025: Open the details panel

**As a** tester, **I want** the whole of one test case beside the list,
**so that** I can read its steps while the list stays where it is.

`Enter` on the selection.

## Rules

- **Rule 109** — Opening the details panel says nothing.
- **Rule 110** — Once the panel is open, moving the selection fills it again.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
