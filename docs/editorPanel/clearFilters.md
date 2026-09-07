[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-021

# UC-EDITOR-PANEL-021: Clear the filters

**As a** tester, **I want** every test case back,
**so that** I can see the whole test set again without undoing four filters one
at a time.

There is no key for this. The entry is at the top of the filter menu.

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
- **Rule-EDITOR-PANEL-099** — **Reset Filters** clears the priority, the group,
  the module and the run status together.
- **Rule-EDITOR-PANEL-100** — It does not clear the search text.

## Main flow

1. The tester presses the filter button.
2. **Reset Filters** is the first entry.
3. The tester chooses it.
4. Every filter is cleared and the whole test set is drawn again.
5. The view goes back to the first page.
6. The count disappears from the button.

## What Testin refuses

**If no filter is on** — the entry is not drawn at all. There is nothing above
the separator.

## What is left behind

The search text stays. A tester who cannot see a test case after clearing the
filters should check the search box, which is still narrowing the list.

**Refresh** clears both. That is
[UC-EDITOR-PANEL-027](refreshEditor.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
