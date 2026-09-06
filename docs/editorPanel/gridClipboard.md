[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-018

# UC-EDITOR-PANEL-018: Copy, cut and paste grid cells

**As a** tester, **I want** the grid to behave like a spreadsheet,
**so that** I can move a column of values between Testin and Excel.

`Ctrl+C`, `Ctrl+X` and `Ctrl+V`, inside the grid.

## Rules

- **Rule 83** — Cells are copied as text with a tab between columns and a line
  break between rows, which is what a spreadsheet reads.
- **Rule 84** — A value holding a tab, a line break or a quote is wrapped in
  quotes.
- **Rule 85** — One value on the clipboard fills every selected cell. A block is
  laid from the top left cell.
- **Rule 86** — A cell that cannot be typed into is skipped, for both cut and
  paste.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

**If the block on the clipboard is bigger than the grid** — it is laid as far as
the last row and the last column, and the rest is dropped.

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
