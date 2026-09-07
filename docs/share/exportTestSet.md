[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-001

# UC-SHARE-001: Export one test set

**As a** tester, **I want** the test cases of a test set as a file,
**so that** somebody without the IDE can read them or review them.

There is no key for this. The menu entry is **Export**.

## Rules

- **Rule-SHARE-007** — The rows are written in the order the editor shows them.
- **Rule-SHARE-008** — Only what the tester ticks is written.
- **Rule-SHARE-009** — Only files ending in `.json` inside the test set are
  read, so nothing else in the folder is exported.
- **Rule-SHARE-010** — The file is written where the tester chose, never under
  the Testin folder.
- **Rule-SHARE-011** — Exporting changes no test case.
- **Rule-SHARE-012** — A test set becomes one sheet, named after itself.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Export Test Cases                                           │
├──────────────────────────────────────────────────────────────┤
│  | Login |                                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │[x]| # | Description        | Expected Result | Priority│  │
│  │[x]| 1 | Log in with a val..| The dashboard.. | P1      │  │
│  │[x]| 2 | Log in with a loc..| The account is..| P2      │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  Save to    [ C:\Users\mtb\Downloads              ] [ ... ]  │
│  File name  [ Login                                       ]  │
│  Format     [ XLSX                                        v] │
│                                                              │
│                                          [ Export ]          │
├──────────────────────────────────────────────────────────────┤
│  [k]  Escape Cancel                                          │
└──────────────────────────────────────────────────────────────┘
```

1. **The tab** — named after the test set.
2. **The tick column** — every test case arrives ticked. The box in the heading
   ticks or unticks the whole tab.
3. **The other columns** — the 17 fields. Every one but the number can be typed
   into before the file is written.
4. **Export** — writes the file. `Enter` does not.

## Main flow

1. The tester selects a test set and chooses **Export**.
2. A progress bar reads *Reading test cases in*, then the test set's name.
3. The **Export Test Cases** dialog opens, every test case ticked.
4. The tester unticks two, and corrects a typo in a third.
5. The tester picks a folder, a name and a format, then presses **Export**.
6. The dialog closes, and a bar reads *Exporting*, the count, *test cases to*,
   then the file name.
7. A message reads *Exported 8*.
8. A second message names the file and carries **Open file** and **Copy path**.

## What Testin refuses

**If more than one node is selected** — **Export** is gray.

**If the node cannot hold test cases** — **Export** is gray. Only a test set, a
test set package and the **Test Cases** folder can be exported.

**If the folder holds no test cases** — a message titled **Export Empty** reads
*No test cases found.*

**If the tester unticks every test case** — a message titled **Export Empty**
reads *Select at least one test case to export.* The dialog stays open.

**If the folder, the file name or the format is empty** — nothing is said. The
cursor moves and the dialog stays open.

**If the file cannot be written** — a message titled **Export Failed** carries
the reason.

**If one test case file cannot be read** — it is left out without a word, and
the count in the message is of what was gathered.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
