[Documentation](../README.md) › [The project panel](main.md) › UC-014

# UC-014: Copy nodes

> **`Ctrl+C`** copies and **`Ctrl+V`** pastes. Dragging with `Ctrl` held copies
> too. On the menu: **Actions → Copy** and **Paste**.

**As a** tester, **I want** to copy a node into another folder, **so that** I can
start from something that already exists instead of writing it again.

## Rules

- **Rule 41** — A copied test case is a new test case, with its own id. Editing
  the copy never changes the original.
- **Rule 84** — A copy cannot be undone. To take one back, remove it, which is
  [UC-012](removeNode.md).

Rules 37, 38, 39 and 43 hold here too. They say where a node can land, and they
are on [UC-013](moveNodes.md).

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The dialog is drawn under [UC-013](moveNodes.md). After a copy its title is
**Paste**, and it asks *Copy N items into '\<folder\>'?*.

## Main flow

1. The tester selects one or more test sets, packages or test runs.
2. The tester presses `Ctrl+C`, or chooses **Actions → Copy**. Nothing in the
   tree changes, and Testin shows *Copied*, or *Copied N* for several.
3. The tester selects a folder that can hold them.
4. The tester presses `Ctrl+V`, or chooses **Actions → Paste**.
5. The **Paste** dialog asks what will be copied, and where to.
6. The tester presses `Enter`. The nodes are duplicated, and Testin shows
   *Pasted*, or *Pasted N*.
7. Every test case in the copy gets its own id, and the copy is selected in the
   tree.

**By dragging.** Holding `Ctrl` while releasing a dragged node titles the dialog
**Copy**, and `Enter` copies instead of moving.

## What Testin refuses

The same three refusals as a move, and they are on
[UC-013](moveNodes.md#what-testin-refuses).

---

[Documentation](../README.md) › [The project panel](main.md)
