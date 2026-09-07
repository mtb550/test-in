[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-022

# UC-EDITOR-PANEL-022: Page through the test cases

**As a** tester, **I want** to move through a long test set a page at a time,
**so that** a test set of 2,770 test cases opens as fast as one of ten.

`Ctrl+Right` and `Ctrl+Left`.

## Rules

- **Rule-EDITOR-PANEL-098** — The whole test set is paged through after the
  filters and the search have narrowed it.
- **Rule-EDITOR-PANEL-099** — An arrow with nowhere to go is gray.
- **Rule-EDITOR-PANEL-100** — Paging says nothing.
- **Rule-EDITOR-PANEL-101** — Reloading lands on whichever page holds the test
  case that was selected.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

**If this is the first page** — the two arrows on the left are gray, drawn
faded, and pressing `Ctrl+Left` does nothing.

**If this is the last page** — the two arrows on the right are the same.

## Where the plugin breaks its own rules

`Ctrl+Right` turns the page here and moves to the next test case in the view
panel. It is the same key on two panels a tester uses together. That is
difference 30 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

---

[Documentation](../README.md) › [The editor panel](main.md)
