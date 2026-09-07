[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-026

# UC-EDITOR-PANEL-026: Step back

**As a** tester, **I want** one key that undoes whatever state I am in,
**so that** I can get back to a plain list without thinking about how I got
here.

`Escape`.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing. Paging, filtering,
  searching and opening the details panel are all silent. It changes nothing
  either: a test case the filter is hiding is reported, never brought into view
  by throwing the filter away.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-114** — One press does one step. The steps are always in
  the same order.
- **Rule-EDITOR-PANEL-115** — In the grid, an open cell takes the press first,
  and only cancels the edit.
- **Rule-EDITOR-PANEL-116** — In the search box, the press returns the keyboard
  to the list and leaves the text.

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
