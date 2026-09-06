[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-012

# UC-VIEW-PANEL-012: Run a test case from the panel

**As a** tester, **I want** to start the test case I am reading,
**so that** I can try it again without going back to the card it came from.

There is no key that works here. The button's tooltip names `F5`, and `F5` does
nothing in the panel.

## Rules

- **Rule 49** — The run button is drawn only where the IDE has the TestNG
  plugin.
- **Rule 50** — The same place shows the run button or the stop button, never
  both.
- **Rule 51** — The button grows under the pointer, so it is clear it can be
  pressed.

Rules 1 to 9 hold everywhere in the panel. They are on
[the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

The two buttons sit between the title and the badges.

```
┌────────────────────────────────────────────────────────────────────────────┐
│   [ go to code ]  [ run ]                                                  │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The first button** — goes to the automation code. It is
   [UC-VIEW-PANEL-014](goToCode.md).
2. **The second button** — runs the test case. Its tooltip reads **Run Test
   Case** and names `F5`.

## Main flow

1. The panel is showing a test case.
2. The tester moves the pointer over the run button, and it grows.
3. The tester clicks it.
4. Testin starts the test case as a run of one.
5. A message reads *Running*.
6. When the result comes back, the whole panel redraws with the new verdict.

## What Testin refuses

**If the IDE has no TestNG plugin** — the button is not drawn at all. Neither is
the button beside it, if the Java plugin is also missing, and then the whole row
is left out.

**If the test case is already running** — the place shows the stop button
instead. Stopping is [UC-VIEW-PANEL-013](stopFromPanel.md).

**If the test case has no automation code** — a message reads the test case's
description, then *has no generated code yet*.

## Where the plugin breaks its own rules

**The tooltip names a key that does nothing here.** It reads `F5`, and `F5` is
bound to the cards in the editor, not to the panel. A tester who reads the
tooltip and presses `F5` gets nothing. That is difference 1 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The view panel](main.md)
