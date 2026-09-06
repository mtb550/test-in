[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-002

# UC-INTERNAL-002: Read a test project from disk

**As a** tester, **I want** Testin to read my test projects into memory before it
draws anything, **so that** every panel answers at once instead of going to disk
for each row.

There is no key for this. It starts on its own.

## Rules

- **Rule-INTERNAL-003** — A folder is a test project only if it holds a file named `.tp`.
- **Rule-INTERNAL-004** — Only the folders directly inside the Testin folder are looked at.
  A test project one level deeper is not found.
- **Rule-INTERNAL-005** — An archived test project is not read at all.
- **Rule-INTERNAL-006** — When `testin.yml` names a test project, only that one is read.
- **Rule-INTERNAL-007** — Inside a test project, Testin reads two folders. They are named
  `Test Cases` and `Test Runs`. Nothing else is read.
- **Rule-INTERNAL-008** — A folder inside `Test Cases` is read only if it holds a file
  named `.ts` or a file named `.tsp`. The first makes it a test set. The second
  makes it a test set package.
- **Rule-INTERNAL-009** — A folder holding both is read as a test set.
- **Rule-INTERNAL-010** — A folder inside `Test Runs` is read the same way, from a file
  named `.tr` or a file named `.trp`.
- **Rule-INTERNAL-011** — Every file ending in `.json` directly inside a test set is a
  test case.
- **Rule-INTERNAL-012** — A test case is known by its file name. What the file says its
  own name is does not decide.
- **Rule-INTERNAL-013** — Each test project is read by its own background job. The tester
  can cancel it, and the IDE stays usable while it runs.
- **Rule-INTERNAL-014** — One thing that cannot be read never stops the rest. Testin skips
  it and carries on.
- **Rule-INTERNAL-052** — A folder skipped for having no marker is reported when it holds
  test cases. A folder holding none is skipped in silence, because a folder that
  is deliberately not a test set is the ordinary case.

## What starts a read

| The tester does this | What Testin reads |
|---|---|
| Opens a code project that has a Testin folder set | Every test project |
| Presses **Refresh** on the panel toolbar | Every test project, from nothing |
| Changes a file outside the IDE | The one test project that holds it, see [UC-INTERNAL-003](noticeOutsideChange.md) |
| Finishes a sync, over Git or over SFTP | The one test project that was synced |
| Switches branch | The one test project in that repository |
| Clones a test project | The one that was cloned |

## The screen

Testin shows one progress bar for each test project it is reading. It sits at
the bottom of the IDE, beside the other background jobs.

```
┌──────────────────────────────────────────────────────────────┐
│  Testin indexing - Checkout                            [ X ] │
│  ████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  40% │
│  Test set: Payment (12 cases)                                │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always *Testin indexing*, then the test project's name.
2. **The cancel button** — stops this test project. What was read is kept.
3. **The line underneath** — the test set or test run being read right now.

## Main flow

1. The tester opens a code project, or presses **Refresh**.
2. Testin reads the Testin folder from the settings.
3. Testin lists the folders directly inside it, and keeps those holding a `.tp`
   file.
4. Testin starts one background job for each of them.
5. The bar reads *Indexing*, then the test project's name.
6. The bar reads the test project's name, then *test sets...*.
7. Testin walks `Test Cases`. For each test set it reads every `.json` file at
   once, not one after another.
8. After each test set the bar reads *Test set:*, then its name, then how many
   test cases it holds.
9. The bar reads the test project's name, then *test runs...*.
10. Testin walks `Test Runs`. Each test run reads its recorded results.
11. The bar reads *Done -*, then the test project's name, and closes.
12. The tree draws itself from memory. Every editor that was open when the IDE
    closed opens again.

## What Testin refuses

**If no Testin folder is set** — nothing is read, and nothing is said. The panel
shows its empty state instead.

**If a folder under the Testin folder holds no `.tp` file** — it is not a test
project, and it is skipped without a word. This is the ordinary case. The
Testin folder usually holds other things.

**If the `.tp` file cannot be read** — the tester gets a notification titled
**Read Test Project Failed**, reading *Skipping invalid format:* and the file
name. That whole test project is left out.

**If a `.ts`, `.tsp`, `.tr` or `.trp` file cannot be read** — the same
notification, titled for that kind of node. The message reads *Failed to parse
directory:* and the file name.

**If a marker file is missing** — nothing is said. A node just created has no
marker yet, so Testin uses defaults and the node appears normally.

**If a marker file is there but damaged** — the node still appears, with default
values. Its number, its status and who made it are lost. Nothing on screen says
so.

**If a folder under `Test Cases` holds no `.ts` and no `.tsp` file** — the
folder is skipped, and so is everything inside it. When it holds test cases,
Testin says so when the read finishes. One message names the folders, and it
stays in the notification list rather than fading, because a read finishes on
its own time.

**If such a folder holds no test cases** — it is skipped without a word. Most
folders that are not test sets are nothing, and reporting each one would be
noise.

**If one test case file cannot be read** — that one test case is left out of its
test set. The others are read. The set is drawn one row shorter, and nothing
says which row is missing.

**If two test case files claim the same identity** — both are read as separate
test cases. The file name decides, so a test case copied by hand becomes a
second, independent test case.

**If the tester presses cancel** — the read stops between one test set and the
next. What was already read stays in memory. The rest of that test project is
missing until the next **Refresh**.

**If the whole read fails** — Testin does not mark itself as read, and tries
again the next time something asks it to.

---

[Documentation](../README.md) › [Inside Testin](main.md)
