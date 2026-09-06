[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-002

# UC-CODEGEN-002: Get a method when I create a test case

**As a** tester, **I want** a test method to appear when I write a test case,
**so that** I can fill in what it does and run it, without writing the
declaration myself.

There is no key for this. It happens when a test case is created, which is
[UC-EDITOR-PANEL-006](../editorPanel/createTestCase.md).

## Rules

- **Rule 12** — The method name is the description with everything but letters
  and digits removed, the first word lowercase and later words capitalized.
- **Rule 13** — `testName` carries the test case's identity. That is what finds
  the method afterwards.
- **Rule 14** — `priority` carries the test case's position in its test set,
  counting from one.
- **Rule 15** — `groups` is written only when the test case belongs to at least
  one group.
- **Rule 16** — Two descriptions that differ only in punctuation or capitals are
  one method.
- **Rule 17** — A test case created with no description gets no method.
- **Rule 18** — A whole sheet of test cases is written as one change, so it is
  one undo and one write.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## What is written

The method is described on
[the automation code page](main.md#what-testin-writes). The body holds one
comment and nothing else.

## Main flow

1. The tester creates a test case with a description.
2. Testin groups the new test cases by the class they belong to.
3. Testin finds the class, or writes it first.
4. Testin adds the TestNG import if the class has not got it.
5. Testin writes every method in one change, just before the closing brace.
6. Testin formats what it wrote, and nothing else.

## What Testin refuses

**If the description is empty** — no method is written. Nothing is said, and
only the log records it. The method appears later, when the description is
filled in. That is [UC-CODEGEN-003](getMissingMethod.md).

**If the description cannot name a Java method** — the create dialog refuses it
before anything is written. The field turns red, and a message titled **That
description cannot name a test method** explains what the method would have been
called.

**If the class already holds a method with that name** — no method is written
for the second test case. Nothing on screen says so.

**If the code project has no Java test source folder** — a message titled **Java
Test Source Not Found** appears, and the test case is still created.

**If the IDE has no Java plugin** — nothing is generated, and a message appears
once for the whole code project.

## Where the plugin breaks its own rules

**Two test cases can share one method.** Descriptions that differ only in
punctuation come to one method name. The second test case ends with no method
carrying its identity, so it cannot be run and cannot be jumped to. Nothing says
so when it is created, and the tester finds out at the first `F5`. That is
difference 3 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

**`priority` is not the priority.** It is the position in the test set. That is
difference 1.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
