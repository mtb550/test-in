[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-041

# UC-EDITOR-PANEL-041: Type an actual result into the grid

**As a** tester, **I want** to write what happened straight into the table,
**so that** noting five results does not need five dialogs.

**Actual Result** is the only column of a test run a tester can type into.

`Enter` on the cell, in the grid, in a test run editor.

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
- **Rule-EDITOR-PANEL-171** — **Actual Result** is the only column of a test run
  that can be typed into.
- **Rule-EDITOR-PANEL-172** — Typing there does not change the verdict.
- **Rule-EDITOR-PANEL-173** — A cell tabbed through unchanged writes nothing and
  says nothing.
- **Rule-EDITOR-PANEL-174** — What is stored is written back into the cell,
  whatever the tester typed.

## The screen

No dialog opens. The cell itself becomes a box.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  #  | Description               | Run Status  | Actual Result            │
├──────────────────────────────────────────────────────────────────────────┤
│  1  | Log in with a valid user  | Passed      |                          │
│  2  | Log in with a locked acc. | Failed      | [The session was dropped]│
│  3  | Log in with wrong passwo. | Pending     |                          │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The open cell** — a box with a blue outline, with the cursor in it.
2. **Every other column** — read only in a test run. Those cells never open.
3. **The row** — grows taller when the tester adds a line with `Ctrl+Enter`.
4. **The keyboard** — belongs to the open cell. `P`, `F` and `B` do nothing
   until it is closed.

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
are all written by the act of judging. Typing them in would let the record say
something nobody did. The actual result is the one thing a tester writes in
their own words, so it is the one thing the grid lets them type.

---

[Documentation](../README.md) › [The editor panel](main.md)
