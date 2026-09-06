[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-018

# UC-CODEGEN-018: Remove a test set or a package

**As a** tester, **I want** the class or the folder to go with the node I
removed, **so that** the code does not keep tests for a test set nobody has any
more.

There is no key for this. It happens when the node is removed, which is
[UC-TREE-PANEL-012](../treePanel/removeNode.md).

## Rules

- **Rule 59** — Removing a test set deletes its class file. Removing a package
  deletes its folder and everything under it.
- **Rule 60** — A removal that finds no code to delete says nothing at all.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester selects a test set and presses `Delete`.
2. The confirmation says what the test set holds.
3. The tester confirms.
4. Testin deletes the class file.
5. Testin removes the test set.

## What Testin refuses

**If the code project has no Java test source folder** — nothing is deleted, and
nothing is said. A removal that finds nothing to tidy up is not a failure.

**If the folder cannot be deleted** — nothing is said, and only the log records
it.

**If the IDE has no Java plugin** — nothing is deleted.

## What the tester should expect

Everything the tester wrote inside those methods goes with the class. The
confirmation before the removal counts test sets, test cases and test runs. It
does not mention the code.

Undoing the removal puts the test data back and writes the classes and methods
again, empty. What was in the method bodies does not come back.

## Where the plugin breaks its own rules

**A removal can report that a name could not be built.** Removing a test set
that sits outside a test cases folder shows a message titled **Class Name
Unknown**, saying no automation class name could be built. The tester was
deleting, not creating. That is difference 8 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
