[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-007

# UC-EDITOR-PANEL-007: Change one field on many test cases at once

**As a** tester, **I want** to correct the same field on 30 test cases in one
go, **so that** a renamed module does not cost me half an hour of typing.

The bulk editor shows one field of every selected test case as a list. The
tester edits the list, and one save writes them all.

Select several test cases, then `F2` or the field's own letter.

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
- **Rule-EDITOR-PANEL-040** — The tester edits the values in place, in a list,
  with the original beside it.
- **Rule-EDITOR-PANEL-041** — A row the tester did not touch is not written, so
  it is not trimmed and its file is not changed.
- **Rule-EDITOR-PANEL-042** — A row the tester did edit has its spaces trimmed.
- **Rule-EDITOR-PANEL-043** — An edited value takes a green background, so it is
  clear what will be written.
- **Rule-EDITOR-PANEL-044** — A line break inside a value is shown as two
  characters and read back as a line break.
- **Rule-EDITOR-PANEL-045** — A test case with nothing in the field still gets a
  line to type into.
- **Rule-EDITOR-PANEL-046** — The whole gesture is one entry on the undo
  history.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Bulk Edit Descriptions                                                    │
├──────────────────────────────────┬─────────────────────────────────────────┤
│ 1  [                             │ 1  [                                    │
│ 2    {                           │ 2    {                                  │
│ 3      "id": "3f2a...",          │ 3      "id": "3f2a...",                 │
│ 4      "description": "Log in"   │ 4      "description": "Sign in"         │
│ 5    },                          │ 5    },                                 │
│ 6    {                           │ 6    {                                  │
│ 7      "id": "8b44...",          │ 7      "id": "8b44...",                 │
│ 8      "description": "Log out"  │ 8      "description": "Log out"         │
│ 9    }                           │ 9    }                                  │
│10  ]                             │10  ]                                    │
├──────────────────────────────────┴─────────────────────────────────────────┤
│  [k] Enter Save   Tab Next   Ctrl+Shift+A All Carets   Escape Cancel       │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The left side** — the values as they are now. It cannot be typed into.
2. **The right side** — the same values, and only the values can be typed into.
   Everything around them is locked.
3. **A changed value** — takes a green background.
4. **The line facing the cursor** — highlighted on the left, so the two sides
   line up.

## The eight bulk editors

**Bulk Edit Descriptions**, **Bulk Edit Expected Results**, **Bulk Edit
Modules**, **Bulk Edit Test Data**, **Bulk Edit Pre-Conditions**, **Bulk Edit
Steps**, **Bulk Edit Priorities**, **Bulk Edit Group**.

The last two of those hold lists, so they also answer `Ctrl+Enter` to add an
item and `Shift+Delete` to drop one.

## Main flow

1. The tester selects 30 test cases and presses `M`.
2. **Bulk Edit Modules** opens with the 30 values on each side.
3. The tester presses `Ctrl+Shift+A` to put a cursor at the end of every value.
4. The tester types the new module once, and every value changes.
5. Each changed value turns green.
6. The tester presses `Enter`.
7. Only the rows that changed are written.
8. A message reads *Updated*.

## What Testin refuses

**If the tester chooses Order** — a message reads *Order is set one test case at
a time*. There is no bulk editor for it.

**If a description is edited to nothing** — that row is skipped without a word.

**If a priority is edited to nothing** — that row is skipped without a word.

**If a group name is not one Testin knows** — it is dropped, and only the log
says so.

**If the cursor is put on the locked text around a value** — it moves to the
nearest place the tester can type. No message is shown.

**If a key would change the locked text** — nothing happens. The platform's own
warning is hidden.

## Where the plugin breaks its own rules

`Shift+Enter` also saves, and the strip along the bottom names only `Enter`.
That is difference 16 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

The group editor shows its values in capitals. Its heading is capitalized
differently from every other bulk editor.

---

[Documentation](../README.md) › [The editor panel](main.md)
