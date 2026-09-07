[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-014

# UC-EDITOR-PANEL-014: Copy a test case's details as text

**As a** tester, **I want** a test case as plain text on my clipboard,
**so that** I can paste it into a chat message or a ticket.

`Ctrl+C` on the cards.

## Rules

- **Rule-EDITOR-PANEL-071** — Each test case is written as the field name, a
  colon, then the value.
- **Rule-EDITOR-PANEL-072** — Several test cases are separated by a blank line.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

A tester who wants every field should export the test set instead, which is
[UC-SHARE-001](../share/exportTestSet.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
