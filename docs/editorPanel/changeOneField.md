[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-006

# UC-EDITOR-PANEL-006: Change one field of one test case

**As a** tester, **I want** to correct one field without opening a form of
twenty, **so that** fixing a typo takes two keys.

`F2` opens the menu of fields. Each field also has a letter of its own.

## Rules

- **Rule 34** — Only the field the tester opened may be written back. Every
  other field on the dialog is gray.
- **Rule 35** — The dialog always shows the description, and shows the expected
  result when it is not empty, so the tester can see what they are changing.
- **Rule 36** — A save that changed nothing writes nothing and says nothing.
- **Rule 37** — One gesture is one entry on the undo history, however many test
  cases it changed.
- **Rule 38** — Undo puts the test case back exactly, including who last changed
  it and when.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌────────────────────────────────────────┐
│  Update Test Case                      │
├────────────────────────────────────────┤
│  Description             D             │
│  Expected Result         E             │
│  Module                  M             │
│  Test Data               T             │
│  Pre Conditions          B             │
│  Steps                   S             │
│  Priority                P             │
│  Group                   G             │
│  Order                   O             │
└────────────────────────────────────────┘
```

1. **Each row** — the field, then the letter that opens it.
2. **The first row** — selected when the menu opens.

Pressing the letter on the card skips this menu and opens the field straight
away.

## Main flow

1. The tester selects a card.
2. The tester presses `E`, or presses `F2` and picks **Expected Result**.
3. The **Update Expected Result** dialog opens with the current value in it.
4. Every other field on the dialog is gray.
5. The tester types and presses `Enter`.
6. Testin writes the test case.
7. A message reads *Updated*.
8. The list is sorted again and redrawn, and the automation code is rewritten.

## What Testin refuses

**If nothing is selected** — **Update** is gray and `F2` does nothing.

**If the save changed nothing** — the dialog closes and nothing at all happens.

**If the description is emptied** — the field turns red and the dialog stays
open.

**If the description cannot name a Java method** — the same message the create
dialog shows.

**If several test cases are selected** — the menu title becomes **Update**, then
the count, then **Test Cases**, and picking a field opens the bulk editor
instead. That is [UC-EDITOR-PANEL-007](bulkEdit.md).

## The dialog cannot be moved

The update dialog cannot be moved or resized. The create dialog can. Nothing
explains the difference.

---

[Documentation](../README.md) › [The editor panel](main.md)
