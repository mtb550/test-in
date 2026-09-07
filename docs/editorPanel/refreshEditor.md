[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-027

# UC-EDITOR-PANEL-027: Refresh the editor from disk

**As a** tester, **I want** to read the test set again from disk,
**so that** I see what a colleague's sync brought in.

There is no key for this. The button's tooltip reads **Refresh**.

## Rules

- **Rule-EDITOR-PANEL-114** — Refresh clears every filter and the search text.
- **Rule-EDITOR-PANEL-115** — Refresh remembers which test case was selected,
  and lands on the page holding it.
- **Rule-EDITOR-PANEL-116** — The tester's own refresh always reloads, even with
  a grid cell open. A refresh Testin starts on its own leaves a busy editor
  alone.
- **Rule-EDITOR-PANEL-117** — In a test run editor, refresh also stops the
  execution.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester presses the refresh button.
2. Every filter and the search text are cleared.
3. The selected test case is remembered.
4. The list empties and reads *Refreshing...*.
5. Testin reads the test set from disk again.
6. The page holding the remembered test case is drawn.
7. A message reads *Refreshed*.

## What Testin refuses

Nothing.

## Where the plugin breaks its own rules

**The filters and the search go without a word.** A tester who has narrowed a
test set of 200 down to four, and presses refresh to pick up a colleague's
change, gets all 200 back. The message says *Refreshed*. That is difference 13
on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

**In a test run editor, refresh stops the execution.** The clock stops, the walk
ends, and the toolbar button turns back into **Start Manual Execution**. The
message still says only *Refreshed*. That is difference 24.

## Refreshing on its own

Testin also reads a test project again when something changes it on disk, with
no button pressed. That is
[UC-INTERNAL-003](../internal/noticeOutsideChange.md), and it does not clear the
filters.

---

[Documentation](../README.md) › [The editor panel](main.md)
