[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-004

# UC-CODEGEN-004: Get the whole subtree's code when I copy

**As a** tester, **I want** every class and every method under what I copied,
**so that** a copied package is as runnable as the one it came from.

There is no key for this. It happens when a copy or a drop lands in the tree,
which is [UC-TREE-PANEL-014](../treePanel/copyNodes.md).

## Rules

- **Rule 22** — Copying a package writes the code for everything beneath it, at
  any depth, not only for the node that was dropped.
- **Rule 23** — A test set's own test cases are written before its children, so
  the class exists before a method goes into it.
- **Rule 24** — The whole subtree is one change, so the tester gets one undo for
  the copy they made.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester copies a package holding three test sets and pastes it.
2. Testin opens one change for the whole subtree.
3. For each node, Testin asks what that kind of node generates.
4. A test set's class is written, then its test cases' methods.
5. Testin walks into every child and does the same.
6. The whole thing is one entry on the undo history.

## What Testin refuses

Every refusal of [UC-CODEGEN-001](getClassForTestSet.md) and
[UC-CODEGEN-002](getMethodForTestCase.md), once for each node it applies to.

**A node that generates nothing says nothing.** The two fixed folders, test run
packages and test runs write no code, and that is not reported.

## What the tester should expect

A copied test case is a new test case with a new identity, so it gets a method
of its own. Its description gains the word `(Copy)`, so the method name is
different too, and the two do not collide.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
