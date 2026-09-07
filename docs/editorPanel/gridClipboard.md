[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-018

# UC-EDITOR-PANEL-018: Copy, cut and paste grid cells

**As a** tester, **I want** the grid to behave like a spreadsheet,
**so that** I can move a column of values between Testin and Excel.

Inside the grid these three keys act on cells, not on whole test cases.

`Ctrl+C`, `Ctrl+X` and `Ctrl+V`, inside the grid.

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
- **Rule-EDITOR-PANEL-086** — Cells are copied as text with a tab between
  columns and a line break between rows, which is what a spreadsheet reads.
- **Rule-EDITOR-PANEL-087** — A value holding a tab, a line break or a quote is
  wrapped in quotes.
- **Rule-EDITOR-PANEL-088** — One value on the clipboard fills every selected
  cell. A block is laid from the top left cell.
- **Rule-EDITOR-PANEL-089** — A cell that cannot be typed into is skipped, for
  both cut and paste.

## What the tester sees

This opens no screen. A copy changes nothing on screen at all. A cut empties the
cells it took, and a paste draws them with their new values.

A cut and a paste raise one small message for every cell they change. Each one
appears at the bottom of the IDE, reads *Updated*, and fades.

## Main flow

1. The tester drags across a block of cells in the grid.
2. The tester presses `Ctrl+C`.
3. The block goes on the clipboard as tab separated text.
4. The tester pastes it into a spreadsheet, and it lands as a table.
5. The tester copies a column back from the spreadsheet.
6. The tester selects the top cell in Testin and presses `Ctrl+V`.
7. The values are laid down the column, stopping at the last row.

## What Testin refuses

**If nothing is selected** — nothing happens.

**If the clipboard holds no text** — nothing happens.

**If a cell cannot be typed into** — it is skipped without a word, for a cut and
for a paste.

**If the block on the clipboard is bigger than the grid** — it is laid down as
far as the last row and the last column. The rest is dropped.

## Where the plugin breaks its own rules

**Every cell raises its own message.** Pasting a block of 20 cells raises 20
messages reading *Updated*. `Ctrl+X` over a block does the same. Every other
gesture in Testin raises one message with a count. That is difference 6 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

## These keys are the grid's

`Ctrl+C`, `Ctrl+X` and `Ctrl+V` mean cells in the grid, and test cases on the
cards. The keys that always mean test cases are `Ctrl+Shift+C`,
`Ctrl+Shift+X` and `Ctrl+Shift+V`, and they work in both views.

---

[Documentation](../README.md) › [The editor panel](main.md)
