[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-035

# UC-EDITOR-PANEL-035: Stop executing

**As a** tester, **I want** to stop the walk part way,
**so that** the clock stops when I go to a meeting and the test run is written
as it stands.

There is no key for this. The button's tooltip reads **Stop Execution**.

## Rules

- **Rule 145** — Stopping writes the test run to disk as it stands.
- **Rule 146** — Stopping changes no verdict already recorded.
- **Rule 147** — Stopping stamps when execution ended. That stamp is written
  again by every stop.
- **Rule 148** — Only the tester's own stop ends any automation this editor
  started.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. A walk is going, and the clock is ticking.
2. The tester presses **Stop Execution**.
3. Any automation this editor started is ended first.
4. Testin stamps when execution ended.
5. The clock stops, writing the last stretch onto the test case it was timing.
6. The button becomes **Start Manual Execution** again.
7. The test run is written to disk.
8. A message reads *Stopped*.

## What Testin refuses

Nothing. The button is never gray while it is on the toolbar.

## What the tester should expect

**Stopping one test case stops every test case running with it.** One test run
started as one gesture is one process, so ending it ends all of them.

**Stopping something that had already finished says *Stopped* anyway.** The
button has no gray state, and a stop that reached nothing still reports itself.
That is difference 27 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

## Two other things stop the walk

**Refresh** stops it, and says only *Refreshed*. That is difference 24.

**Closing the tab** stops it, writes the test run, and asks nothing.

---

[Documentation](../README.md) › [The editor panel](main.md)
