[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-016

# UC-TREE-PANEL-016: Undo a change to the tree

> **`Ctrl+Z`**. On the menu: **Actions → Undo**, which names what it will undo,
> as in *Undo Move 'Login'* or *Undo Remove 3 items*.

**As a** tester, **I want** to take back the last change I made to the tree,
**so that** a wrong move, rename or removal costs nothing.

This puts the tree back the way it was before the last change.

## Rules

- **Rule-TREE-PANEL-001** — The panel shows exactly one test project. It is the
  one this repository is bound to. There is no list of test projects in the
  tree.
- **Rule-TREE-PANEL-002** — **Test Cases** and **Test Runs** are fixed
  containers. They cannot be created, renamed, moved, copied or removed. They
  come with the test project and go with it.
- **Rule-TREE-PANEL-003** — The tree has two sides, and nothing moves between
  them. Test sets and test set packages live under **Test Cases**. Test runs and
  test run packages live under **Test Runs**.
- **Rule-TREE-PANEL-004** — Two nodes under one parent cannot share a name. This
  holds whether the node is created, renamed, pasted or dropped.
- **Rule-TREE-PANEL-005** — A node name is never empty.
- **Rule-TREE-PANEL-006** — Removing, moving or copying asks first. Nothing in
  the tree changes until the tester confirms.
- **Rule-TREE-PANEL-007** — When something changes, Testin says so once, in the
  past tense. The tester sees *Created*, *Renamed* or *Removed*. Several changes
  at once confirm once, with a count: the tester sees *Removed 4*, never four
  messages. Looking at something confirms nothing.
- **Rule-TREE-PANEL-008** — A retired node keeps everything inside it. Retired
  means a **Deprecated** test set or an **Archived** package. It is drawn gray.
  It sorts after live nodes. It is not offered when a test run is created.
- **Rule-TREE-PANEL-009** — A test run that is **Completed** or **Closed** is
  signed off. Its test cases, verdicts and configuration can no longer change.
- **Rule-TREE-PANEL-010** — Siblings are shown in one order:
  1. live nodes, then retired ones
  2. the number the tester gave the node
  3. the date the node was created
  4. the name
- **Rule-TREE-PANEL-011** — A removed node goes to the desktop's recycle bin. It
  can be put back from the tree.
- **Rule-TREE-PANEL-012** — A test case is not a node in this tree. It is
  reached by opening its test set.
- **Rule-TREE-PANEL-013** — Nodes move only within one test project. Nothing cut
  in one test project can be pasted into another.
- **Rule-TREE-PANEL-059** — The tree keeps one history of its last 20 changes,
  per code project and separate from any editor. It is held in memory, so
  closing the IDE loses it, and the copies kept aside for restoring removed
  nodes are deleted at the next start.
- **Rule-TREE-PANEL-060** — Four things can be undone: a move, a rename, a
  removal, and an edit of a test run. Four cannot: creating anything, an order
  number, a copy, and a status change.

## What the tester sees

Undo opens no screen of its own. On the **Actions** menu, the entry names what
it will take back, as in *Undo Move 'Login'* or *Undo Remove 3 items*. After
`Ctrl+Z` the tree redraws with the change reversed, and *Undone* shows above the
status bar at the bottom right of the IDE.

## Main flow

1. The last change to the tree was a move, a rename, a removal or an edit of a
   test run.
2. The tester presses `Ctrl+Z`, or chooses **Actions → Undo \<what\>**.
3. Testin reverses the change. Moved nodes go back. A renamed node gets its old
   name. Removed nodes are restored from the copy kept aside. An edited test run
   gets its previous name, test cases and settings.
4. Testin shows *Undone*.

To put an undone change back, see [UC-TREE-PANEL-017](redoChange.md).

## What Testin refuses

**If the tree's history is empty** — the entry reads plain **Undo**, and is
gray. `Ctrl+Z` does nothing.

**If some removed nodes can no longer be put back** — the rest are restored.
*Undo Incomplete* is shown in red, with the line *N of M could not be put back*.
This happens when something already sits where the node used to be.

> **A rename or a move that failed on disk is still on the history.** The menu
> offers to undo it, and undoing does nothing useful.

> **After the IDE restarts, nothing can be undone.** The history and the copies
> behind it are both gone. A removed node is still in the desktop's recycle
> bin.

---

[Documentation](../README.md) › [The tree panel](main.md)
