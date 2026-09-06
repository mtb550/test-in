[Documentation](../README.md) › [The project panel](main.md) › UC-011

# UC-011: Undo and redo a change to the tree

**As a** tester, **I want** to take back the last change I made to the tree,
**so that** a wrong move, rename or removal costs nothing.

## Rules

- **Rule 48** — The tree remembers its own last 20 changes. It remembers them
  separately from any editor.
- **Rule 49** — Four things can be undone: a move, a rename, a removal, and an
  edit of a test run. Three cannot: an order number, a copy, and a status change.
- **Rule 50** — Making a new change forgets everything that was undone.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The last change to the tree was a move, a rename, a removal or an edit of a
   test run.
2. The tester presses `Ctrl+Z`, or chooses **Actions → Undo \<what\>**. The menu
   entry names what it will undo, as in *Undo Move 'Login'* or *Undo Remove 3
   items*.
3. Testin reverses the change. Moved nodes go back. A renamed node gets its old
   name. Removed nodes are restored from the copy kept aside.
4. Testin shows *Undone*.
5. The tester presses `Ctrl+Y`, or chooses **Actions → Redo \<what\>**.
6. Testin re-applies the change, and shows *Redone*.

## What Testin refuses

**If a new change was made after the undo** — **Redo** is gray.

**If the tree's history is empty** — the entry reads plain **Undo**, and is gray.
`Ctrl+Z` does nothing.

**If some removed nodes can no longer be put back** — the rest are restored, and
*Undo Incomplete* is shown in red, with a line saying how many of them could not
be put back.

---

[Documentation](../README.md) › [The project panel](main.md)
