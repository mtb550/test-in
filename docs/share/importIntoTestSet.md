[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-005

# UC-SHARE-005: Import into a test set

**As a** tester, **I want** to bring in a sheet of test cases somebody wrote
elsewhere, **so that** work done in a spreadsheet does not have to be typed
again.

There is no key for this. The menu entry is **Import**.

## Rules

- **Rule 23** — Every imported test case is new, with a new identity. An import
  never overwrites a test case that is already there.
- **Rule 24** — Imported test cases land after everything already in the test
  set. Nothing already there is touched.
- **Rule 25** — An imported test case keeps the audit the file carried, rather
  than being stamped as written now.
- **Rule 26** — Every sheet in the file goes into this one test set.
- **Rule 27** — The file is read as soon as the box holds a path Testin
  recognizes.
- **Rule 28** — Thirteen columns are read. **Order**, **ID**, **FQCN**,
  **Path** and **Status** are not.
- **Rule 29** — Importing the same file twice makes two copies of everything.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester selects a test set and chooses **Import**.
2. The **Import Test Cases** dialog opens, and a file chooser opens with it.
3. The tester picks a workbook and confirms.
4. Testin reads it at once, with no progress bar.
5. The preview fills with one tab for each sheet, every test case ticked.
6. The tester unticks what they do not want and corrects a few values.
7. The tester presses **Import**.
8. A bar reads *Importing*, the count, *test cases into*, then the test set.
9. Each test case is written, and the bar names it as it goes.
10. Test methods are generated in batches, and the bar counts them.
11. The test set's editor is closed and opened again, so the new test cases are
    in front of the tester.
12. A message reads *Imported 24*.

## What Testin refuses

**If more than one node is selected, or the node cannot hold test cases** —
**Import** is gray.

**If the file parses to nothing** — a message titled **No Data** reads *No test
cases found in the selected file.*

**If the file will not parse** — a message titled for the format, such as **CSV
Parse Error**, carries the reason. The preview stays empty.

**If the tester presses Import with nothing loaded** — a message titled **Import
Empty** reads *No data loaded from the selected file.*

**If the tester unticks every test case** — a message titled **Import Empty**
reads *Select at least one test case to import.*

**If the file is not one Testin can import** — nothing is read and nothing is
said.

**If the import fails part way** — a message titled **Import Failed** carries
the reason, and everything already written stays. Nothing says how many landed.

**If the IDE has no Java plugin** — the test cases are imported and no test
methods are generated. A message says so once for the whole code project.

## What is done with a row Testin cannot read

| The row | What happens |
|---|---|
| A column is missing, or the row stops early | Those fields are blank |
| Every value is blank | The row is skipped without a word |
| A value cannot be read | It is replaced, silently. See [UC-SHARE-007](chooseWhatToImport.md) |
| The sheet has no header row | The sheet contributes nothing |

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
