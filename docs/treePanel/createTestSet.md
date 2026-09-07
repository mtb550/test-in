[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-007

# UC-TREE-PANEL-007: Create a test set

> **`Ctrl+M`**, with **Test Cases** or a test set package selected, then pick
> *Test Set*. On the menu: **Create**.

**As a** tester, **I want** to add a test set under **Test Cases** or under a
package, **so that** the test cases I am about to write have somewhere to live.

A test set is the folder that holds test cases. This makes one.

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
- **Rule-TREE-PANEL-024** — Under **Test Cases** or a test set package, only a
  test set or a test set package can be created. The dialog offers nothing else.
- **Rule-TREE-PANEL-025** — Nothing can be created directly under the test
  project, under a test set, or under a test run.
- **Rule-TREE-PANEL-026** — A new test set opens in its editor at once. Its
  automation code is written where the Java plugin allows it.

## The Create Test Node dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Node                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [set]  set name...                                     (1)  │
│                                                              │
│  [set]  Test Set          Holds test cases              (2)  │
│  [pkg]  Test Set Package  Groups test sets                   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    ↑ ↓ Select    Escape Cancel           (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The name** — its gray hint text reads *set name...*. If the tester presses
   `Enter` with the box empty, the hint turns red and the cursor stays here.
2. **The two kinds** — each with a hint beside it. `↑` and `↓` move between
   them. The icon at the front of the name box changes to match. Clicking a row
   confirms straight away.
3. **The status bar** — every key this dialog answers to.

**Create Run Node** is the same dialog on the test run side. Two things differ.
Its kinds are *test run* (*Records execution results*) and *test run package*
(*Groups test runs*). Its hint text reads *set name, like Sprint 3 Cycle 1...*.

> **Today the two kind names are blank.** The rows show only their hints. This
> is difference 4 on
> [the tree panel page](main.md#where-the-plugin-breaks-its-own-rules).

## Main flow

1. The tester selects **Test Cases** or a test set package.
2. The tester presses `Ctrl+M`, or chooses **Create**.
3. The **Create Test Node** dialog opens. Its first row is selected, and reads
   *Holds test cases*.
4. The tester types a name and presses `Enter`.
5. Testin creates the test set under the selected parent, refreshes the tree,
   and shows *Created*.
6. The test set opens in its editor at once.
7. Where the Java plugin is installed, Testin also writes its automation code.

To group test sets instead of adding one, see
[UC-TREE-PANEL-008](createTestSetPackage.md).

## What Testin refuses

**If the name is empty** — the dialog stays open, the gray hint text turns red,
and the cursor stays in the box.

**If a node with that name already exists under the parent** — the dialog
closes, nothing is created, and *\<name\> Already Exists* is shown in red.

**If the test project, a test set or a test run is selected** — **Create** is
gray, and `Ctrl+M` does nothing.

**If the IDE project has no Java test folder** — *Java Test Source Not Found*
says no automation code will be written. The test set is still created.

**If the Java plugin is not installed** — *Java Plugin Not Available* says that
automation code and navigation need it. Testin says this once per project. The
test set is still created.

**If several rows are selected** — **Create** stays black and creates under the
first of them.

**If the selected package is retired** — **Create** is not gray. The test set is
created inside an **Archived** package. No test run will ever be offered it
there.

---

[Documentation](../README.md) › [The tree panel](main.md)
