[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-012

# UC-CODEGEN-012: Change a test case's groups

**As a** tester, **I want** the groups on the test method to follow the groups
on the test case, **so that** a run of the smoke group runs what the tree calls
smoke.

There is no key for this. It happens when the groups are changed, which is
[UC-EDITOR-PANEL-007](../editorPanel/changeOneField.md).

## Rules

- **Rule-CODEGEN-045** — The groups are written into the annotation as a list,
  using the names in capitals.
- **Rule-CODEGEN-046** — A test case belonging to no group has no `groups` in
  its method at all.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester adds the smoke group to a test case.
2. Testin finds the method by the test case's identity.
3. Testin rewrites the list of groups in the annotation.

## What Testin refuses

**If the test case has no method** — a message reads the test case's
description, then *has no generated code yet*. Nothing is written.

**If the method has no annotation** — nothing is written, and only the log says
so.

**If the IDE has no Java plugin** — nothing is written.

## What the tester sees elsewhere

The groups are written in capitals in the code, as `REGRESSION` and `SMOKE`.
Everywhere in Testin they read as words, as **Regression** and **Smoke**. The
code follows the way TestNG expects them.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
