[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-015

# UC-EDITOR-PANEL-015: Copy test cases

**As a** tester, **I want** to copy test cases so I can paste them into another
test set, **so that** a set of login tests can be the start of a set of sign-up
tests.

`Ctrl+Shift+C`.

## Rules

- **Rule-EDITOR-PANEL-072** — Copying puts the test cases themselves on the
  clipboard, not their text.
- **Rule-EDITOR-PANEL-073** — Copying calls off any cut that was waiting, and
  the faded cards come back.
- **Rule-EDITOR-PANEL-074** — The key works in both views.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects three cards.
2. The tester presses `Ctrl+Shift+C`, or chooses **Copy Node**.
3. The three test cases go on the clipboard.
4. A message reads *Copied 3*.
5. The tester opens another test set and presses `Ctrl+Shift+V`.

## What Testin refuses

**If nothing is selected** — **Copy Node** is gray and the key does nothing.

**If writing to the clipboard fails** — nothing is said, and only the log
records it.

## What a pasted copy becomes

A copy is a new test case with a new identity, and its description gains the
word `(Copy)`. It gets a test method of its own, so the two do not collide.
Pasting is [UC-EDITOR-PANEL-017](pasteTestCases.md).

## Two different copies

`Ctrl+C` and `Ctrl+Shift+C` are not the same. The first copies text a person can
read. The second copies test cases Testin can paste. They use different keys
because they are different things.

---

[Documentation](../README.md) › [The editor panel](main.md)
