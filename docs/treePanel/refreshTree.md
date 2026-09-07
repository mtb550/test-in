[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-025

# UC-TREE-PANEL-025: Refresh the tree from disk

> **No key.** Press **Refresh** at the top of the panel.

**As a** tester, **I want** to reload the tree after something changed outside
the IDE, **so that** the tree shows what is on disk. Getting changes from Git,
running a sync, or editing files by hand all change the tree from outside.

## Rules

- **Rule-TREE-PANEL-068** — Refresh checks again which test project this code
  project uses, first. So a test project changed by hand, or changed by a branch
  switch, is picked up.
- **Rule-TREE-PANEL-069** — Editors on a node that no longer exists are closed.
  The other editors are reloaded, unless a tester is in the middle of something.
- **Rule-TREE-PANEL-070** — Only one refresh runs at a time. A second request
  while one is running is ignored.

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
