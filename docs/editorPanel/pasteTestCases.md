[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-017

# UC-EDITOR-PANEL-017: Paste test cases

**As a** tester, **I want** to drop the test cases I copied or cut into this
test set, **so that** they end up where I am working.

This is the second half of a copy or a cut. It is the press that really moves
them.

the **Paste Test Case** entry on the right-click menu. It has no key of its
own.

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
- **Rule-EDITOR-PANEL-081** — A pasted copy is a new test case with a new
  identity, and its description gains the word `(Copy)`.
- **Rule-EDITOR-PANEL-082** — A pasted cut keeps its identity, because it is the
  same test case in a new place.
- **Rule-EDITOR-PANEL-083** — Pasted test cases land at the end of the test set.
- **Rule-EDITOR-PANEL-084** — A cut and its paste are one entry on the undo
  history.
- **Rule-EDITOR-PANEL-085** — The clipboard is read as test cases. Anything else
  is turned away.

## What the tester sees

This opens no screen. The pasted test cases appear at the end of the list. On a
cut they also disappear from the test set they came from.

A small message appears at the bottom of the IDE and fades. It reads *Pasted*,
with a count after it for more than one test case.

## Main flow

1. The tester has copied or cut test cases.
2. The tester opens the test set they want them in.
3. The tester chooses **Paste Test Case** from the right-click menu.
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
test case. Its description ends in `(Copy)`. It has its own identity and its own
test method.

---

[Documentation](../README.md) › [The editor panel](main.md)
