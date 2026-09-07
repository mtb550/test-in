[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-003

# UC-SHARE-003: Choose what goes into the export, and correct it

**As a** tester, **I want** to drop a few test cases and fix a typo before the
file leaves, **so that** what I send is what I meant to send.

There is no key for this. The table is in the export dialog.

## Rules

- **Rule-SHARE-016** — Every test case arrives ticked.
- **Rule-SHARE-017** — The box in the first column's heading ticks or unticks
  the whole tab.
- **Rule-SHARE-018** — Every column but the number can be typed into.
- **Rule-SHARE-019** — A correction made here changes the file, and never the
  test case itself.
- **Rule-SHARE-020** — Moving away from a cell saves what was typed in it.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## The two special columns

**Priority** is a list offering **P1**, **P2** and **P3**.

**Group** opens a picker. Clicking the cell opens a window listing every group,
with the test case's own already selected. `Ctrl+Click` adds one, `Enter`
confirms and `Escape` cancels. The chosen groups are written back joined by
commas.

## Main flow

1. The export dialog opens with the table filled.
2. The tester unticks three test cases that are not ready.
3. The tester clicks an expected result and corrects it.
4. The tester clicks a **Group** cell and adds **Smoke**.
5. The tester presses **Export**.
6. Only the ticked rows are written, with the corrections in them.

## What Testin refuses

**If nothing is ticked on any tab** — a message titled **Export Empty** reads
*Select at least one test case to export.*

**A value Testin cannot read is not refused, it is replaced.**

| The tester types | What is written |
|---|---|
| A priority Testin does not know | The lowest |
| A status Testin does not know | Whatever the row had already |
| A group Testin does not know | Dropped from the list |
| A date Testin cannot read | An empty cell |

None of the four says anything. That is difference 9 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

**If the priority or the group column cannot be found** — both pickers are
missing from the whole table, and only the log says so.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
