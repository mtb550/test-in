[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-011

# UC-CODEGEN-011: Reorder the test cases in a test set

**As a** tester, **I want** the automation to run in the order I put the test
cases in, **so that** a test case that signs in comes before the one that signs
out.

There is no key for this. It happens when test cases are reordered, which is
[UC-EDITOR-PANEL-011](../editorPanel/dragToReorder.md).

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
- **Rule-CODEGEN-042** — Reordering rewrites the position on **every** test case
  in the test set, not only the one that moved.
- **Rule-CODEGEN-043** — The position counts from one, and is written into the
  annotation's `priority`.
- **Rule-CODEGEN-044** — A test case with no method is skipped without a word,
  because the sweep touches every test case in the set.

## Main flow

1. The tester drags a test case to the top of its test set.
2. Testin rewrites the order of the test cases themselves.
3. Testin then walks every test case in that test set.
4. For each one that has a method, Testin writes its new position into
   `priority`.
5. A test run of that whole test set now executes in the tester's order.

## What Testin refuses

**If a test case has no method** — it is skipped, and nothing is said. This is
deliberate. The sweep touches every test case in the set, and reporting each one
without a method would be a message for every test case nobody has written yet.

**If a method has no annotation** — it is skipped, and only the log says so.

**If the IDE has no Java plugin** — nothing is rewritten.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
