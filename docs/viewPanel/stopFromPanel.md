[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-013

# UC-VIEW-PANEL-013: Stop a test case from the panel

**As a** tester, **I want** to stop a test case that is running,
**so that** I can change something and start it again.

While a test case runs, the run button becomes a stop button.

There is no key for this, and no tooltip claims one.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has three tabs, and all three are drawn
  every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-053** — While a test case is running, the run button is
  replaced by the stop button.
- **Rule-VIEW-PANEL-054** — Stopping one test case stops every test case running
  with it. They share one process.
- **Rule-VIEW-PANEL-055** — A test case the tester stopped is recorded as not
  run, never as failed.

## The screen

The stop button stands exactly where the run button stands.

```
┌──────────────────────────────────────────────────────────────────────────┐
│   [ go to code ]  [ stop ]                                               │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The first button** — goes to the automation code. A running test case does
   not change it.
2. **The second button** — the stop button. Its tooltip reads **Stop Test
   Case**, and it names no key.

The run button is drawn on [UC-VIEW-PANEL-012](runFromPanel.md).

## Main flow

1. A test case is running, so the panel shows the stop button.
2. The tester clicks it.
3. Testin ends the process the test case is running in.
4. A message reads *Stopped*, with a count when more than one stopped.

## What Testin refuses

**If the test case is not running** — the place shows the run button, and this
gesture does not exist.

**If nothing was actually stopped** — no message is raised.

**If the IDE has no TestNG plugin** — the button is not drawn.

## What the tester should expect

The panel shows one test case, and stopping it can report *Stopped 12*. A test
run started with one gesture runs as one process. Stopping any test case in it
stops all the rest. The count says how many really stopped, not how many the
tester aimed at.

---

[Documentation](../README.md) › [The view panel](main.md)
