[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-016

# UC-EDITOR-PANEL-016: Cut test cases

**As a** tester, **I want** to move test cases into another test set,
**so that** a test case written in the wrong place ends up in the right one.

A cut on its own changes nothing. The test cases move when the tester pastes.

`Ctrl+Shift+X`.

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
- **Rule-EDITOR-PANEL-078** — A cut test case is drawn faded, so the tester can
  see what is waiting to move.
- **Rule-EDITOR-PANEL-079** — Nothing is removed until the paste. A cut on its
  own changes nothing.
- **Rule-EDITOR-PANEL-080** — A cut is called off by a paste, by a copy, and by
  `Escape`.

## What the tester sees

This opens no screen. The cut cards stay where they are and are drawn faded, so
the tester can see what is waiting to move.

A small message appears at the bottom of the IDE and fades. It reads *Cut*, with
a count after it for more than one test case.

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

It keeps its identity, so the verdicts in every test run still point at it. It
keeps its own test method too. Only its place changes.

## Undoing a move

The cut and the paste are one entry on the undo history, named as a move. One
press of `Ctrl+Z` puts the test cases back where they came from.

---

[Documentation](../README.md) › [The editor panel](main.md)
