[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-016

# UC-CODEGEN-016: Move a test set

**As a** tester, **I want** the Java class to move with the test set,
**so that** the package the class is in still matches where the test set sits.

There is no key for this. It happens when a test set is moved, which is
[UC-TREE-PANEL-013](../treePanel/moveNodes.md).

## Rules

- **Rule-CODEGEN-053** — The class file is moved, and its package line is
  rewritten to match where it landed.
- **Rule-CODEGEN-054** — A test set dropped where it already is, is not a move,
  and nothing is rewritten.
- **Rule-CODEGEN-055** — A move into a place Testin has not read leaves the
  class exactly where it is.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester drags a test set into another package.
2. Testin moves the class file into the matching package folder, before the tree
   changes.
3. Testin rewrites the package line at the top of the file.
4. Testin then moves the test set itself.

## What Testin refuses

**If the place it is moving to has not been read by Testin** — the class is left
where it is. The test set moves, the code does not, and only the log says so.

**If the class cannot be found** — nothing is moved, and only the log says so.

**If the code project has no Java test source folder** — nothing is moved, and
nothing is said.

**If the IDE has no Java plugin** — nothing is moved.

## Where the plugin breaks its own rules

**A move Testin declines leaves the tree and the code disagreeing.** The test
set is in one place and its class is in another, and nothing on screen says so.
That is difference 6 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
