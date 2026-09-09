[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-003

# UC-EDITOR-PANEL-003: Choose which fields are shown

**As a** tester, **I want** to show only the fields I am working with,
**so that** a card stays short and the grid stays narrow.

A test case carries 18 fields. This is where the tester picks which ones the
rows show.

There is no key for this. The button's tooltip reads **Details**.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, and
  the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-021** — Ticking a field shows it at once, in whichever
  view is on screen.
- **Rule-EDITOR-PANEL-022** — The choice is remembered, and is separate for a
  test set and a test run.
- **Rule-EDITOR-PANEL-023** — Three fields cannot be changed. **Order** and
  **Description** are always shown, and **ID** is never shown. Order is the
  grid's row header and not a field a tester chooses: clicking it selects the
  row, and `Enter` and the double-click on it open the details panel.
- **Rule-EDITOR-PANEL-024** — A burst of ticks costs one redraw, not one for
  each.

## The screen

The list opens under the button. It has no title.

```
┌──────────────────────────────┐
│ [x] Order            (gray)  │
│ [x] Description              │
│ [ ] ID                       │
│ [x] Expected Result          │
│ [ ] Steps                    │
│ [x] Priority                 │
│ [ ] FQCN                     │
│    ...                       │
│ [ ] Updated At               │
└──────────────────────────────┘
```

1. **The list** — every field, one to a line, each with a tick box. The order is
   always the same.
2. **A ticked box** — that field is shown, on the cards and in the grid.
3. **Description and ID** — drawn gray. Their boxes do not answer a click or the
   space bar.
4. **Every tick** — acts at once. The list stays open, so the tester can tick
   several.

## The 18 fields

| Field | Shown to start with | Can be changed |
|---|---|---|
| Order | Yes | **No** |
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
4. The cards are measured again and drawn with a steps line.
5. The choice is remembered for the next time a test set is opened.

## What Testin refuses

**If the tester tries to untick Description** — the row is gray and does not
answer. No message is shown.

**If the tester tries to tick ID** — the same.

**If a remembered choice cannot be read** — it is dropped, and only the log says
so.

---

[Documentation](../README.md) › [The editor panel](main.md)
