[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-027

# UC-TREE-PANEL-027: See what a node holds

> **No key.** On the menu: **Details**.

**As a** tester, **I want** to see a node's counts, dates, status and verdict
breakdown without opening anything, **so that** I can see how big a part of the
tree is at a glance.

It is a read-only window. Nothing in it can be changed.

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
- **Rule-TREE-PANEL-099** — A node says its status beside its name when the status
  is not active. An inactive test project, a deprecated test set and an archived
  package each say which they are, in gray; a node in current work says nothing,
  because "Active" on every name is a word read a hundred times and needed
  never. A test run always says its status, because where a cycle stands is what
  the tree is read for.
- **Rule-TREE-PANEL-087** — Opening **Details** changes nothing, so Testin says
  nothing. (Rule-TREE-PANEL-007)
- **Rule-TREE-PANEL-088** — Testin counts what a node holds when the tester
  asks. It never saves the number.

## The Details dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Details                                                     │
├──────────────────────────────────────────────────────────────┤
│  Name              cycle-2                              (1)  │
│  Path              C:\Testin\Demo\Test Runs\cycle-2          │
│  Created By        Muteb                                (2)  │
│  Created At        12 Aug 2026 09:14                         │
│  Updated By        Muteb                                     │
│  Updated At        14 Aug 2026 16:02                         │
│  Status            In Progress                          (3)  │
│  Execution Started 12 Aug 2026 10:00                    (4)  │
│  Execution Ended   12 Aug 2026 13:42                         │
│  Execution Time    03:42:00                                  │
│  Platform          Web                                  (5)  │
│  Component         Frontend                                  │
│  Total             14                                   (6)  │
│                                                              │
│      ╭───╮         Passed     9                         (7)  │
│      │75%│         Failed     2                              │
│      ╰───╯         Blocked    1                              │
│                    Untested   2                              │
│                    Removed    0                              │
├──────────────────────────────────────────────────────────────┤
│  Escape Close                                           (8)  │
└──────────────────────────────────────────────────────────────┘
```

1. **Name and Path** — **Path** is the node's full path on disk.
2. **Who and when** — who created the node, who last changed it, and the dates.
3. **Status** — left out on **Test Cases** and **Test Runs**, which have none.
4. **The execution rows** — **Execution Started**, **Execution Ended** and
   **Execution Time**, which is how long the run took. Only a test run has
   them, and a test run that never started shows none of them. A run that
   started and has not ended shows the first alone: there is no length yet.
5. **The settings** — every answer the tester gave when the test run was made.
   Only a test run has them.
6. **The counts** — what the node holds. Which counts appear depends on the
   kind of node. The table under **Main flow** says which.
7. **The verdict chart** — a ring with the pass rate inside it, and the five
   verdicts beside it. Each verdict has a color and a count. Only a test run has
   this chart.
8. **The status bar** — `Escape` closes the dialog.

A row with nothing in it is not drawn. So a test set shows **Name**, **Path**,
the four rows about who and when, **Status** and **Test cases**, and nothing
else.

## Main flow

1. The tester selects one node. With several selected, **Details** is still
   offered, and shows the first of them.
2. The tester chooses **Details**.
3. The **Details** dialog opens, titled **Details**, with `Escape Close` on its
   status bar.
4. It shows **Name** and **Path**. **Path** is the node's full path on disk.
5. It shows **Created By**, **Created At**, **Updated By** and **Updated At**.
6. It shows **Status**, except on **Test Cases** and **Test Runs**, which have
   none.
7. It shows what the node holds. The counts are worked out when the tester
   asks, and never saved. Which counts appear depends on the kind of node:

   | Node | Counts |
   |---|---|
   | Test project | **Test sets**, **Packages**, **Test cases**, **Test runs** |
   | **Test Cases** | **Test sets**, **Packages**, **Test cases** |
   | **Test Runs** | **Packages**, **Test runs** |
   | Test set package | **Test sets**, **Packages**, **Test cases** |
   | Test run package | **Packages**, **Test runs** |
   | Test set | **Test cases** |
   | Test run | **Total** |

8. **A test run, and only a test run**, also shows a verdict chart. It is a
   ring, with **Passed**, **Failed**, **Blocked**, **Untested** and **Removed**
   listed beside it. Each one has a color and a count. Inside the ring is the
   pass rate. It reads **Not run** when nothing in the test run has been
   executed.
9. A test run also shows **Execution Started**, **Execution Ended** and
   **Execution Time**. It shows every setting the tester gave when the test run
   was made: **Test Type**, **Change Log**, **Commit ID**, **Platform**,
   **Component**, **Language**, **Browser** and **Device Type**.
10. A row with nothing in it is not drawn at all. A test run that never started
    shows none of the three execution rows, and one still running shows only
    the first.
11. The tester presses `Escape`. It closes. Nothing was changed, and nothing is
    announced.

## What Testin refuses

**Nothing.** **Details** is never gray, and it opens on every kind of node. With
several rows selected, it opens on the first of them and says nothing about the
rest.

---

[Documentation](../README.md) › [The tree panel](main.md)
