[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-041

# UC-EDITOR-PANEL-041: Type an actual result into the grid

**As a** tester, **I want** to write what happened straight into the table,
**so that** noting five results does not need five dialogs.

`Enter` on the cell, in the grid, in a test run editor.

## Rules

- **Rule-EDITOR-PANEL-168** — **Actual Result** is the only column of a test run
  that can be typed into.
- **Rule-EDITOR-PANEL-169** — Typing there does not change the verdict.
- **Rule-EDITOR-PANEL-170** — A cell tabbed through unchanged writes nothing and
  says nothing.
- **Rule-EDITOR-PANEL-171** — What is stored is written back into the cell,
  whatever the tester typed.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester switches the test run editor to the grid.
2. The tester puts the cursor on an **Actual Result** cell and presses `Enter`.
3. The cell opens with the cursor in it.
4. The tester types. `Ctrl+Enter` adds a line.
5. The tester presses `Enter`.
6. The value is written onto the test run and saved.
7. A message reads *Updated*.
8. The cards behind the grid, and the view panel, both catch up.

## What Testin refuses

**If the column is any other** — the cell does not open. Every other column of a
test run is read only.

**If the test case was deleted from its test set** — the cell is put back to
what it held, and a message reads *The test case was removed - the run keeps
what it recorded.*

**If nothing really changed** — nothing is saved and nothing is said.

**If `Escape` is pressed while the cell is open** — the edit is thrown away.

**If any menu key is pressed while the cell is open** — it is refused. `P`, `F`
and `B` do nothing until the cell is closed.

## Why so little can be typed into

A test run records what happened. The verdict, the duration, who ran it and when
are all recorded by the act of judging, and typing them in would let the record
say something nobody did. The actual result is the one thing a tester writes in
their own words, so it is the one thing the grid lets them type.

---

[Documentation](../README.md) › [The editor panel](main.md)
