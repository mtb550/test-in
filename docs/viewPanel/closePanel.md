[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-015

# UC-VIEW-PANEL-015: Close the panel

**As a** tester, **I want** the panel out of the way,
**so that** I have the whole width of the screen for the editor.

`Escape`, pressed in the editor, closes it.

## Rules

- **Rule-VIEW-PANEL-057** — `Escape` in the editor does three things in order.
  It drops a pending cut, then closes the panel, then clears the selection. One
  press does one of them.
- **Rule-VIEW-PANEL-058** — A panel the tester closed stays closed. Moving the
  selection does not open it again.
- **Rule-VIEW-PANEL-059** — Closing the editor a test case came from closes the
  panel too.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The panel is open, and the keyboard is in the editor.
2. The tester presses `Escape`.
3. The panel closes. Nothing is said.
4. The tester presses `Escape` again.
5. The selection is cleared.

The tester can also press the IDE's own hide button on the panel.

## What Testin refuses

**If the panel is already closed** — the press clears the selection instead.

**If a cut is waiting to be pasted** — the first press drops the cut. The panel
stays open. A second press closes it.

## Where the plugin breaks its own rules

**`Escape` does nothing from inside the panel.** It closes the panel only when
the keyboard is in the editor. A tester who has just used `F2`, which needs the
keyboard in the panel, cannot close it with `Escape`. That is difference 2 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

**Closing any Testin editor empties the panel.** A tester with two editors open,
reading a test case from the first, who closes the second, watches the panel go
blank. That is difference 9.

---

[Documentation](../README.md) › [The view panel](main.md)
