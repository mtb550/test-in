[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-002

# UC-EDITOR-PANEL-002: Switch between cards and a grid

**As a** tester, **I want** the same test cases as a table,
**so that** I can compare many at once and correct them in columns.

There is no key for this. The button is on the toolbar.

## Rules

- **Rule 15** — Only one of the two buttons is on the toolbar at a time. It is
  always the view the tester is not in.
- **Rule 16** — The grid is built the first time the tester asks for it, not
  when the editor opens.
- **Rule 17** — The two views show the same rows, and the selection follows from
  one to the other.
- **Rule 18** — Anything half typed in a grid cell is saved, not thrown away,
  before the grid is rebuilt.
- **Rule 19** — The grid has one column for each field the tester chose to show,
  in a fixed order.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  #  | Description                | Expected Result       | Priority | Group│
├────────────────────────────────────────────────────────────────────────────┤
│  1  | Log in with a valid user   | The dashboard opens.  | P1       | Smoke│
│  2  | Log in with a locked accou.| The account is refus. | P2       |      │
│  3  | Log in with the wrong pass.| The password is refu. | P1       | Smoke│
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The first column** — the test case's number in the test set. Clicking it
   selects the whole row.
2. **Every other column** — one field. Some can be typed into, some cannot.
3. **The rows** — as tall as their tallest wrapped cell. Text wraps rather than
   being cut off.

## Main flow

1. The tester presses the grid button on the toolbar.
2. Testin builds the table from the test cases on this page.
3. Each column is made as wide as its content needs, up to a limit.
4. The selection the cards had is carried across.
5. The keyboard moves into the table.
6. The button on the toolbar becomes the one that goes back to cards.

## What Testin refuses

**If the grid cannot be built** — the view the tester was in stays on screen,
and only the log says why.

## What is different in each view

| | Cards | Grid |
|---|---|---|
| Reading one test case | Every field, one under the other | One row, cut to the column width |
| Correcting a value | `F2` or the field's letter | Type straight into the cell |
| Dragging to reorder | Yes | No |
| `Ctrl+C` | Copies the details as text | Copies the cells |
| Selecting | One or more cards | Cells, or whole rows from the first column |

---

[Documentation](../README.md) › [The editor panel](main.md)
