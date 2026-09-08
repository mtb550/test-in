[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-009

# UC-CODEGEN-009: Stop a running test case

**As a** tester, **I want** to end a run that is going,
**so that** I can change something and start it again.

Stopping ends the whole run. Every test case in it goes back to not run.

There is no key of its own. `F5` on a running test case stops it.

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
- **Rule-CODEGEN-036** — While a test case is running, the run button and the
  menu entry both become the stop.
- **Rule-CODEGEN-037** — Stopping one test case stops every test case running
  with it. One run is one process.
- **Rule-CODEGEN-038** — A test case the tester stopped is recorded as not run,
  never as failed.

## What the tester sees

The stop button on the card turns back into a run button, and every card that
was running goes back to how it looked before. One message appears near the
bottom right of the IDE, reading *Stopped* and then the count. It fades after
about five seconds.

## Main flow

1. Three test cases are running.
2. The tester presses `F5` on one of them, or clicks its stop button.
3. Testin ends the process.
4. All three go back to not run.
5. A message reads *Stopped 3*.

## What Testin refuses

**If nothing was actually stopped** — no message is raised.

**If nothing is selected** — the menu entry is gray.

## What the tester should expect

The count in the message is what really went back. It can be more than the
tester aimed at. Stopping one test case in a run of twelve reports *Stopped 12*,
because the twelve share one process.

A result arriving after the tester stopped a test case is ignored, so a stop is
never read as a failure.

## The tooltip has no key

The run button's tooltip names `F5`. The stop button's names no key, because
stopping has none of its own. The same key does both, decided by whether the
test case is running.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
