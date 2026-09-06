[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-014

# UC-CODEGEN-014: Remove a test case

**As a** tester, **I want** the generated method to go when I remove a test
case, **so that** the code does not fill up with tests for things that no longer
exist.

There is no key for this. It happens when a test case is removed, which is
[UC-EDITOR-PANEL-012](../editorPanel/removeTestCases.md).

## Rules

- **Rule 49** — Removing a test case deletes its method from the class. The
  class itself stays.
- **Rule 50** — A test case with no method is skipped without a word.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

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
declaration, but deleting a method deletes the body too, and there is no
separate warning about that. The confirmation before the removal names the test
case, not the code.

Undoing the removal writes the method again, empty. What was in its body does
not come back.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
