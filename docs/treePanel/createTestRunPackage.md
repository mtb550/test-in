[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-010

# UC-TREE-PANEL-010: Create a test run package

> **`Ctrl+M`**, with **Test Runs** or another test run package selected, then
> pick *test run package*. On the menu: **Create**.

**As a** tester, **I want** a folder to group test runs, **so that** a year of
cycles does not sit in one flat list.

A test run package is a folder that holds test runs. This makes one.

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
- **Rule-TREE-PANEL-032** — Under **Test Runs** or a test run package, only a
  test run or a test run package can be created.
- **Rule-TREE-PANEL-033** — A test run package is created empty, and nothing
  opens.
- **Rule-TREE-PANEL-034** — A test run package can hold another test run
  package, as deep as the tester needs.

## What the tester sees

The **Create Run Node** dialog opens. It is the test run side of the dialog
drawn under [UC-TREE-PANEL-007](createTestSet.md). It carries the same two rows,
reading *test run* and *test run package*. Its gray hint text reads *set name,
like Sprint 3 Cycle 1...*. The tester moves to the second row, which reads
*Groups test runs*. After `Enter`, a new folder row appears in the tree.
*Created* shows above the status bar at the bottom right of the IDE. Nothing
opens.

## Main flow

1. The tester selects **Test Runs** or another test run package.
2. The tester presses `Ctrl+M`, or chooses **Create**.
3. The **Create Run Node** dialog opens. The tester moves to *test run package*
   with `↓`, and beside it the dialog says *Groups test runs*.
4. The tester types a name and presses `Enter`.
5. Testin creates the package, refreshes the tree, and shows *Created*.
6. Nothing else opens. That is the whole of it.

## What Testin refuses

**If the name is empty** — the dialog stays open, the gray hint text turns red,
and the cursor stays in the box.

**If a node with that name already exists under the parent** — the dialog
closes, nothing is created, and *\<name\> Already Exists* is shown in red.

**If the test project, a test set or a test run is selected** — **Create** is
gray, and `Ctrl+M` does nothing.

---

[Documentation](../README.md) › [The tree panel](main.md)
