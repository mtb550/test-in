[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-008

# UC-EDITOR-PANEL-008: Type straight into a grid cell

**As a** tester, **I want** to correct a value where I can see it,
**so that** fixing five expected results does not need five dialogs.

`Enter` on the cell, or a double click.

## Rules

- **Rule 46** — Only some columns can be typed into. The rest never open.
- **Rule 47** — `Ctrl+Enter` puts a line break in. `Enter` saves.
- **Rule 48** — Clicking away from an open cell saves it.
- **Rule 49** — What is stored is what Testin made of what was typed, and the
  cell is redrawn to match.
- **Rule 50** — A cell that ends up the same as it started writes nothing and
  says nothing.
- **Rule 51** — Every cell saved is one entry on the undo history, named after
  the test case.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Which columns can be typed into

| Can be typed into | Cannot |
|---|---|
| Description, Expected Result, Steps, Priority, Reference, Test Data, Pre Conditions, Group, Module, Status | Order, ID, FQCN, Path, Created By, Updated By, Created At, Updated At |

In a test run editor only **Actual Result** can be typed into.

## Main flow

1. The tester puts the cursor on a cell and presses `Enter`.
2. The cell opens as a box with a blue outline and the cursor in it.
3. The tester types. `Ctrl+Enter` adds a line, and the row grows to fit.
4. The tester presses `Enter`.
5. Testin makes what it can of the text and writes it into the test case.
6. The cell is redrawn with the stored value.
7. A message reads *Updated*.
8. The cards behind the grid, and the details panel, both catch up.

## What Testin refuses

**If the column cannot be typed into** — the cell does not open. `Enter` there
does nothing at all, and nothing is said.

**If the value did not really change** — nothing is saved and nothing is said.

**If `Escape` is pressed while the cell is open** — the edit is thrown away and
nothing else happens.

**If any menu key is pressed while the cell is open** — it is refused. The cell
owns the keyboard until it is closed.

**If the editor has no test set to write to** — nothing is written, and only the
log says so.

## What Testin makes of what is typed

| The tester types | What is stored |
|---|---|
| Steps, one to a line | One step for each line |
| A priority Testin does not know | The lowest priority |
| A status Testin does not know | Whatever the test case had already |
| A group Testin does not know | Dropped from the list |
| A description with characters Testin will not keep | Those characters removed |

## Where the plugin breaks its own rules

**A description can change on screen with nothing saved and nothing said.** The
characters Testin will not keep are taken out and the cell is redrawn. If
nothing else changed, no save happens and no message appears, so the tester
watches their text change for no stated reason. That is difference 7 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

**Two columns answer a typo two ways.** An unreadable priority becomes the
lowest. An unreadable status keeps the old value. That is difference 8.

---

[Documentation](../README.md) › [The editor panel](main.md)
