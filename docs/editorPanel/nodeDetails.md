[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-028

# UC-EDITOR-PANEL-028: See the test set's own details

**As a** tester, **I want** to know what this test set holds and who made it,
**so that** I can answer a question about it without leaving the editor.

There is no key for this. The button is at the far right of the toolbar.

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
- **Rule-EDITOR-PANEL-121** — The dialog is about the node the editor is
  showing, not about the test case that is selected.
- **Rule-EDITOR-PANEL-122** — The counts are worked out as the dialog opens and
  stored nowhere.

## Main flow

1. The tester presses the last button on the toolbar.
2. The **Details** dialog opens on the test set the editor is showing.
3. It says the name, the path, who made it and when, its status, and how many
   test cases it holds.
4. `Escape` closes it.

The same dialog opens from the tree, and is drawn on
[UC-TREE-PANEL-027](../treePanel/nodeDetails.md). What the counts mean is on
[UC-INTERNAL-006](../internal/countNodeContents.md).

## What Testin refuses

Nothing.

## Where the plugin breaks its own rules

This button and the fields button are both tooltipped **Details**, on the same
toolbar. One picks which fields the rows show. This one opens a dialog about the
test set. That is difference 5 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

---

[Documentation](../README.md) › [The editor panel](main.md)
