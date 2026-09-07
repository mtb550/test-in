[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-025

# UC-TREE-PANEL-025: Refresh the tree from disk

> **No key.** Press **Refresh** at the top of the panel.

**As a** tester, **I want** to reload the tree after something changed outside
the IDE, **so that** the tree shows what is on disk. Getting changes from Git,
running a sync, or editing files by hand all change the tree from outside.

Testin reads the folders again and draws the tree from what it finds.

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
- **Rule-TREE-PANEL-081** — Refresh checks again which test project this code
  project uses, first. So a test project changed by hand, or changed by a branch
  switch, is picked up.
- **Rule-TREE-PANEL-082** — Editors on a node that no longer exists are closed.
  The other editors are reloaded, unless a tester is in the middle of something.
- **Rule-TREE-PANEL-083** — Only one refresh runs at a time. A second request
  while one is running is ignored.

## What the tester sees

Refresh opens no screen of its own. A progress bar reads *Testin indexing -
\<project\>* while Testin reads the test project. The tree then redraws, with
the same rows open as before. *Refreshed* shows above the status bar at the
bottom right of the IDE.

## Main flow

1. The tester presses the **Refresh** button in the panel header.
2. Testin checks again which test project this code project uses.
3. Testin reads the test project again, showing a progress bar reading *Testin
   indexing - \<project\>*.
4. Editors on nodes that are gone are closed, and the rest are reloaded.
5. The tree redraws, with the same rows expanded.
6. Testin shows *Refreshed* when it finishes.

## What Testin refuses

**If a refresh is already running** — nothing happens.

---

[Documentation](../README.md) › [The tree panel](main.md)
