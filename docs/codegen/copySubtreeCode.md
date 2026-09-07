[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-004

# UC-CODEGEN-004: Get the whole subtree's code when I copy

**As a** tester, **I want** every class and every method under what I copied,
**so that** a copied package is as runnable as the one it came from.

A copy brings the code with it, at every level below what was copied.

There is no key for this. It happens when a copy or a drop lands in the tree,
which is [UC-TREE-PANEL-014](../treePanel/copyNodes.md).

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — Testin writes only the parts listed above. The body is
  the tester's, and Testin never touches it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — Nearly everything that goes wrong here is written only
  to the log.
- **Rule-CODEGEN-022** — Copying a package writes the code for everything
  beneath it, at any depth, not only for the node that was dropped.
- **Rule-CODEGEN-023** — A test set's own test cases are written before its
  children, so the class exists before a method goes into it.
- **Rule-CODEGEN-024** — The whole subtree is one change, so the tester gets one
  undo for the copy they made.

## What the tester sees

The tree shows the copied nodes, and one message reads *Pasted*, with a count
when more than one node landed. Nothing on screen mentions the code. The new
classes and methods appear in the Project tool window, in a folder tree that
matches the one in the Testin tree.

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

A copied test case is a new test case with a new identity. So it gets a method
of its own. Its description gains the word `(Copy)`. That makes the method name
different too, so the two never collide.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
