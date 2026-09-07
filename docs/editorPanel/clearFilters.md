[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-021

# UC-EDITOR-PANEL-021: Clear the filters

**As a** tester, **I want** every test case back,
**so that** I can see the whole test set again without undoing four filters one
at a time.

There is no key for this. The entry is at the top of the filter menu.

## Rules

- **Rule-EDITOR-PANEL-097** — **Reset Filters** clears the priority, the group,
  the module and the run status together.
- **Rule-EDITOR-PANEL-098** — It does not clear the search text.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
