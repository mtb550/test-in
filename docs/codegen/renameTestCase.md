[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-010

# UC-CODEGEN-010: Change a test case's description

**As a** tester, **I want** the generated method to follow when I reword a test
case, **so that** the code says the same thing the test case says.

There is no key for this. It happens when the description is changed, which is
[UC-EDITOR-PANEL-007](../editorPanel/changeOneField.md).

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
- **Rule-CODEGEN-039** — The method is found by the test case's identity, so the
  old description is not needed.
- **Rule-CODEGEN-040** — Both the annotation's description and the method's name
  are rewritten.
- **Rule-CODEGEN-041** — A description cleared back to nothing leaves the method
  under the name it already has, and records the empty description.

## Main flow

1. The tester changes a test case's description.
2. Testin finds the method by the test case's identity.
3. Testin rewrites the description in the annotation.
4. Testin renames the method to match the new description.
5. Everything that called the method is updated by the IDE's own rename.

## What Testin refuses

**If the test case has no method yet** — the method is written instead. That is
[UC-CODEGEN-003](getMissingMethod.md).

**If the description cannot name a Java method** — the dialog refuses it before
anything is written.

**If the method has no annotation** — nothing is rewritten, and only the log
says so.

**If the test case's place in the tree is too shallow** — nothing happens at
all, and nothing is written anywhere, not even the log.

## What is not rewritten

The body of the method. Testin owns the annotation and the name. Everything
between the braces is the tester's.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
