[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-011

# UC-CODEGEN-011: Reorder the test cases in a test set

**As a** tester, **I want** the automation to run in the order I put the test
cases in, **so that** a test case that signs in comes before the one that signs
out.

There is no key for this. It happens when test cases are reordered, which is
[UC-EDITOR-PANEL-011](../editorPanel/dragToReorder.md).

## Rules

- **Rule 42** — Reordering rewrites the position on **every** test case in the
  test set, not only the one that moved.
- **Rule 43** — The position counts from one, and is written into the
  annotation's `priority`.
- **Rule 44** — A test case with no method is skipped without a word, because
  the sweep touches every test case in the set.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

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

## Where the plugin breaks its own rules

**The attribute is called `priority` and does not hold the priority.** A tester
reading the generated code sees `priority = 3` and reads it as the test case's
priority, which is a separate thing with the values High, Medium and Low.
Changing the real priority writes nothing into the code at all. That is
difference 1 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
