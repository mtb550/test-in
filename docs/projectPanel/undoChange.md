[Documentation](../README.md) › [The project panel](main.md) › UC-016

# UC-016: Undo a change to the tree

> **`Ctrl+Z`**. On the menu: **Actions → Undo**, which names what it will undo,
> as in *Undo Move 'Login'* or *Undo Remove 3 items*.

**As a** tester, **I want** to take back the last change I made to the tree,
**so that** a wrong move, rename or removal costs nothing.

## Rules

- **Rule 48** — The tree remembers its own last 20 changes. It remembers them
  separately from any editor.
- **Rule 49** — Four things can be undone: a move, a rename, a removal, and an
  edit of a test run. Three cannot: an order number, a copy, and a status
  change.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The last change to the tree was a move, a rename, a removal or an edit of a
   test run.
2. The tester presses `Ctrl+Z`, or chooses **Actions → Undo \<what\>**.
3. Testin reverses the change. Moved nodes go back. A renamed node gets its old
   name. Removed nodes are restored from the copy kept aside. An edited test run
   gets its previous name, test cases and settings.
4. Testin shows *Undone*.

To put an undone change back, see [UC-017](redoChange.md).

## What Testin refuses

**If the tree's history is empty** — the entry reads plain **Undo**, and is gray.
`Ctrl+Z` does nothing.

**If some removed nodes can no longer be put back** — the rest are restored, and
*Undo Incomplete* is shown in red, with a line saying how many of them could not
be put back.

---

[Documentation](../README.md) › [The project panel](main.md)
