[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-022

# UC-TREE-PANEL-022: Edit a test run

> **No key.** On the menu: **Edit Run**. **`Ctrl+Z`** in the tree takes the
> edit back.

**As a** tester, **I want** to change which test cases a test run covers, its
name and its configuration, **so that** a test run can be corrected without
being recreated.

## Rules

- **Rule-TREE-PANEL-060** — A signed-off test run cannot be edited.
  (Rule-TREE-PANEL-009)
- **Rule-TREE-PANEL-061** — Removing a test case from a test run drops
  everything that test case recorded in that test run. Adding a test case adds
  it as **Pending**.
- **Rule-TREE-PANEL-062** — An edit can be undone, as one step.
  (Rule-TREE-PANEL-049)
- **Rule-TREE-PANEL-089** — A test case that was deleted from its test set after
  the test run was made is not in this dialog. What the test run recorded about
  it is kept, because a row that cannot be shown cannot have been unticked.

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The dialog is drawn under [UC-TREE-PANEL-009](createTestRun.md).

## Main flow

1. The tester selects a test run that is not signed off.
2. The tester chooses **Edit Run**.
3. The **Edit Test Run** dialog opens. It shows the test run's name, its test
   cases already ticked, and its configuration already filled in. Test cases
   added to a test set since the test run was created appear unticked.
4. The tester changes any of it and presses **Save**.
5. If the name was changed, Testin closes the test run's editor, renames its
   folder, and only then saves the test run.
6. Testin removes any test case the tester unticked, with everything the test
   run recorded about it. A newly ticked test case is added as **Pending**.
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

**If the name is emptied** — *A test run needs a name* is shown in red, and the
dialog stays open.

**If the new name is already used by a sibling** — *\<name\> Already Exists* is
shown in red, and the dialog stays open. Keeping the test run's own name is not
a clash.

**If the last test case is unticked** — **Save** goes dead, the same way
**Create** does on an empty new test run.

**If several rows are selected** — **Edit Run** is gray. It needs exactly one.

> **Saving quietly drops the rows of deleted test cases.** A test run keeps a row
> at **Removed** for a test case deleted from its test set. That row is not in
> this dialog, cannot be ticked, and pressing **Save** deletes it with its
> verdict, its duration and its failure detail. It happens even when the tester
> changed nothing else.

---

[Documentation](../README.md) › [The tree panel](main.md)
