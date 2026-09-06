[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-027

# UC-TREE-PANEL-027: See what a node holds

> **No key.** On the menu: **Details**.

**As a** tester, **I want** to see a node's counts, dates, status and verdict
breakdown without opening anything, **so that** I can see how big a part of the
tree is at a glance.

## Rules

- **Rule 74** — Opening **Details** changes nothing, so Testin says nothing.
  (rule 7)
- **Rule 75** — Testin counts what a node holds when the tester asks. It never
  saves the number.

Rules 1 to 13 hold everywhere in the panel. They are on
[the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
9. A test run also shows **Execution Started** and **Execution Ended**, and every
   setting the tester gave when it was made: **Test Type**, **Change Log**,
   **Commit ID**, **Platform**, **Component**, **Language**, **Browser** and
   **Device Type**.
10. A row with nothing in it is not drawn at all. A test run that never started
    shows neither execution row.
11. The tester presses `Escape`. It closes. Nothing was changed, and nothing is
    announced.

## What Testin refuses

**Nothing.** **Details** is never gray, and it opens on every kind of node. With
several rows selected it opens on the first of them without saying so.

---

[Documentation](../README.md) › [The tree panel](main.md)
