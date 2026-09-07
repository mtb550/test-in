[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-020

# UC-TREE-PANEL-020: Set a test run's status

> **No key opens it.** On the menu: **Set Status**. Inside the popup,
> **`1`** is **Assigned**, **`2`** is **Completed** and **`3`** is
> **Closed**.

**As a** tester, **I want** to mark a test run **Assigned**, **Completed** or
**Closed** from the tree, **so that** the test run's place in its life is
visible without opening it.

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
- **Rule-TREE-PANEL-067** — **Completed** and **Closed** are final. The test run
  accepts no more verdicts. Every test case still **Pending** becomes
  **Untested**. (Rule-TREE-PANEL-009)
- **Rule-TREE-PANEL-068** — A tester sets **Assigned**, **Completed** and
  **Closed**. **Created** and **In Progress** are the test run's own record of
  itself.

## The Set Test Run Status popup

```
┌──────────────────────────────────────────────────────────────┐
│  Set Test Run Status                                         │
├──────────────────────────────────────────────────────────────┤
│  > +  Created                                           (1)  │
│    @  In Progress                                            │
│    *  Completed                                     2   (2)  │
│    !  Assigned                                      1        │
│    x  Closed                                        3        │
└──────────────────────────────────────────────────────────────┘
```

1. **Five rows, one per status** — each with its icon. It is the same icon the
   tree draws for a test run in that status.
2. **A key beside three of them** — *Created* and *In Progress* have none,
   because they are the test run's own record. The keys are not in order down
   the list, because the rows follow the status order instead: **Assigned** is
   `1`, **Completed** is `2`, **Closed** is `3`.
3. The popup opens in the middle of the IDE window, not at the pointer, with
   the first row selected. The tester chooses with `↑` `↓` and `Enter`, with a
   key, or with a click. **Nothing on screen says so**: the popup has no status
   bar.

## Main flow

1. The tester selects a test run that is **Created**, **In Progress** or
   **Assigned**.
2. The tester chooses **Set Status**.
3. The **Set Test Run Status** popup lists the five statuses. Three of them
   carry a key: `1` **Assigned**, `2` **Completed**, `3` **Closed**.
4. The tester presses that key, or moves with `↑` `↓` and presses `Enter`, or
   clicks a row.
5. Testin writes the status. The test run's icon and its gray status word in the
   tree both change, and Testin shows the new status word.
6. Setting **Completed** or **Closed** signs the test run off. Every test case
   still **Pending** becomes **Untested**. Testin records the time the test run
   finished, but only if it had been started. From then on **Set Status**,
   **Edit Run** and **Run Tests** are gray on it.

**Signing a test run off cannot be undone.** A status change is not on the
tree's history (Rule-TREE-PANEL-060), and there is no confirmation before it.
Every **Pending** verdict becomes **Untested** at that moment.

## What Testin refuses

**If the tester clicks outside the popup** — it closes, and nothing changes.

**If several rows are selected** — **Set Status** stays black and acts on the
last row the tester clicked.

> **The popup offers the status the test run already has.** Choosing it rewrites
> the record, stamps who changed it and when, and confirms with that word. Rule
> 54, which keeps the current status off the menu, holds for retiring and not
> here.

> **An open editor of that test run is not told.** It keeps showing the old
> status, and the rows that just became **Untested**, until it is reopened.

**Not decided** — see question 1 and question 2 on
[the tree panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The tree panel](main.md)
