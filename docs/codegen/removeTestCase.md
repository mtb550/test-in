[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-014

# UC-CODEGEN-014: Remove a test case

**As a** tester, **I want** the generated method to go when I remove a test
case, **so that** the code does not fill up with tests for things that no longer
exist.

Remove a test case, and its method is deleted from the class.

There is no key for this. It happens when a test case is removed, which is
[UC-EDITOR-PANEL-012](../editorPanel/removeTestCases.md).

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — Testin writes only the method's annotation and its
  declaration. The body is the tester's, and Testin never touches it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-049** — Removing a test case deletes its method from the class.
  The class itself stays.
- **Rule-CODEGEN-050** — A test case with no method is skipped without a word.

## What the tester sees

The card goes, and a message reads *Removed*, with a count when more than one
went. Nothing on screen mentions the code. The method is simply gone from the
class file, and the class stays with one fewer method in it.

## Main flow

1. The tester selects a test case and presses `Delete`.
2. The tester confirms.
3. Testin removes the test case.
4. Testin finds the method by that test case's identity and deletes it.
5. The class stays, with one fewer method.

## What Testin refuses

**If the test case has no method** — nothing is deleted and nothing is said.

**If the class cannot be found** — nothing is deleted, and only the log says so.

**If the IDE has no Java plugin** — nothing is deleted.

## What the tester should expect

Anything the tester wrote inside the method goes with it. Testin owns the
declaration, but deleting a method deletes the body too. Nothing warns about
that. The confirmation before the removal names the test case, not the code.

Undoing the removal writes the method again, empty. What was in its body does
not come back.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
