[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-022

# UC-EDITOR-PANEL-022: Page through the test cases

**As a** tester, **I want** to move through a long test set a page at a time,
**so that** a test set of 2,770 test cases opens as fast as one of ten.

Testin never draws the whole test set at once. It draws one page.

`Ctrl+Right` and `Ctrl+Left`.

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
- **Rule-EDITOR-PANEL-101** — The whole test set is paged through after the
  filters and the search have narrowed it.
- **Rule-EDITOR-PANEL-102** — An arrow with nowhere to go is gray.
- **Rule-EDITOR-PANEL-103** — Paging says nothing.
- **Rule-EDITOR-PANEL-104** — Reloading lands on whichever page holds the test
  case that was selected.

## The screen

The five controls sit in the middle of the status bar.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  1 of 12 test cases            |<   <   2 of 3   >   >|            [ 50 ]  │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **First page** — no key. Its tooltip reads **First page**.
2. **Previous page** — `Ctrl+Left`.
3. **The label** — which page this is, of how many.
4. **Next page** — `Ctrl+Right`.
5. **Last page** — no key.

## Main flow

1. The tester presses `Ctrl+Right`.
2. The next page of test cases is drawn.
3. The label reads the new page number.
4. Nothing is said.

## What Testin refuses

**If this is the first page** — the two arrows on the left are gray and drawn
faded. `Ctrl+Left` does nothing.

**If this is the last page** — the two arrows on the right are the same.

## Where the plugin breaks its own rules

`Ctrl+Right` turns the page here and moves to the next test case in the view
panel. It is the same key on two panels a tester uses together. That is
difference 30 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

---

[Documentation](../README.md) › [The editor panel](main.md)
