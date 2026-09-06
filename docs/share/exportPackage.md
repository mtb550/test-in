[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-002

# UC-SHARE-002: Export a package, one sheet for each test set

**As a** tester, **I want** everything under a package in one file with the test
sets kept apart, **so that** a reviewer gets one workbook rather than six.

There is no key for this. The same **Export** entry, on a package.

## Rules

- **Rule 13** — Each test set directly under the package becomes one sheet,
  named after itself.
- **Rule 14** — A test set holding no test cases produces no sheet.
- **Rule 15** — Two sheets that would take the same name are numbered. The first
  keeps the name, and the rest get a number in brackets.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

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

**If a tab has nothing ticked** — that sheet is left out of the file entirely,
and nothing is said. If no tab has anything ticked, the export is refused.

**If a test set is nested two levels down** — it is not exported at all, and
nothing says so. Only the test sets directly under the package are walked. That
is difference 7 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

## What each format does with the sheets

| Format | What it makes of several sheets |
|---|---|
| **XLSX** | One sheet in the workbook for each |
| **CSV** | One header line, then every test case from every sheet. The sheet names are not written anywhere |
| **HTML** | One heading and one table for each sheet, then a total |
| **JSON** | The sheet names are kept as the keys |

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
