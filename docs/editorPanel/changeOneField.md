[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-006

# UC-EDITOR-PANEL-006: Change one field of one test case

**As a** tester, **I want** to correct one field without opening a form with
twenty boxes on it, **so that** fixing a typo takes two keys.

One field, one small dialog. The rest of the test case is left alone.

`F2` opens the menu of fields. Each field also has a letter of its own.

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
- **Rule-EDITOR-PANEL-035** — Only the field the tester opened may be written
  back. Every other field on the dialog is gray.
- **Rule-EDITOR-PANEL-036** — The dialog always shows the description, and shows
  the expected result when it is not empty, so the tester can see what they are
  changing.
- **Rule-EDITOR-PANEL-037** — A save that changed nothing writes nothing and
  says nothing.
- **Rule-EDITOR-PANEL-038** — One gesture is one entry on the undo history,
  however many test cases it changed.
- **Rule-EDITOR-PANEL-039** — Undo puts the test case back exactly, including
  who last changed it and when.
- **Rule-EDITOR-PANEL-194** — Status is on the update menu and has no letter of
  its own. It is the one field with no key, because every letter that would name
  it is taken by a field a tester reaches more often.

## The screen

```
┌────────────────────────────────────────┐
│  Update Test Case                      │
├────────────────────────────────────────┤
│  Description             D             │
│  Expected Result         E             │
│  Module                  M             │
│  Test Data               T             │
│  Pre Conditions          B             │
│  Steps                   S             │
│  Priority                P             │
│  Group                   G             │
│  Status                                │
│  Order                   O             │
└────────────────────────────────────────┘
```

1. **Each row** — the field, then the letter that opens it.
2. **The first row** — selected when the menu opens.
3. **Status** — the one row with no letter. It is opened from this menu and
   nowhere else.

Pressing the letter on the card skips this menu and opens the field straight
away.

## Main flow

1. The tester selects a card.
2. The tester presses `E`, or presses `F2` and picks **Expected Result**.
3. The **Update Expected Result** dialog opens with the current value in it.
4. Every other field on the dialog is gray.
5. The tester types and presses `Enter`.
6. Testin writes the test case.
7. A message reads *Updated*.
8. The list is sorted again and redrawn, and the automation code is rewritten.

## What Testin refuses

**If nothing is selected** — **Update** is gray and `F2` does nothing.

**If the save changed nothing** — the dialog closes and nothing at all happens.

**If the description is emptied** — the field turns red and the dialog stays
open.

**If the description cannot name a Java method** — the same message the create
dialog shows.

**If several test cases are selected** — the menu title becomes **Update**, then
the count, then **Test Cases**. Picking a field opens the bulk editor instead.
That is [UC-EDITOR-PANEL-007](bulkEdit.md).

## The dialog cannot be moved

The update dialog cannot be moved or resized. The create dialog can. Nothing
explains the difference.

---

[Documentation](../README.md) › [The editor panel](main.md)
