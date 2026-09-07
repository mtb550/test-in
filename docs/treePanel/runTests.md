[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-023

# UC-TREE-PANEL-023: Run the automation for everything a node holds

> **No key.** On the menu: **Run Tests**.

**As a** tester, **I want** to run every automated test case under a test set, a
package or **Test Cases**, **so that** a whole area runs in one gesture.

## Rules

- **Rule-TREE-PANEL-063** — Running from a parent skips retired branches.
  Running a retired test set directly still runs it. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-064** — A node with no test cases to run says so. It runs
  nothing.
- **Rule-TREE-PANEL-065** — Running needs the TestNG plugin. Without it, the
  item is not offered.
- **Rule-TREE-PANEL-090** — **Run Tests** is offered on **Test Cases**, a test
  set package, a test set and a test run. It is not offered on the test project
  row, on **Test Runs**, or on a test run package.

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

**From a folder of test cases**

1. The TestNG plugin is installed.
2. The tester selects **Test Cases**, a test set package or a test set.
3. The tester chooses **Run Tests**.
4. Every test case under the node runs, skipping every retired branch under it.
   Selecting a retired node itself and running it still runs everything in it.
5. Testin shows *Running* for one test case, or *Running N*, where N is how many
   actually started. Test cases already running are not counted.
6. The IDE's own Run window opens on a test configuration named after the
   generated class, or *\<class\> and N more* where the selection spans several.

**From a test run**

1. The tester selects a test run and chooses **Run Tests**.
2. Its editor opens, or comes forward if it is already open.
3. Only the test cases still **Pending** in that test run are run.

## What Testin refuses

**If the node holds no test case that can run** — nothing runs, and *\<name\>
has no test cases to run* is shown in red. A test run with nothing left
**Pending** says the same.

**If the test run is already running** — *\<name\> is already running* is shown
in red.

**If a test case has no generated automation code** — it is dropped and named:
*\<test case\> has no generated code yet*. The rest still run. If none of them
can, nothing starts.

**If the IDE is indexing** — nothing runs, and the IDE says *Cannot run tests
while IntelliJ is indexing. Please wait a moment.* If indexing starts while the
run is being prepared, it says *Indexing interrupted the test run. Please try
again.*

**If every test case under the node was already running** — nothing starts, and
Testin says nothing at all.

**If the TestNG plugin is not installed** — **Run Tests** is not in the
menu.

---

[Documentation](../README.md) › [The tree panel](main.md)
