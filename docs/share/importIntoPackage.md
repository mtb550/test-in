[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-006

# UC-SHARE-006: Import into a package, one test set for each sheet

**As a** tester, **I want** a workbook of six sheets to become six test sets,
**so that** a whole test plan written in a spreadsheet arrives in one gesture.

There is no key for this. The same **Import** entry, on a package.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. The Git ones can be
  canceled. The export, import and report ones cannot.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-031** — One new test set is made for each sheet, named after the
  sheet with special characters removed.
- **Rule-SHARE-032** — The test sets are made before any test case is written,
  because making one generates a Java class.
- **Rule-SHARE-033** — No editor is opened afterwards, because a package has no
  editor of its own.

## Main flow

1. The tester selects a test set package, or the **Test Cases** folder.
2. The tester chooses **Import** and picks a workbook of six sheets.
3. The preview shows six tabs.
4. The tester presses **Import**.
5. Six test sets are made, named after the sheets.
6. Each sheet's test cases are written into its own test set.
7. A message reads *Imported*, then the total.

Everything else is as [UC-SHARE-005](importIntoTestSet.md).

## Where the sheet names come from

| The file | The sheet name |
|---|---|
| A spreadsheet | The name of the sheet in the workbook |
| A comma separated file | The file name, without its ending |
| A JSON file | Whatever keys the file carries |

## What Testin refuses

**If a new test set's Java class cannot be generated** — the test set is still
made, and only the log says so.

Every other refusal is the same as importing into one test set.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
