[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-011

# UC-EDITOR-PANEL-011: Remove test cases

**As a** tester, **I want** to delete test cases that are no longer wanted,
**so that** the test set is what somebody would actually run.

Testin asks first. The test case's file and its generated test method both go.

`Delete` on the selection.

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
- **Rule-EDITOR-PANEL-062** — The confirmation names the test case, or counts
  them, and says which test set they are in.
- **Rule-EDITOR-PANEL-063** — A removal can be taken back with `Ctrl+Z`.
- **Rule-EDITOR-PANEL-064** — Nothing is renumbered. The removed test case
  simply leaves a gap in the order, and the numbers on screen close up on their
  own.
- **Rule-EDITOR-PANEL-065** — A test case that is waiting to be pasted is
  removed without asking again, because the move was already agreed to.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Confirm Removing                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Remove these 4 test cases?                                  │
│                                                              │
│  From    C:\Users\mtb\Downloads\Testin\Demo\Test Cases\Login │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Remove       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — names the one test case, or counts several.
2. **From** — the test set they are in, as a path on disk.

## Main flow

1. The tester selects four cards and presses `Delete`.
2. The **Confirm Removing** dialog opens.
3. The tester presses `Enter`.
4. The four test cases leave the editor, the test project and the automation
   code.
5. A message reads *Removed 4*.

## What Testin refuses

**If nothing is selected** — **Delete** is gray and the key does nothing.

**If the tester presses `Escape`** — nothing is removed and nothing is said.

## What goes with them

The test case's file, and its generated test method. Anything the tester wrote
inside that method goes with it, and the confirmation does not mention the code.

Undoing the removal brings the test cases back. Their methods are written again,
empty. What was inside those methods does not come back.

---

[Documentation](../README.md) › [The editor panel](main.md)
