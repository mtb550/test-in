[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-007

# UC-EDITOR-PANEL-007: Change one field on many test cases at once

**As a** tester, **I want** to correct the same field on 30 test cases in one
go, **so that** a renamed module does not cost me half an hour of typing.

Select several test cases, then `F2` or the field's own letter.

## Rules

- **Rule-EDITOR-PANEL-039** — The tester edits the values in place, in a list,
  with the original beside it.
- **Rule-EDITOR-PANEL-040** — A row the tester did not touch is not written, so
  it is not trimmed and its file is not changed.
- **Rule-EDITOR-PANEL-041** — A row the tester did edit has its spaces trimmed.
- **Rule-EDITOR-PANEL-042** — An edited value takes a green background, so it is
  clear what will be written.
- **Rule-EDITOR-PANEL-043** — A line break inside a value is shown as two
  characters and read back as a line break.
- **Rule-EDITOR-PANEL-044** — A test case with nothing in the field still gets a
  line to type into.
- **Rule-EDITOR-PANEL-045** — The whole gesture is one entry on the undo
  history.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

**If the cursor is put on the locked text around a value** — it is pulled to the
nearest place it can type. No message is shown.

**If a key would change the locked text** — nothing happens, and the platform's
own warning is suppressed.

## Where the plugin breaks its own rules

`Shift+Enter` also saves, and the strip along the bottom names only `Enter`.
That is difference 16 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

The group editor shows its values in capitals, and puts them under a heading
capitalized differently from every other bulk editor.

---

[Documentation](../README.md) › [The editor panel](main.md)
