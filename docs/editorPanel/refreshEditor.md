[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-027

# UC-EDITOR-PANEL-027: Refresh the editor from disk

**As a** tester, **I want** to read the test set again from disk,
**so that** I see what a colleague's sync brought in.

There is no key for this. The button's tooltip reads **Refresh**.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing. Paging, filtering,
  searching and opening the details panel are all silent. It changes nothing
  either: a test case the filter is hiding is reported, never brought into view
  by throwing the filter away.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-117** — Refresh keeps every filter and the search text. It
  reads the data again and changes nothing else about the view.
- **Rule-EDITOR-PANEL-118** — Refresh remembers which test case was selected,
  and lands on the page holding it.
- **Rule-EDITOR-PANEL-119** — The tester's own refresh always reloads, even with
  a grid cell open. A refresh Testin starts on its own leaves a busy editor
  alone.
- **Rule-EDITOR-PANEL-120** — In a test run editor, refresh also stops the
  execution, and the message says so. Refresh reads the run again from disk and
  the walk goes with the copy it replaces.

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
