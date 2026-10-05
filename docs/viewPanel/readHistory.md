[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-007

# UC-VIEW-PANEL-007: Read a test case's history

**As a** tester, **I want** to see what changed on a test case, who changed it
and when, **so that** I can tell whether a failure follows a change somebody
made.

The History tab reads the test case's history from Git. Each commit that changed
the test case's file is one entry. Testin stores no history of its own.

There is no key for this. The tab is called **History**.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has three tabs, and all three are drawn
  every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-096** — The History tab shows the test case's Git history on
  the current branch, newest first: one entry for each commit that changed its
  file. Each entry names when, who and the commit's message, then each field
  from what it was to what it became.
- **Rule-VIEW-PANEL-097** — A committed entry shows its commit's short hash, seven
  characters, and the full hash when the pointer rests on it.
- **Rule-VIEW-PANEL-098** — Edits saved but not committed head the list as one
  entry, Not committed yet. It names Testin's last editor and time, and each
  field that differs from the last commit. It has no hash.
- **Rule-VIEW-PANEL-099** — The history is read from Git each time the tab shows
  a test case, and Testin stores none of it. It appears as soon as it is read,
  newest first. Only a read longer than 0.3 seconds shows a Reading line, and
  one test case's history is never drawn over another's.
- **Rule-VIEW-PANEL-100** — A test project that is not under Git has no history.
  The tab says so in one line, and names Initialize Git (git init) in View
  Pending Commits as the way to start one.
- **Rule-VIEW-PANEL-101** — The tab shows every commit that changed the test
  case. Git is read away from the screen, and the entries are drawn in groups,
  so the IDE never waits for a long history.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Details    | History |   Open Bugs                                       │
├────────────────────────────────────────────────────────────────────────────┤
│  [Not committed yet]  Muteb  5 Oct 2026, 08:12                        (1)  │
│     Priority          Medium → High                                   (4)  │
│  2 Oct 2026, 16:40  Sara  [4f1c9e2]  Cycle 4 review                   (2)  │
│     Expected Result   The dashboard opens → The dashboard opens within …   │
│     Steps             3 steps → 4 steps                                    │
│  24 Sep 2026, 16:07  Muteb  [a83d07e]  UC-10                          (3)  │
│     Test Case         [Created]                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Not committed yet** — edits saved but not committed. It names who saved
   last and when. It has no hash.
2. **A committed entry** — when, who, the commit's short hash, and the commit's
   message. The full hash shows when the pointer rests on the short one.
3. **Created** — the commit that first added the test case.
4. **A field row** — the field, what it was, and what it became. A commit that
   changed none of the fields reads *reordered or restamped*.

## Main flow

1. The tester shows a test case in the view panel and clicks **History**.
2. If the read takes longer than 0.3 seconds, the tab reads *Reading the
   history from Git...*.
3. Testin asks Git for the commits that changed the test case's file, newest
   first, and reads each version.
4. The tab shows the entries. Paging to another test case reads its history the
   same way.

## What Testin refuses

**The test project is not under Git.** The tab shows one line: *This test
project is not under Git, so it has no history. Choose View Pending Commits,
then Initialize Git (git init), to start one.*

**Git cannot be read.** The tab shows one line with Git's reason. The rest of
the panel works as before.

**A version cannot be read.** Its entry still shows when, who, the hash and the
message, and says *This version could not be read*. The entry after it has
nothing to compare with, so it says *What changed is not known, because the
version before it could not be read*.

**A long history.** Every commit is shown. The first entries appear at once,
and the rest follow while the tester reads.

The Details tab's **Updated** row still says who last changed the test case,
and when.

---

[Documentation](../README.md) › [The view panel](main.md)
