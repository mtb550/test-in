[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-009

# UC-CODEGEN-009: Stop a running test case

**As a** tester, **I want** to end a run that is going,
**so that** I can change something and start it again.

There is no key of its own. `F5` on a running test case stops it.

## Rules

- **Rule-CODEGEN-036** — While a test case is running, the run button and the
  menu entry both become the stop.
- **Rule-CODEGEN-037** — Stopping one test case stops every test case running
  with it. One run is one process.
- **Rule-CODEGEN-038** — A test case the tester stopped is recorded as not run,
  never as failed.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

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

The count in the message is what really went back, which can be more than the
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
