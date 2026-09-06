[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-010

# UC-SHARE-010: See what I have not committed

**As a** tester, **I want** to see every change I have made since my last
commit, **so that** I can decide what to send and what to put back.

There is no key on the menu. `Shift+Enter` reaches it from the message about
uncommitted work.

## Rules

- **Rule 42** — The review reads Git directly, so a repository the IDE does not
  track and a test case Git has never seen both appear.
- **Rule 43** — One row for each changed **field**, not one for each changed
  file.
- **Rule 44** — A changed file with nothing readable different still gets a row,
  so it can be committed.
- **Rule 45** — What a file is is decided by reading it, not by its name.
- **Rule 46** — A rename is two rows, the removal first and the addition second.
- **Rule 47** — Every row arrives selected.
- **Rule 48** — Reading Git happens off the main thread, so the IDE stays
  usable.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Pending Changes                                                           │
├────────────────────────────────────────────────────────────────────────────┤
│ [x]| Change Type      | Test Set | Name          | Before   | After        │
│ [x]| Description      | Login    | Log in with.. | Log in   | Sign in      │
│ [x]| Expected Result  | Login    | Log in with.. | It opens | It opens now │
│ [x]| New Test Case    | Login    | Log out       |          | Log out      │
├────────────────────────────────────────────────────────────────────────────┤
│  Branch     [ main                                                      v] │
│                                                                            │
│  [ what changed, in a line...                                            ] │
│                                                                            │
│                                        [ Commit & Push  v ]                │
├────────────────────────────────────────────────────────────────────────────┤
│  [k]  Right click Revert a change       Escape Cancel                      │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The tick column** — every row arrives ticked.
2. **Change Type** — what kind of change this row is.
3. **Test Set** — filled for a test case, and blank for a test run, a marker or
   any other file.
4. **Name** — the test case's description, or the file's name.
5. **Before** and **After** — the two values of the field that changed.
6. **Branch** — the branch to commit onto. It can be typed into, which starts a
   new branch.
7. **The message box** — no label, only its gray hint.
8. **Commit & Push** — a split button. Its arrow offers **Commit** alone.

## Main flow

1. The tester selects a test project and chooses **View Pending Commits**.
2. A progress bar reads *Scanning for changes*, and can be canceled.
3. Testin asks Git what has changed, and reads each changed file.
4. The **Pending Changes** dialog opens with one row for each changed field.
5. The tester unticks the rows they are not ready to send.
6. The tester types a message and presses **Commit & Push**.

## What Testin refuses

**If nothing that is a test project is selected** — the menu entry is gray.

**If the folder is not a Git repository** — the message offering to make one.
That is [UC-SHARE-009](putUnderGit.md).

**If a pull stopped on a conflict earlier** — the conflict offer appears instead
of the review. That is [UC-SHARE-017](resolveConflicts.md).

**If nothing has changed and nothing is unpushed** — a message reads *No
changes*.

**If nothing has changed but a commit never reached the remote** — the message
about that instead. That is [UC-SHARE-015](pushOldCommit.md).

**If one file cannot be read** — the row is still listed, with only what Git
said about it.

**If Git listed a new file that is already gone** — the row is dropped, and only
the log says so.

**If the IDE has no Git plugin** — the menu entry is not there at all.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
