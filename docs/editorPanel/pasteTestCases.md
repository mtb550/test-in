[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-017

# UC-EDITOR-PANEL-017: Paste test cases

**As a** tester, **I want** to drop the test cases I copied or cut into this
test set, **so that** they end up where I am working.

`Ctrl+Shift+V`.

## Rules

- **Rule-EDITOR-PANEL-079** — A pasted copy is a new test case with a new
  identity, and its description gains the word `(Copy)`.
- **Rule-EDITOR-PANEL-080** — A pasted cut keeps its identity, because it is the
  same test case in a new place.
- **Rule-EDITOR-PANEL-081** — Pasted test cases land at the end of the test set.
- **Rule-EDITOR-PANEL-082** — A cut and its paste are one entry on the undo
  history.
- **Rule-EDITOR-PANEL-083** — The clipboard is read as test cases. Anything else
  is turned away.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester has copied or cut test cases.
2. The tester opens the test set they want them in.
3. The tester presses `Ctrl+Shift+V`, or chooses **Paste Node**.
4. On a cut, the test cases are taken out of the test set they came from first.
5. Each test case is written into this test set.
6. The test set's order is worked out again and saved.
7. A message reads *Pasted*, with a count for more than one.

## What Testin refuses

**If the clipboard does not hold test cases** — **Paste Node** is gray.

**If the clipboard holds text that is not test cases** — it is turned away
without being read, and nothing is said.

**If the clipboard holds test cases that will not read** — the entry stays gray,
and only the log says why.

## Pasting into the same test set

Pasting a copy into the test set it came from is allowed. The result is a second
test case whose description ends in `(Copy)`, with its own identity and its own
test method.

---

[Documentation](../README.md) › [The editor panel](main.md)
