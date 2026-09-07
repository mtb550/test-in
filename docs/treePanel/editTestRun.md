[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-022

# UC-TREE-PANEL-022: Edit a test run

> **No key.** On the menu: **Edit Run**. **`Ctrl+Z`** in the tree takes the
> edit back.

**As a** tester, **I want** to change which test cases a test run covers, its
name and its configuration, **so that** a test run can be corrected without
being recreated.

This changes a test run that is not signed off yet.

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
- **Rule-TREE-PANEL-073** — A signed-off test run cannot be edited.
  (Rule-TREE-PANEL-009)
- **Rule-TREE-PANEL-074** — Removing a test case from a test run drops
  everything that test case recorded in that test run. Adding a test case adds
  it as **Pending**.
- **Rule-TREE-PANEL-075** — An edit can be undone, as one step.
  (Rule-TREE-PANEL-060)
- **Rule-TREE-PANEL-076** — A test case that was deleted from its test set after
  the test run was made is not in this dialog. What the test run recorded about
  it is kept, because a row that cannot be shown cannot have been unticked.

## What the tester sees

The **Edit Test Run** dialog opens. It is the dialog drawn under
[UC-TREE-PANEL-009](createTestRun.md), with two differences. Its title reads
**Edit Test Run**, and its button reads **Save**. It arrives filled in with the
test run's own name, its test cases and its configuration. After **Save**, the
tree refreshes and *Updated* shows above the status bar at the bottom right of
the IDE.

## Main flow

1. The tester selects a test run that is not signed off.
2. The tester chooses **Edit Run**.
3. The **Edit Test Run** dialog opens. It shows the test run's name, its test
   cases already ticked, and its configuration already filled in. Test cases
   added to a test set since the test run was created appear unticked.
4. The tester changes any of it and presses **Save**.
5. If the name was changed, Testin closes the test run's editor first. It then
   renames the folder, and only then saves the test run.
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

**If the last test case is unticked** — **Save** is disabled. **Create** does
the same on an empty new test run.

**If several rows are selected** — **Edit Run** is gray. It needs exactly one.

---

[Documentation](../README.md) › [The tree panel](main.md)
