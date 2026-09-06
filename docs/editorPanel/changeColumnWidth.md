[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-004

# UC-EDITOR-PANEL-004: Change a grid column's width

**As a** tester, **I want** to widen the column I am reading,
**so that** a long expected result is not cut off every time I open the editor.

There is no key for this. Drag the divider in the header.

## Rules

- **Rule 24** — A width the tester dragged is remembered, and comes back the
  next time the grid is built.
- **Rule 25** — Only a drag saves a width. A column Testin sized itself is not
  remembered.
- **Rule 26** — A column Testin sizes is made as wide as its content needs, up
  to 500 points.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester drags the divider between two column headers.
2. The column changes width, and every row is measured again.
3. The width is remembered against that column of that kind of editor.
4. Every later grid opens with that column at that width.

## What Testin refuses

Nothing. Any width can be dragged.

## What is remembered separately

A width is remembered per column name and per kind of editor. The Description
column in a test set editor and the Description column in a test run editor are
remembered apart, so the two can be different widths.

---

[Documentation](../README.md) › [The editor panel](main.md)
