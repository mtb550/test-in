[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-029

# UC-EDITOR-PANEL-029: Open the menu from the keyboard

**As a** tester, **I want** the menu without reaching for the mouse,
**so that** a whole test run can be walked with two hands on the keyboard.

The `Context Menu` key.

## Rules

- **Rule-EDITOR-PANEL-121** — The menu opens on whatever is selected, in both
  views and in the tree.
- **Rule-EDITOR-PANEL-122** — With nothing selected, nothing opens.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects a card.
2. The tester presses the `Context Menu` key.
3. The menu opens a quarter of the way across the selected card.
4. The tester moves down it with the arrow keys and presses `Enter`.

In the grid the menu opens on the selected cell instead.

## What Testin refuses

**If nothing is selected** — nothing opens, and nothing is said.

## What the menu holds

In a test set editor, in this order: **Create Test Case**, **View Details**,
**Update**, **Copy**, **Copy Node**, **Cut Node**, **Paste Node**, **Delete**,
**Undo**, **Redo**, **Automate Test Case**, **Run Test Case**, **Navigate to
Code**, **Next page**, **Previous page**.

The last group of three is not there at all in an IDE without the Java and
TestNG plugins.

In a test run editor the menu holds the three verdicts first, then **Failed Test
Case Details**, then the rest.

---

[Documentation](../README.md) › [The editor panel](main.md)
