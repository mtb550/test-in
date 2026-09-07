[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-013

# UC-EDITOR-PANEL-013: Redo a change

**As a** tester, **I want** to put back something I just took back,
**so that** an undo pressed by mistake costs nothing.

`Ctrl+Y`.

## Rules

- **Rule-EDITOR-PANEL-068** — The menu entry says what the next press would put
  back.
- **Rule-EDITOR-PANEL-069** — Making any new change clears everything waiting to
  be put back.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester presses `Ctrl+Z` and the change is taken back.
2. The tester presses `Ctrl+Y`.
3. The change is made again.
4. A message reads *Redone*.

## What Testin refuses

**If there is nothing to put back** — the menu entry is gray and the key does
nothing.

**If the tester has made any change since undoing** — there is nothing to put
back. Making a new change clears the list.

**If something else has written those test cases since** — the same message the
undo shows, and nothing is changed.

Everything else about the history is on
[UC-EDITOR-PANEL-012](undoChange.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
