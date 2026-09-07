[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-028

# UC-EDITOR-PANEL-028: See the test set's own details

**As a** tester, **I want** to know what this test set holds and who made it,
**so that** I can answer a question about it without leaving the editor.

There is no key for this. The button is at the far right of the toolbar.

## Rules

- **Rule-EDITOR-PANEL-118** — The dialog is about the node the editor is
  showing, not about the test case that is selected.
- **Rule-EDITOR-PANEL-119** — The counts are worked out as the dialog opens and
  stored nowhere.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
