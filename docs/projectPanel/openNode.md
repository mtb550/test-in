[Documentation](../README.md) › [The project panel](main.md) › UC-004

# UC-004: Open a test set or a test run

**As a** tester, **I want** to open a test set or a test run from the tree,
**so that** I can read and work its test cases.

## Rules

- **Rule 21** — Only a test set and a test run open in an editor. A package has
  nothing to open, and neither does a container. Neither of them says anything.
- **Rule 22** — Opening a node that is already open brings its tab forward. It
  does not open a second time.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects a test set or a test run.
2. The tester presses `Enter`, chooses **Open** from the menu, or double-clicks
   the row.
3. It opens in its editor tab, named after the node.
4. Nothing is announced, because opening only moves the view. (rule 7)

## What Testin refuses

**If the node's editor tab is already open** — that tab comes to the front, and
no second tab opens.

**If a package, a container or the test project is selected** — **Open** is gray,
and `Enter` does nothing.

---

[Documentation](../README.md) › [The project panel](main.md)
