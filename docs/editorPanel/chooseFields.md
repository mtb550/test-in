[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-003

# UC-EDITOR-PANEL-003: Choose which fields are shown

**As a** tester, **I want** to show only the fields I am working with,
**so that** a card is short enough to scan and the grid is narrow enough to
read.

There is no key for this. The button's tooltip reads **Details**.

## Rules

- **Rule-EDITOR-PANEL-021** — Ticking a field shows it at once, in whichever
  view is on screen.
- **Rule-EDITOR-PANEL-022** — The choice is remembered, and is separate for a
  test set and a test run.
- **Rule-EDITOR-PANEL-023** — Two fields cannot be changed. **Description** is
  always shown and **ID** is never shown.
- **Rule-EDITOR-PANEL-024** — A burst of ticks costs one redraw, not one for
  each.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The 18 fields

| Field | Shown to start with | Can be changed |
|---|---|---|
| Order | Yes | Yes |
| Description | Yes | **No** |
| ID | No | **No** |
| Expected Result | Yes | Yes |
| Steps | No | Yes |
| Priority | Yes | Yes |
| FQCN | No | Yes |
| Reference | No | Yes |
| Test Data | No | Yes |
| Pre Conditions | No | Yes |
| Group | Yes | Yes |
| Path | No | Yes |
| Module | No | Yes |
| Status | No | Yes |
| Created By | No | Yes |
| Updated By | No | Yes |
| Created At | No | Yes |
| Updated At | No | Yes |

## Main flow

1. The tester presses the fields button on the toolbar.
2. A list of every field opens under it, each with a tick box.
3. The tester ticks **Steps**.
4. The cards are measured again and redrawn with a steps line.
5. The choice is remembered for the next time a test set is opened.

## What Testin refuses

**If the tester tries to untick Description** — the row is gray and does not
answer. No message is shown.

**If the tester tries to tick ID** — the same.

**If a remembered choice cannot be read** — it is dropped, and only the log says
so.

## Where the plugin breaks its own rules

**Two toolbar buttons are both tooltipped Details.** This one, and the one at
the far right that opens the test set's own details. That is difference 5 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

**Unticking Order stops three gestures working in the grid.** Clicking a row to
select it, `Enter` to open the details panel, and the double-click all stop.
Nothing says why. That is difference 12.

---

[Documentation](../README.md) › [The editor panel](main.md)
