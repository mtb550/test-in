[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-010

# UC-EDITOR-PANEL-010: Reorder test cases by dragging

**As a** tester, **I want** to drag a test case where it belongs,
**so that** the test set reads in the order somebody would work through it.

There is no key for this. Drag the cards.

## Rules

- **Rule 55** — Dragging works on cards only. The grid cannot be dragged.
- **Rule 56** — A drag is always a move, never a copy.
- **Rule 57** — Only the test cases that really moved are written.
- **Rule 58** — The whole drag is one entry on the undo history.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects one or more cards.
2. The tester drags them up the list and drops them.
3. The cards land above the first card at or after the drop point.
4. Testin gives new places to the test cases that moved, and writes only those.
5. Testin rewrites the position in every generated test method in that test set.
6. A message reads *Re-sorted*, with a count when more than one card moved.
7. The first dragged card is selected.

## What Testin refuses

**If the drag did not start in this list** — nothing happens.

**If the drag carries something that is not test cases** — nothing happens.

**If anything fails on the way** — nothing is said, and only the log records it.

## Where the plugin breaks its own rules

**A card can land somewhere the tester cannot see.** With a filter on, the drop
lands after whatever the filter is hiding between the two visible cards. The
move is saved and the message says *Re-sorted*. That is difference 14 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

**This says *Re-sorted* and typing a position says *Updated*.** That is
difference 15.

## Why only some files are written

A test case carries its own place in the order rather than pointing at the one
before it. There is always room for a new place between any two, so dropping one
card into a test set of 200 writes one file, not 200.

---

[Documentation](../README.md) › [The editor panel](main.md)
