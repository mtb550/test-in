[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-008

# UC-TREE-PANEL-008: Create a test set package

> **`Ctrl+M`**, with **Test Cases** or another test set package selected, then
> pick *Test Set Package*. On the menu: **Create**.

**As a** tester, **I want** a folder to group test sets, **so that** a tree with
many test sets still reads the way the product is organized.

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
- **Rule-TREE-PANEL-027** — A test set package is created empty. Nothing opens,
  and no automation code is written for it.
- **Rule-TREE-PANEL-028** — A test set package can hold another test set
  package, as deep as the tester needs.

Rule-TREE-PANEL-024 and Rule-TREE-PANEL-025 hold here too. They say what can be
created where, and they are on [UC-TREE-PANEL-007](createTestSet.md).

The dialog is drawn under [UC-TREE-PANEL-007](createTestSet.md).

## Main flow

1. The tester selects **Test Cases** or another test set package.
2. The tester presses `Ctrl+M`, or chooses **Create**.
3. The **Create Test Node** dialog opens. The tester moves to *Test Set Package*
   with `↓`, and beside it the dialog says *Groups test sets*.
4. The tester types a name and presses `Enter`.
5. Testin creates the package under the selected parent, refreshes the tree, and
   shows *Created*.
6. Nothing opens. The package is empty until the tester puts something in it.

## What Testin refuses

**If the name is empty** — the dialog stays open, the gray hint text turns red,
and the cursor stays in the box.

**If a node with that name already exists under the parent** — the dialog
closes, nothing is created, and *\<name\> Already Exists* is shown in red.

**If the test project, a test set or a test run is selected** — **Create** is
gray, and `Ctrl+M` does nothing.

---

[Documentation](../README.md) › [The tree panel](main.md)
