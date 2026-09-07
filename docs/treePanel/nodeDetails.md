[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-027

# UC-TREE-PANEL-027: See what a node holds

> **No key.** On the menu: **Details**.

**As a** tester, **I want** to see a node's counts, dates, status and verdict
breakdown without opening anything, **so that** I can see how big a part of the
tree is at a glance.

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
- **Rule-TREE-PANEL-087** — Opening **Details** changes nothing, so Testin says
  nothing. (Rule-TREE-PANEL-007)
- **Rule-TREE-PANEL-088** — Testin counts what a node holds when the tester
  asks. It never saves the number.

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
7. It shows what the node holds, counted when asked and never saved. Which
   counts appear depends on the kind:

   | Node | Counts |
   |---|---|
   | Test project | **Test sets**, **Packages**, **Test cases**, **Test runs** |
   | **Test Cases** | **Test sets**, **Packages**, **Test cases** |
   | **Test Runs** | **Packages**, **Test runs** |
   | Test set package | **Test sets**, **Packages**, **Test cases** |
   | Test run package | **Packages**, **Test runs** |
   | Test set | **Test cases** |
   | Test run | **Total** |

8. **A test run, and only a test run**, also shows a verdict chart: a ring with
   **Passed**, **Failed**, **Blocked**, **Untested** and **Removed** beside it,
   each with a color and a count. Inside the ring is the pass rate, or the words
   **Not run** when nothing in the test run has been executed.
9. A test run also shows **Execution Started** and **Execution Ended**, and
   every setting the tester gave when it was made: **Test Type**, **Change
   Log**, **Commit ID**, **Platform**, **Component**, **Language**, **Browser**
   and **Device Type**.
10. A row with nothing in it is not drawn at all. A test run that never started
    shows neither execution row.
11. The tester presses `Escape`. It closes. Nothing was changed, and nothing is
    announced.

## What Testin refuses

**Nothing.** **Details** is never gray, and it opens on every kind of node. With
several rows selected it opens on the first of them without saying so.

---

[Documentation](../README.md) › [The tree panel](main.md)
