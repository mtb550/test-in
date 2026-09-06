[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-026

# UC-EDITOR-PANEL-026: Step back

**As a** tester, **I want** one key that undoes whatever state I am in,
**so that** I can get back to a plain list without thinking about how I got
here.

`Escape`.

## Rules

- **Rule 111** — One press does one step. The steps are always in the same
  order.
- **Rule 112** — In the grid, an open cell takes the press first, and only
  cancels the edit.
- **Rule 113** — In the search box, the press returns the keyboard to the list
  and leaves the text.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The order of the steps

| Press | What it does |
|---|---|
| A cell is open for editing | Cancels the edit |
| A cut is waiting | Drops the cut and empties the clipboard |
| The details panel is open | Closes it |
| Anything else | Clears the selection |

## Main flow

1. The tester cuts three test cases, opens the details panel, and changes their
   mind.
2. The tester presses `Escape`. The cut is dropped and the cards stop being
   faded.
3. The tester presses `Escape`. The details panel closes.
4. The tester presses `Escape`. The selection is cleared.

## What Testin refuses

Nothing. Every press does the next step, and a press with nothing left to do
does nothing.

## What it does not close

The editor tab. `Escape` never closes an editor, only the things inside it.

---

[Documentation](../README.md) › [The editor panel](main.md)
