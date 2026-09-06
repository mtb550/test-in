[Documentation](../README.md) › [The project panel](main.md) › UC-016

# UC-016: Run the automation for everything a node holds

> **No key.** On the menu: **Run Tests**.

**As a** tester, **I want** to run every automated test case under a test set, a
package or **Test Cases**, **so that** a whole area runs in one gesture.

## Rules

- **Rule 63** — Running from a parent skips retired branches. Running a retired
  test set directly still runs it. (rule 8)
- **Rule 64** — A node with no test cases to run says so. It runs nothing.
- **Rule 65** — Running needs the automation plugin. Without it, the item is
  not offered.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The automation plugin is installed.
2. The tester selects **Test Cases**, a test set package or a test set.
3. The tester chooses **Run Tests**.
4. Every test case under the node runs, skipping retired branches.
5. Testin shows *Running*, or *Running N*.
6. Running a retired test set directly still runs its test cases.

## What Testin refuses

**If the node holds no test case that can run** — nothing runs, and *\<name\>
has no test cases to run* is shown in red.

**If the automation plugin is not installed** — **Run Tests** is not in the
menu.

---

[Documentation](../README.md) › [The project panel](main.md)
