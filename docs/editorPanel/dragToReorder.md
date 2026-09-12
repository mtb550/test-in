[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-010

# UC-EDITOR-PANEL-010: Reorder test cases by dragging

**As a** tester, **I want** to drag a test case where it belongs,
**so that** the test set reads in the order somebody would work through it.

The cards are moved with the mouse. Nothing opens.

There is no key for this. Drag the cards.

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
- **Rule-EDITOR-PANEL-057** — Dragging works on cards only. The grid cannot be
  dragged.
- **Rule-EDITOR-PANEL-058** — A drag is always a move, never a copy.
- **Rule-EDITOR-PANEL-059** — A card lands directly under the card it was
  dropped on. Under a filter that is the card the tester can see, so test cases
  the filter is hiding between the two move down rather than staying above it.
- **Rule-EDITOR-PANEL-060** — Only the test cases that really moved are written.
- **Rule-EDITOR-PANEL-061** — The whole drag is one entry on the undo history.

## What the tester sees

This opens no screen. While the tester drags, the card follows the pointer, and
a line shows where it will land. When the button is released, the cards are
drawn again in their new order and every number is worked out again.

A small message then appears at the bottom of the IDE and fades. It reads
*Re-sorted*, with a count after it when more than one card moved.

## Main flow

1. The tester selects one or more cards.
2. The tester drags them up the list and drops them.
3. The cards land above the first card at or after the drop point.
4. Testin gives a new place to each test case that moved. Only those files are
   written.
5. Testin rewrites the position in every generated test method in that test set.
6. A message reads *Re-sorted*, with a count when more than one card moved.
7. The first dragged card is selected.

## What Testin refuses

**If the drag did not start in this list** — nothing happens.

**If the drag carries something that is not test cases** — nothing happens.

**If anything fails on the way** — nothing is said, and only the log records it.

## Why only some files are written

A test case carries its own place in the order rather than pointing at the one
before it. There is always room for a new place between any two, so dropping one
card into a test set of 200 writes one file, not 200.

---

[Documentation](../README.md) › [The editor panel](main.md)
