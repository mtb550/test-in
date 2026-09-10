[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-029

# UC-EDITOR-PANEL-029: Open the menu from the keyboard

**As a** tester, **I want** the menu without reaching for the mouse,
**so that** a whole test run can be walked with two hands on the keyboard.

It is the same menu the right button opens. The key just opens it from the
keyboard.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-123** — The menu opens on whatever is selected, in both
  views and in the tree.
- **Rule-EDITOR-PANEL-124** — With nothing selected, nothing opens.
- **Rule-EDITOR-PANEL-213** — The clipboard entries and the two history entries
  are gathered under one Actions entry that opens onto them, so the top level of
  the menu is what a tester opens it for. Every key those entries carry still
  works, on the cards and in the grid.
- **Rule-EDITOR-PANEL-214** — Actions holds the same seven entries in both
  editors. Cut Node, Paste Node and Delete are gray in a test run editor with
  the reason on the entry, because a test run's test cases were chosen when it
  was created and it keeps what it recorded. They are shown and refused rather
  than left out, so a tester who learns the gesture in one editor can find it in
  the other.

## The screen

This is the menu in a test set editor.

```
┌──────────────────────────────────┐
│  Create Test Case                │
│  View Details                    │
│  ──────────────────────────────  │
│  Update                      >   │
│  Actions                       > │
│  ──────────────────────────────  │
│  Automate Test Case              │
│  Run Test Case                   │
│  Navigate to Code                │
└──────────────────────────────────┘
```

1. **Where it opens** — a quarter of the way across the selected card, or on the
   selected cell in the grid.
2. **The separator lines** — they group entries that belong together.
3. **The last group of three** — not drawn at all in an IDE without the Java and
   TestNG plugins.
4. **Moving in it** — the arrow keys move down the entries, and `Enter` chooses
   one.

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
**Update**, **Actions**, **Automate Test Case**, **Run Test Case**, **Navigate
to Code**.

**Actions** opens onto the seven that act on the test case rather than on the
automation: **Copy**, **Copy Node**, **Cut Node**, **Paste Node**, **Delete**,
then **Undo** and **Redo**. They are one level down because they all have keys
and none of them is what the menu is opened for — at the top level they pushed
Automate, Run and Navigate to Code off the end of a list nobody read that far
down.

**The same seven are in the test run editor's menu**, in the same place and
under the same word. Three of them are gray there, each saying why on the entry:

| Entry | Why it is gray in a test run |
|---|---|
| **Cut Node** | A test run's test cases were chosen when it was created. Cut the test case in its test set. |
| **Paste Node** | The same. Paste into a test set. |
| **Delete** | A test run keeps what it recorded, including for a test case that is gone. Delete the test case in its test set. |

Shown and refused rather than left out. A menu that changes shape between
editors teaches a tester nothing, and they cannot learn that the gesture exists
or where it does work.

Each entry decides this for itself, from the node its editor is open on — the
same flag Import and Export read to find out whether a node can hold test
cases.

Paging is not on it. It moves the view and does nothing to the test case the
tester right-clicked, and the status bar already draws four arrows that each
print their own key.

The last group of three is not there at all in an IDE without the Java plugin
and the TestNG plugin.

In a test run editor the menu holds the three verdicts first, then **Failed Test
Case Details**, then the rest.

---

[Documentation](../README.md) › [The editor panel](main.md)
