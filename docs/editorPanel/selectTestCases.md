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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-108** — Several test cases can be selected, in as many
  separate runs as the tester likes.
- **Rule-EDITOR-PANEL-109** — Clicking outside every card clears the selection.
- **Rule-EDITOR-PANEL-110** — Right-clicking outside the selection moves the
  selection to what was clicked first.
- **Rule-EDITOR-PANEL-111** — The grid's selection and the cards' selection are
  always the same. Changing one changes the other.
- **Rule-EDITOR-PANEL-215** — The editor's own status bar names where the open
  node sits - the test project, the folders above it and the node itself - after
  the counts and in a quieter colour. The IDE's bar underneath shows a Testin
  editor's bare node name and no ancestors, because that bar is built from PSI
  and a Testin editor has none.

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

Then, after a dot and in a quieter colour, **where this editor's node sits**:

```
3 of 120 test cases (filtered from 340)  ·  NAFATH › Test Cases › Login
```

The counts come first because they are what changes and what a tester reads
constantly. The path comes second because it does not change while the editor is
open, so on a narrow editor it is the half that gets cut — and it is the half a
tester can widen the window once to read.

### Why it is here and not in the IDE's bar

The IDE's own bar, underneath this one, shows a Java file's whole breadcrumb —
`testin_example > src > test > java > nafath > LoginTest` — and for a Testin
editor shows the node's bare name with no ancestors. That bar is built from PSI
and a Testin editor deliberately has none, so it has nothing to walk up.

Teaching it about a Testin node means implementing `NavBarItemProvider`, which
the platform marks **`@ApiStatus.Internal`** — no deprecation cycle, and a break
on any IDE update. Testin declares `sinceBuild` and no upper bound on purpose, so
a tester would be carried onto the version that broke it and told nothing. The
path goes in the bar Testin owns instead (#161).

## What Testin refuses

**If the tester right-clicks inside the selection** — the menu acts on the whole
selection, not on the card under the pointer.

**If the tester right-clicks outside the selection** — the selection moves to
that card first, then the menu opens.

**With nothing selected** — the status bar reads the count alone, *12 test
cases*. It used to read *0 of 12 test cases*, putting a position where a tester
reads a count.

---

[Documentation](../README.md) › [The editor panel](main.md)
