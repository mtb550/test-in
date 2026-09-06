[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-016

# UC-EDITOR-PANEL-016: Cut test cases

**As a** tester, **I want** to move test cases into another test set,
**so that** a test case written in the wrong place ends up in the right one.

`Ctrl+Shift+X`.

## Rules

- **Rule 75** — A cut test case is drawn faded, so the tester can see what is
  waiting to move.
- **Rule 76** — Nothing is removed until the paste. A cut on its own changes
  nothing.
- **Rule 77** — A cut is called off by a paste, by a copy, and by `Escape`.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects two cards.
2. The tester presses `Ctrl+Shift+X`, or chooses **Cut Node**.
3. The two cards are drawn faded.
4. A message reads *Cut 2*.
5. The tester opens the other test set and presses `Ctrl+Shift+V`.
6. The two test cases leave the first test set and appear in the second.

## What Testin refuses

**If nothing is selected** — **Cut Node** is gray and the key does nothing.

**If writing to the clipboard fails** — nothing is said, and only the log
records it.

**If the tester never pastes** — nothing happens. The test cases stay where they
are, drawn faded until the cut is called off.

## What a moved test case keeps

Its identity, so its verdicts in every test run still point at it, and its test
method is still its own. Only its place changes.

## Undoing a move

The cut and the paste are one entry on the undo history, named as a move. One
press of `Ctrl+Z` puts the test cases back where they came from.

---

[Documentation](../README.md) › [The editor panel](main.md)
