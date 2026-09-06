[Documentation](../README.md) › [The project panel](main.md) › UC-015

# UC-015: Edit a test run

> **No key.** On the menu: **Edit Run**. **`Ctrl+Z`** in the tree takes the
> edit back.

**As a** tester, **I want** to change which test cases a test run covers, its
name and its configuration, **so that** a test run can be corrected without being
recreated.

## Rules

- **Rule 60** — A signed-off test run cannot be edited. (rule 9)
- **Rule 61** — Removing a test case from a test run drops everything that test
  case recorded in that test run. Adding a test case adds it as **Pending**.
- **Rule 62** — An edit can be undone, as one step. (rule 49)

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The dialog is drawn under [UC-006](createTestRun.md).

## Main flow

1. The tester selects a test run that is not signed off.
2. The tester chooses **Edit Run**.
3. The **Edit Test Run** dialog opens. It shows the test run's name, its test
   cases already ticked, and its configuration already filled in. Test cases
   added to a test set since the test run was created appear unticked.
4. The tester changes any of it and presses **Save**.
5. If the name was changed, Testin closes the test run's editor, renames its
   folder, and only then saves the test run.
6. Testin removes any test case the tester unticked, with everything the test run
   recorded about it. A newly ticked test case is added as **Pending**.
7. The tree refreshes, and Testin shows *Updated*.
8. `Ctrl+Z` in the tree puts the previous name, test cases and configuration all
   back, and Testin shows *Undone*.

## What Testin refuses

**If the test run is Completed or Closed** — **Edit Run** is gray.

**If the test run was signed off from its editor while the dialog was open** —
the dialog stays open, and *'\<run\>' was Completed while this was open -
nothing saved* is shown in red.

**If the test run was removed while the dialog was open** — *'\<run\>' no longer
exists - nothing saved* is shown in red.

---

[Documentation](../README.md) › [The project panel](main.md)
