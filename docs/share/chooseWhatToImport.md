[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-007

# UC-SHARE-007: Choose what is imported, and correct it

**As a** tester, **I want** to fix what the spreadsheet got wrong before it
becomes test cases, **so that** I am not correcting 40 test cases afterwards.

The import dialog shows the file in a table first. The tester unticks rows and
edits cells there, and only then are the test cases written.

There is no key for this. The table is in the import dialog.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. A Git step that
  only reads can be canceled; one that is writing to the repository or to the
  remote cannot, because a push or a rebase stopped half way leaves the
  repository in a state nobody asked for. The export, import and report ones
  cannot be canceled either.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-034** — The table holds the 13 columns that can be imported, not
  the 17 that can be exported.
- **Rule-SHARE-035** — The table is empty until a file is chosen.
- **Rule-SHARE-036** — Choosing a second file replaces every tab.
- **Rule-SHARE-037** — An import that stops part way says how many test cases
  were written before it did. They are still there.
- **Rule-SHARE-038** — A correction made here changes what is written, and the
  file on disk is never touched.

## The screen

The table fills the middle of the import dialog.
[UC-SHARE-005](importIntoTestSet.md) draws the whole dialog around it.

```
┌──────────────────────────────────────────────────────────────────────┐
│  | Login | Checkout |                                                │
│                                                                      │
│  [x]  | #   | Description         | Priority | Group    | Module     │
│  [x]  | 1   | Log in with a valid | P1     v | Smoke    | Accounts   │
│  [ ]  | 2   | Description         | Priority | Group    | Module     │
│  [x]  | 3   | Log out             | P3     v | Smoke    | Accounts   │
└──────────────────────────────────────────────────────────────────────┘
```

1. **The box in the heading** — ticks or unticks every row on this tab.
2. **The box on each row** — every row arrives ticked. Only ticked rows are
   written.
3. **The columns** — the 13 that can be imported, not the 17 that can be
   exported.
4. **The number column** — the one column that cannot be typed into.
5. **Row 2 above** — a heading row somebody left in the data. Unticking it
   keeps it out.

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

None of these says anything. Take a file of 200 test cases whose priority
column says High, Medium and Low. All 200 arrive at the lowest priority, and
nothing warns the tester. That is difference 9 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

**No Group cannot be imported.** The group picker offers it. Reading it back
drops it. That is difference 10.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
