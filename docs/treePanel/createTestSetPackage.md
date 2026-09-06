[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-008

# UC-TREE-PANEL-008: Create a test set package

> **`Ctrl+M`**, with **Test Cases** or another test set package selected, then
> pick *Test Set Package*. On the menu: **Create**.

**As a** tester, **I want** a folder to group test sets, **so that** a tree with
many test sets still reads the way the product is organized.

## Rules

- **Rule 80** — A test set package is created empty. Nothing opens, and no
  automation code is written for it.
- **Rule 81** — A test set package can hold another test set package, as deep as
  the tester needs.

Rules 23 and 24 hold here too. They say what can be created where, and they are
on [UC-TREE-PANEL-007](createTestSet.md).

Rules 1 to 13 hold everywhere in the panel. They are on
[the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

**If a node with that name already exists under the parent** — the dialog closes,
nothing is created, and *\<name\> Already Exists* is shown in red.

**If the test project, a test set or a test run is selected** — **Create** is
gray, and `Ctrl+M` does nothing.

---

[Documentation](../README.md) › [The tree panel](main.md)
