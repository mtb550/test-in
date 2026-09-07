[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-003

# UC-CODEGEN-003: Get a method for a test case that had none

**As a** tester, **I want** the method to appear once I have given a test case
its description, **so that** a test case I sketched without a name is not left
without automation forever.

There is no button for this. It happens when the description is filled in.

## Rules

- **Rule-CODEGEN-019** — Filling in a description on a test case that has no
  method writes the method, rather than reporting that there is nothing to
  change.
- **Rule-CODEGEN-020** — Changing a description on a test case that already has
  a method rewrites the method instead. That is
  [UC-CODEGEN-010](renameTestCase.md).
- **Rule-CODEGEN-021** — A description cleared back to nothing leaves the method
  under the name it already has.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## Main flow

1. A test case exists with no description, so it has no method.
2. The tester selects it and presses `D`, or opens **Description** from the
   `F2` menu.
3. The tester types a description and presses `Enter`.
4. Testin looks for a method carrying this test case's identity.
5. There is none, so instead of stopping, Testin writes it.
6. The method appears with the description, the identity, the groups and the
   position.

## What Testin refuses

**If the description cannot name a Java method** — the dialog refuses it, and
the field turns red. The message names what the method would have been called.

**If the test case's place in the tree is too shallow to name a class** —
nothing happens at all, and nothing is written anywhere, not even the log.

**If the IDE has no Java plugin** — nothing is generated.

## Why this is worth knowing

There is no button anywhere in Testin that says "write the method for this test
case". The one menu entry that sounds like it, **Automate Test Case**, is not
built. Filling in the description is the way, and no label says so. That is
difference 2 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
