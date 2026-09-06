[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-010

# UC-TREE-PANEL-010: Create a test run package

> **`Ctrl+M`**, with **Test Runs** or another test run package selected, then
> pick *test run package*. On the menu: **Create**.

**As a** tester, **I want** a folder to group test runs, **so that** a year of
cycles does not sit in one flat list.

## Rules

- **Rule 26** — Under **Test Runs** or a test run package, only a test run or a
  test run package can be created.
- **Rule 82** — A test run package is created empty, and nothing opens.
- **Rule 83** — A test run package can hold another test run package, as deep as
  the tester needs.

Rules 1 to 13 hold everywhere in the panel. They are on
[the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The dialog is drawn under [UC-TREE-PANEL-007](createTestSet.md). The test run side of it
carries the same two rows, reading *test run* and *test run package*, and its
gray hint text reads *set name, like Sprint 3 Cycle 1...*.

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

**If a node with that name already exists under the parent** — the dialog closes,
nothing is created, and *\<name\> Already Exists* is shown in red.

**If the test project, a test set or a test run is selected** — **Create** is
gray, and `Ctrl+M` does nothing.

---

[Documentation](../README.md) › [The tree panel](main.md)
