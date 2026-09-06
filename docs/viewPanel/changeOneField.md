[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-011

# UC-VIEW-PANEL-011: Change one field without leaving the panel

**As a** tester, **I want** to correct a field I have just noticed is wrong,
**so that** I do not have to go back to the editor and find the test case again.

`F2` opens the menu of fields.

## Rules

- **Rule 44** — `F2` works only once a test case has been drawn. It does nothing
  while the panel is empty.
- **Rule 45** — The panel always changes exactly one test case, because it only
  ever shows one.
- **Rule 46** — A save that would leave the file as it is writes nothing, and
  raises no message.
- **Rule 47** — A saved change is one entry on the undo history.
- **Rule 48** — Saving a change rewrites the automation code for that field,
  where the field has any.

Rules 1 to 9 hold everywhere in the panel. They are on
[the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

1. **Each row** — the field's name, then the letter that opens it.
2. **The first row** — selected when the menu opens.

Choosing a row opens that field's own editor. Those editors belong to the editor
panel and are drawn on
[UC-EDITOR-PANEL-007](../editorPanel/changeOneField.md).

## Main flow

1. The panel is showing a test case, and the keyboard is in the panel.
2. The tester presses `F2`.
3. The **Update Test Case** menu opens, one row for each field.
4. The tester presses the field's letter, or picks the row.
5. That field's editor opens with the current value in it.
6. The tester types and presses `Enter`.
7. Testin writes the test case and shows *Updated*.
8. The panel redraws with the new value.

## What Testin refuses

**If the panel is showing no test case** — `F2` does nothing.

**If the panel has never drawn a test case** — `F2` is not bound yet. It becomes
live the first time a test case is drawn.

**If the save would change nothing** — nothing is written, no message is raised,
and nothing goes on the undo history.

## Where the plugin breaks its own rules

**A change Testin cannot place is dropped in silence.** If neither the test
case nor the panel's path names a test set, nothing is written. There is no
message, no balloon and nothing on screen. Only the log records it. That is
difference 10 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

**`Escape` cannot close the panel afterwards.** Using `F2` needs the keyboard to
be in the panel, and `Escape` does nothing there. That is difference 2.

---

[Documentation](../README.md) › [The view panel](main.md)
