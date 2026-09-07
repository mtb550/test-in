[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-024

# UC-EDITOR-PANEL-024: Select test cases

**As a** tester, **I want** to pick several test cases at once,
**so that** one gesture changes all of them.

Almost every other page here starts with a selection. This is how one is made.

There is no key that starts this. Click, `Ctrl`-click and `Shift`-click.

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
- **Rule-EDITOR-PANEL-108** — Several test cases can be selected, in as many
  separate runs as the tester likes.
- **Rule-EDITOR-PANEL-109** — Clicking outside every card clears the selection.
- **Rule-EDITOR-PANEL-110** — Right-clicking outside the selection moves the
  selection to what was clicked first.
- **Rule-EDITOR-PANEL-111** — The grid's selection and the cards' selection are
  always the same. Changing one changes the other.

## What the tester sees

This opens no screen. The selected cards, or the selected cells, are drawn with
a highlight behind them.

The left of the status bar changes to say what is selected. Nothing else
happens, and no message appears.

## How to select

| In the cards | In the grid |
|---|---|
| Click a card | Click a cell |
| `Ctrl`-click to add one | Drag across cells |
| `Shift`-click to take a range | Click the number column to take a whole row |
| Click the empty space to clear | `Ctrl`-click the number column to add a row |
| | `Shift`-click the number column to take a range of rows |

## What the status bar says

| The selection | What it reads |
|---|---|
| Nothing | *0 of 12 test cases* |
| One test case | Its position, then *of 12 test cases* |
| Several | The count, then *selected of 12 test cases* |
| Anything, with a filter on | The same, then *(filtered from 120)* |

## What Testin refuses

**If the tester right-clicks inside the selection** — the menu acts on the whole
selection, not on the card under the pointer.

**If the tester right-clicks outside the selection** — the selection moves to
that card first, then the menu opens.

## Where the plugin breaks its own rules

With one row highlighted the status bar can read *0 of 12 test cases*, because
the zero is meant as a position and reads as a count. That is difference 18 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

---

[Documentation](../README.md) › [The editor panel](main.md)
