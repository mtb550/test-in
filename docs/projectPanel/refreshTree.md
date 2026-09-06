[Documentation](../README.md) › [The project panel](main.md) › UC-018

# UC-018: Refresh the tree from disk

**As a** tester, **I want** to reload the tree after something changed outside
the IDE, **so that** the tree shows what is on disk. Getting changes from Git,
running a sync, or editing files by hand all change the tree from outside.

## Rules

- **Rule 68** — Refresh checks again which test project this code project uses,
  first. So a test project changed by hand, or changed by a branch switch, is
  picked up.
- **Rule 69** — Editors on a node that no longer exists are closed. The other
  editors are reloaded, unless a tester is in the middle of something.
- **Rule 70** — Only one refresh runs at a time. A second request while one is
  running is ignored.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester presses the **Refresh** button in the panel header.
2. Testin checks again which test project this code project uses.
3. Testin reads the test project again, showing a progress bar reading *Testin
   indexing - \<project\>*.
4. Editors on nodes that are gone are closed, and the rest are reloaded.
5. The tree redraws, with the same rows expanded.
6. Testin shows *Refreshed* when it finishes.

**Expand and collapse.** **Expand All** opens every node except retired ones,
which stay closed. **Collapse All** closes every row under the test project, and
leaves the test project, **Test Cases** and **Test Runs** visible.

## What Testin refuses

**If a refresh is already running** — nothing happens.

---

[Documentation](../README.md) › [The project panel](main.md)
