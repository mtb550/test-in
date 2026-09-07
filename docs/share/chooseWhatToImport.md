[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-007

# UC-SHARE-007: Choose what is imported, and correct it

**As a** tester, **I want** to fix what the spreadsheet got wrong before it
becomes test cases, **so that** I am not correcting 40 test cases afterwards.

There is no key for this. The table is in the import dialog.

## Rules

- **Rule-SHARE-033** — The table holds the 13 columns that can be imported, not
  the 17 that can be exported.
- **Rule-SHARE-034** — The table is empty until a file is chosen.
- **Rule-SHARE-035** — Choosing a second file replaces every tab.
- **Rule-SHARE-036** — A correction made here changes what is written, and the
  file on disk is never touched.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester chooses a file, and the preview fills.
2. The tester unticks the header row somebody left in the data.
3. The tester corrects a module name that was misspelled throughout.
4. The tester presses **Import**.
5. Only the ticked rows are written, with the corrections in them.

The tick column, the whole-tab tick box and the group picker work exactly as
they do for an export, on
[UC-SHARE-003](chooseWhatToExport.md).

## What Testin refuses

**If nothing is ticked** — a message titled **Import Empty** reads *Select at
least one test case to import.*

**A value Testin cannot read is replaced, not refused.**

| In the file | What is imported |
|---|---|
| A priority Testin does not know | The lowest |
| A group Testin does not know | Dropped from the list |
| A date Testin cannot read | Blank |
| Steps on one line, numbered | Split into separate steps, with the numbers taken off |

None of these says anything. A tester importing 200 test cases whose priority
column says High, Medium and Low gets 200 test cases at the lowest priority,
with no warning. That is difference 9 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

**No Group cannot be imported.** It is offered in the group picker and dropped
when it is read back. That is difference 10.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
