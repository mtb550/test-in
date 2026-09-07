[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-019

# UC-EDITOR-PANEL-019: Search the test cases

**As a** tester, **I want** to find a test case by a word in it,
**so that** I do not page through 200 of them looking for one.

`Ctrl+F`.

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
- **Rule-EDITOR-PANEL-090** — The list narrows three tenths of a second after
  the last keystroke, not on every letter.
- **Rule-EDITOR-PANEL-091** — The search reads the description, the identity,
  the expected result and the steps. Nothing else.
- **Rule-EDITOR-PANEL-092** — Searching goes back to the first page.
- **Rule-EDITOR-PANEL-093** — `Escape` in the box returns the keyboard to the
  list and leaves the text where it is.

## Main flow

1. The tester presses `Ctrl+F`.
2. The cursor moves to the search box and what is in it is selected.
3. The tester types a word.
4. Three tenths of a second later the list narrows to the test cases holding it.
5. The status bar says how many are left, of how many the test set holds.
6. The tester presses `Escape`, and the keyboard goes back to the list.

## What Testin refuses

**If nothing matches** — the middle reads *No test cases match the search*.

**If the tester clears the filters** — the search text stays. Clearing the
filters and clearing the search are two different things.

## What is not searched

The module, the group, the test data and the pre-conditions are not searched,
though each has its own column and three of them have their own filter. A tester
looking for a module has to use the filter instead. That is difference 17 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

## Where the plugin breaks its own rules

**Refresh throws the search away.** Pressing the refresh button clears the box
and every filter with it, and the message afterwards says only *Refreshed*. That
is difference 13.

---

[Documentation](../README.md) › [The editor panel](main.md)
