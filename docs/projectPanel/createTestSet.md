[Documentation](../README.md) › [The project panel](main.md) › UC-007

# UC-007: Create a test set

> **`Ctrl+M`**, with **Test Cases** or a test set package selected, then pick
> *Test Set*. On the menu: **Create**.

**As a** tester, **I want** to add a test set under **Test Cases** or under a
package, **so that** the test cases I am about to write have somewhere to live.

## Rules

- **Rule 23** — Under **Test Cases** or a test set package, only a test set or a
  test set package can be created. The dialog offers nothing else.
- **Rule 24** — Nothing can be created directly under the test project, under a
  test set, or under a test run.
- **Rule 25** — A new test set opens in its editor at once. Its automation code
  is written where the Java plugin allows it.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
2. **The two kinds** — each with a hint beside it. `↑` `↓` move between them,
   and the icon at the front of the name box changes to match. Clicking a row
   confirms straight away.
3. **The status bar** — every key this dialog answers to.

**Create Run Node** is the twin dialog on the test run side. It is identical
except for two things. Its kinds are *test run* (*Records execution results*)
and *test run package* (*Groups test runs*). Its hint text reads *set name,
like Sprint 3 Cycle 1...*.

> **Today the two kind names are blank.** The rows show only their hints.
> This is difference 4 on [the project panel page](main.md#where-the-plugin-breaks-its-own-rules).

## Main flow

1. The tester selects **Test Cases** or a test set package.
2. The tester presses `Ctrl+M`, or chooses **Create**.
3. The **Create Test Node** dialog opens. *Test Set* is selected, and beside it
   the dialog says *Holds test cases*.
4. The tester types a name and presses `Enter`.
5. Testin creates the test set under the selected parent, refreshes the tree,
   and shows *Created*.
6. The test set opens in its editor at once.
7. Where the Java plugin is installed, Testin also writes its automation code.

To group test sets instead of adding one, see
[UC-008](createTestSetPackage.md).

## What Testin refuses

**If the name is empty** — the dialog stays open, the gray hint text turns red,
and the cursor stays in the box.

**If a node with that name already exists under the parent** — the dialog closes,
nothing is created, and *\<name\> Already Exists* is shown in red.

**If the test project, a test set or a test run is selected** — **Create** is
gray, and `Ctrl+M` does nothing.

**If the IDE project has no Java test folder** — *Java Test Source Not Found*
says no automation code will be written. The test set is still created.

---

[Documentation](../README.md) › [The project panel](main.md)
