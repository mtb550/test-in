[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-012

# UC-EDITOR-PANEL-012: Undo a change

**As a** tester, **I want** to take back what I just did,
**so that** a wrong bulk edit across 30 test cases costs one keystroke.

`Ctrl+Z`.

## Rules

- **Rule 63** — Each editor has a history of its own, kept against the test set
  it is showing. The tree keeps another.
- **Rule 64** — The menu entry says what the next press would take back.
- **Rule 65** — A test case is put back exactly, including who last changed it
  and when.
- **Rule 66** — A test case coming back from a removal gets its test method
  written again.
- **Rule 67** — A gesture that changed nothing is not on the history at all.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester changes the module on 30 test cases.
2. The tester presses `Ctrl+Z`.
3. All 30 are written back exactly as they were.
4. Every editor open on that test set reloads, keeping its filters and its
   search.
5. A message reads *Undone*.

## What Testin refuses

**If there is nothing to take back** — the menu entry is gray and the key does
nothing.

**If something else has written those test cases since** — a message titled
**These test cases changed since** reads *Something else has written them - a
sync, a pull, or another IDE - so taking this back would write over work that is
not yours. Nothing was changed.*

## What one press takes back

| The tester did | One press takes back |
|---|---|
| Changed one field on 30 test cases | All 30 |
| Removed four test cases | All four |
| Dragged three cards | All three |
| Typed in one grid cell | That one cell |
| Cut in one test set and pasted into another | Both halves |

## What the history does not hold

The history belongs to this editor and this test set. A change made in the tree
is taken back in the tree. A change made in another editor is taken back there.
Pressing `Ctrl+Z` in the wrong place takes back the wrong thing, or nothing.

---

[Documentation](../README.md) › [The editor panel](main.md)
