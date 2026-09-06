[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-019

# UC-EDITOR-PANEL-019: Search the test cases

**As a** tester, **I want** to find a test case by a word in it,
**so that** I do not page through 200 of them looking for one.

`Ctrl+F`.

## Rules

- **Rule 87** — The list narrows three tenths of a second after the last
  keystroke, not on every letter.
- **Rule 88** — The search reads the description, the identity, the expected
  result and the steps. Nothing else.
- **Rule 89** — Searching goes back to the first page.
- **Rule 90** — `Escape` in the box returns the keyboard to the list and leaves
  the text where it is.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
