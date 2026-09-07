[Documentation](../README.md) › Inside Testin

# Inside Testin

Two things in Testin belong to no panel. The search opens from anywhere in the
IDE. The one part that reads and writes the files on disk sits under every
panel, so everything a tester reads has come through it.

A tester meets both without going looking for them. That is why they are written
up here rather than left to the code.

| | |
|---|---|
| **Part of Testin** | The parts that belong to no panel |
| **Answers** | What the search finds, and what Testin does with the files under the Testin folder |
| **Numbering** | Use cases are `UC-INTERNAL-001` and up. Rules are `Rule-INTERNAL-001` and up, and belong to this part |
| **Last rule** | `Rule-INTERNAL-052`. The next rule written here is `Rule-INTERNAL-053` |
| **State** | **Written** |
| **Checked against** | `main` at `2cb8c1dc`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

| | What the tester does | Where its rules are |
|---|---|---|
| **UC-INTERNAL-001** | [Find anything in the test project](globalSearch.md) | Rule-INTERNAL-001 to Rule-INTERNAL-002 |
| **UC-INTERNAL-002** | [Read a test project from disk](readTestProject.md) | Rule-INTERNAL-003 to Rule-INTERNAL-014 |
| **UC-INTERNAL-003** | [Pick up a change made outside the IDE](noticeOutsideChange.md) | Rule-INTERNAL-016 to Rule-INTERNAL-024 |
| **UC-INTERNAL-004** | [Give a test case its number](numberTestCase.md) | Rule-INTERNAL-025 to Rule-INTERNAL-035 |
| **UC-INTERNAL-005** | [Keep a removed node so it can come back](keepRemovedNode.md) | Rule-INTERNAL-036 to Rule-INTERNAL-045 |
| **UC-INTERNAL-006** | [Count what a node holds](countNodeContents.md) | Rule-INTERNAL-046 to Rule-INTERNAL-052 |

---

## What this part is for

Testin keeps a whole test project in memory. Every panel, every dialog and every
report is a question answered from memory, not a trip to disk. That is why a
tree of 2,770 test cases opens at once.

Memory that is wrong is worse than disk that is slow. So most of this part is
about keeping the two the same: reading the files once at the start, noticing
when something changes them, and making sure only one thing is ever writing.

## The words it uses

- **The Testin folder** is the one folder on this machine that holds test
  projects. It is set once, on the settings page.
- **A marker file** is a small file Testin writes beside a node, saying what the
  node is. A test project holds one named `.tp`. A test set holds one named
  `.ts`. Testin uses seven of them.
- **To read** a test project is to walk its folders once and hold everything it
  holds in memory.
- **A kept copy** is the copy Testin puts aside when it removes something, so
  `Ctrl+Z` has something to put back.

## Every key, in one place

| Key | What it does | The page that owns it |
|---|---|---|
| `Ctrl+Alt+F`, `Cmd+Alt+F` on a Mac | Opens search, from anywhere in the IDE | [UC-INTERNAL-001](globalSearch.md) |

Nothing else in this part has a key of its own. It runs when a panel asks it to.

---

## Why it is built this way

**One place owns every file.** No other part of Testin reads or writes test
data. Everything else holds what that one place gave it. The reason is that a
second writer cannot be kept honest: two of them once wrote the same test run,
the older write landed second, and the tester found their failure analysis gone
after the next reload.

Six other parts of Testin do touch files, and none of them touches test data.
They write generated source code, the code repository's own settings file, the
Git working folder, files chosen outside the Testin folder, generated reports,
and the log.

**A write is claimed before it happens, not after.** Testin tells itself which
file it is about to write, then writes it. The other order looks the same and is
not: the change can be noticed while the write is still running, and a file
claimed a moment too late looks like somebody else's edit.

**Nothing empty is ever written.** Writing nothing over a marker file empties
it, and an empty marker takes its node's history with it. Six markers in a real
test project were left at zero bytes that way before the rule was added.

**A node is forgotten only after the file is actually gone.** A file the IDE
refused to delete was once dropped from memory anyway, and the tree stopped
showing a node that was still on disk.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | Rule-INTERNAL-047 — a container is the sum of everything beneath it | Details counts retired test sets. Making a test run on the same node leaves them out. A test project whose Details says 40 test cases can offer 31, and nothing explains the difference. |
| **Difference 2** | Rule-INTERNAL-040 — one press puts back what one removal took | A failed undo shows two messages on one press. *Undone* arrives first, then **Undo Incomplete**. A tester who reads only the first believes a node is back. |
| **Difference 4** | Rule-INTERNAL-014 — nothing that cannot be read stops the rest | A damaged marker file leaves its node drawn with default values. Its number, its status and who made it are silently wrong, and only the log says so. |
| **Difference 5** | Rule-INTERNAL-019 — Testin ignores its own writes for five seconds | A tester who edits a file by hand within five seconds of Testin saving it is ignored too. The edit is on disk and not on screen until **Refresh**. |

A sixth belongs to the tree panel and is written there. A removal whose copy
could not be kept aside still happens, is not undoable, and says nothing. That
is difference 15 on
[the tree panel page](../treePanel/main.md#where-the-plugin-breaks-its-own-rules).

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone | Was |
|---|---|
| **Difference 3** | Test cases in an unmarked folder were invisible everywhere, and nothing said so. Fixed 7 September 2026, [#276](https://github.com/mtb550/test-in/issues/276) |

---

## Not decided

**Question 1** — Should Testin say when it skips an unmarked folder holding test
cases? It is silent today, so test cases can exist and be invisible. Saying so
on every read would be noise in a folder that is deliberately not a test set.

**Question 2** — Should a damaged marker file be shown to the tester? The node
appears with default values today, and only the log says why.

**Question 3** — Five seconds is how long Testin ignores its own writes. Nobody
has measured whether it is the right number, and nothing shows it to the tester.

---

[Documentation](../README.md) › **Inside Testin**
