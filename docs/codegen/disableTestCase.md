[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-013

# UC-CODEGEN-013: Turn a test case off

**As a** tester, **I want** a test case's method to stop running,
**so that** a test I know is broken does not fail every run until I fix it.

There is no key for this. It happens when the test case's status is set to
disabled.

## Rules

- **Rule 47** — Turning a test case off writes `enabled = false` into its
  method's annotation.
- **Rule 48** — Turning it back on takes the attribute off again, rather than
  writing `enabled = true`.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester sets a test case's status to disabled.
2. Testin finds the method by the test case's identity.
3. Testin writes `enabled = false` into the annotation.
4. TestNG skips the method from then on.
5. The tester sets the status back.
6. Testin takes the attribute off, leaving the annotation as it was.

## What Testin refuses

**If the test case has no method** — a message reads the test case's
description, then *has no generated code yet*.

**If the method has no annotation** — nothing is written, and only the log says
so.

**If the IDE has no Java plugin** — nothing is written.

## Why the attribute is removed rather than set

An annotation carrying `enabled = true` says the same thing as an annotation
with no `enabled` at all. Leaving it out keeps the generated code as short as it
can be, and means a tester reading the code only ever sees the attribute when it
matters.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
