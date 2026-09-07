[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-002

# UC-SHARE-002: Export a package, one sheet for each test set

**As a** tester, **I want** everything under a package in one file with the test
sets kept apart, **so that** a reviewer gets one workbook rather than six.

A package is a folder that holds test sets. This writes all of them into one
file. Each test set becomes a sheet of its own inside it.

There is no key for this. The same **Export** entry, on a package.

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
- **Rule-SHARE-013** — Every test set beneath the package becomes one sheet,
  however deep it sits. A sheet takes the test set's own name, and the name of
  the package above it as well when two would otherwise read the same.
- **Rule-SHARE-014** — A test set holding no test cases produces no sheet.
- **Rule-SHARE-015** — A test case file that cannot be read is named before the
  export is written, and the tester decides whether to send it anyway.
- **Rule-SHARE-016** — Two sheets that would take the same name are numbered.
  The first keeps the name, and the rest get a number in brackets.

## The screen

The same dialog as [UC-SHARE-001](exportTestSet.md), with more than one tab.

```
┌──────────────────────────────────────────────────────────────┐
│  Export Test Cases                                           │
├──────────────────────────────────────────────────────────────┤
│  | Login | Checkout | Search | Payments |                    │
│  ┌────────────────────────────────────────────────────────┐  │
│  │[x]| #  | Description       | Expected Result| Priority │  │
│  │[x]| 1  | Pay with a saved. | The order is.. | P1       │  │
│  │[x]| 2  | Pay with an expi. | The card is r..| P2       │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  Destination:  [ C:\Users\mtb\Downloads           ] [ ... ]  │
│  File name:    [ Payments                         ]          │
│  Format:       [ XLSX                            v]          │
│                                                              │
│                                          [ Export ]          │
├──────────────────────────────────────────────────────────────┤
│  [k]  Escape Cancel                                          │
└──────────────────────────────────────────────────────────────┘
```

1. **The tabs** — one for each test set that holds test cases. A tab takes the
   name of its test set.
2. **The table** — the test cases of the tab in front. Each tab keeps its own
   ticks.
3. **File name** — one file is written, however many tabs there are.
4. **Export** — writes the file. `Enter` does not.

## Main flow

1. The tester selects a test set package, or the **Test Cases** folder.
2. The tester chooses **Export**.
3. The dialog opens with one tab for each test set that holds test cases.
4. The tester unticks what they do not want, on any tab.
5. The tester presses **Export**.
6. The file is written with one sheet for each tab that still has something
   ticked.

Everything else is as [UC-SHARE-001](exportTestSet.md).

## What Testin refuses

**If a tab has nothing ticked** — that sheet is left out of the file, and
nothing is said. If no tab has anything ticked, the whole export is refused.

**If a test set is nested two levels down** — it is exported like any other.
Every test set under the package becomes a sheet, however deep it sits.

## What each format does with the sheets

| Format | What it makes of several sheets |
|---|---|
| **XLSX** | One sheet in the workbook for each |
| **CSV** | One header line, then every test case from every sheet. The sheet names are not written anywhere |
| **HTML** | One heading and one table for each sheet, then a total |
| **JSON** | The sheet names are kept as the keys |

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
