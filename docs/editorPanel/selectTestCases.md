[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-024

# UC-EDITOR-PANEL-024: Select test cases

**As a** tester, **I want** to pick several test cases at once,
**so that** one gesture changes all of them.

There is no key that starts this. Click, `Ctrl`-click and `Shift`-click.

## Rules

- **Rule-EDITOR-PANEL-106** — Several test cases can be selected, in as many
  separate runs as the tester likes.
- **Rule-EDITOR-PANEL-107** — Clicking outside every card clears the selection.
- **Rule-EDITOR-PANEL-108** — Right-clicking outside the selection moves the
  selection to what was clicked first.
- **Rule-EDITOR-PANEL-109** — The grid's selection and the cards' selection are
  always the same. Changing one changes the other.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
