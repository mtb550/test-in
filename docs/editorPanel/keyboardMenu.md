[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-029

# UC-EDITOR-PANEL-029: Open the menu from the keyboard

**As a** tester, **I want** the menu without reaching for the mouse,
**so that** a whole test run can be walked with two hands on the keyboard.

The `Context Menu` key.

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
- **Rule-EDITOR-PANEL-123** — The menu opens on whatever is selected, in both
  views and in the tree.
- **Rule-EDITOR-PANEL-124** — With nothing selected, nothing opens.

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
