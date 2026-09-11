[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-018

# UC-TREE-PANEL-018: Retire a test project, a test set or a package

> **No key.** On the menu: **Actions**, then the status. A test project has
> **Deactivate** and **Archive**. A test set has **Mark Deprecated**. A package
> has **Archive**.

**As a** tester, **I want** to mark old work retired, **so that** it stays for
its history without getting in the way of what I am testing now.

Retiring deletes nothing. It only moves the node out of the way.

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
- **Rule-TREE-PANEL-099** — A node that is retired says which kind of retired it
  is. A deprecated test set, an archived package and a test run all draw their
  status in gray beside the name - a run always, because a cycle state is what
  the tree is read for, and everything else only when it is retired, so the word
  appears where it means something.
- **Rule-TREE-PANEL-062** — Retiring deletes nothing. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-063** — A test project that is not **Active** shows nothing
  under it.
- **Rule-TREE-PANEL-064** — An **Archived** test project is not opened at all on
  the next load. The panel says so, and offers the other test projects.
- **Rule-TREE-PANEL-065** — A status is set on one node at a time. The status a
  node already has is offered gray, so the tester can see what it is now.

## What the tester sees

Retiring opens no screen of its own, and it asks no question. A retired test set
or package turns gray at once and moves to the bottom of its folder. A test
project that is not **Active** shows no children instead. The new status word
shows above the status bar at the bottom right of the IDE. It reads *Inactive*,
*Archived* or *Deprecated*.

## Main flow

1. The tester selects exactly one node.
2. The tester opens **Actions**. It shows the status entries for that node's
   kind. The status the node already has is gray.

   | Node | Retire it with | Statuses it can have |
   |---|---|---|
   | Test project | **Deactivate**, **Archive** | Active, Inactive, Archived |
   | Test set | **Mark Deprecated** | Active, Deprecated |
   | Package | **Archive** | Active, Archived |

3. The tester chooses one.
4. Testin writes the status, refreshes the tree, and shows the new status word:
   *Inactive*, *Archived* or *Deprecated*.

**What retiring does.** A **Deprecated** test set or an **Archived** package is
drawn gray, and it sorts last. **Expand All** leaves it closed. It is not
offered when a test run is created. Its test cases are skipped when a parent is
run. Nothing inside it is deleted.

**What an inactive test project does.** Its row shows no children. If it is
**Archived**, the next load skips it. The panel then shows *\<name\> is
archived, so it is not opened*. That screen is drawn under
[UC-TREE-PANEL-001](reachTheTree.md).

To bring one back, see [UC-TREE-PANEL-019](reactivateNode.md).

## What Testin refuses

**If the status could not be written** — Testin says *Unable to update status to
\<status\>* for a test project, *Unable to mark test set \<status\>* for a test
set, or *Unable to mark package \<status\>* for a package. Nothing changes.

**If more than one node is selected** — the status entries are not offered.

---

[Documentation](../README.md) › [The tree panel](main.md)
