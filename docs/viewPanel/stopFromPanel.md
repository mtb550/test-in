[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-013

# UC-VIEW-PANEL-013: Stop a test case from the panel

**As a** tester, **I want** to stop a test case that is running,
**so that** I can change something and start it again.

There is no key for this, and no tooltip claims one.

## Rules

- **Rule-VIEW-PANEL-052** — While a test case is running, the run button is
  replaced by the stop button.
- **Rule-VIEW-PANEL-053** — Stopping one test case stops every test case running
  with it. They share one process.
- **Rule-VIEW-PANEL-054** — A test case the tester stopped is recorded as not
  run, never as failed.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. A test case is running, so the panel shows the stop button.
2. The tester clicks it.
3. Testin ends the process the test case is running in.
4. A message reads *Stopped*, with a count when more than one stopped.

The stop button is in the same place as the run button, drawn on
[UC-VIEW-PANEL-012](runFromPanel.md).

## What Testin refuses

**If the test case is not running** — the place shows the run button, and this
gesture does not exist.

**If nothing was actually stopped** — no message is raised.

**If the IDE has no TestNG plugin** — the button is not drawn.

## What the tester should expect

The panel shows one test case, and stopping it can report *Stopped 12*. A test
run started as one gesture runs as one process, so stopping any test case in it
stops the rest. The count is what really went back, not what the tester aimed
at.

---

[Documentation](../README.md) › [The view panel](main.md)
