[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-014

# UC-EDITOR-PANEL-014: Copy a test case's details as text

**As a** tester, **I want** a test case as plain text on my clipboard,
**so that** I can paste it into a chat message or a ticket.

This copies words a person can read. It does not copy the test case itself.

`Ctrl+C` on the cards.

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
- **Rule-EDITOR-PANEL-073** — Each test case is written as the field name, a
  colon, then the value.
- **Rule-EDITOR-PANEL-074** — Several test cases are separated by a blank line.

## What the tester sees

This opens no screen. Nothing on the list changes, because nothing was written.

A small message appears at the bottom of the IDE and fades. It reads *Details
copied*, with a count after it when more than one card was selected.

## Main flow

1. The tester selects two cards and presses `Ctrl+C`.
2. The text goes on the clipboard.
3. A message reads *Details copied 2*.

## What Testin refuses

**If nothing is selected** — **Copy** is gray and the key does nothing.

**If the grid is showing** — `Ctrl+C` belongs to the grid there, and copies the
selected cells instead. That is
[UC-EDITOR-PANEL-018](gridClipboard.md).

## Where the plugin breaks its own rules

**Only the description is copied.** The message says *Details copied*, and what
lands on the clipboard is one line reading `Description:` and the text. The
expected result, the steps, the priority and every other field are left out.
That is difference 1 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

A tester who wants every field should export the test set instead. That is
[UC-SHARE-001](../share/exportTestSet.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
