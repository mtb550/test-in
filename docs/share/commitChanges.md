[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-012

# UC-SHARE-012: Commit without pushing

**As a** tester, **I want** to record my work locally,
**so that** I can decide about the team's copy later.

There is no key for this. **Commit** is behind the arrow of the split button.

## Rules

- **Rule 52** — The marker files above every committed test case are committed
  too, whether they were ticked or not.
- **Rule 53** — Only a path that is really on disk is added. A path that is gone
  is committed as the removal it is.
- **Rule 54** — No Git command Testin runs may open an editor.
- **Rule 55** — A password inside a remote address is never written to the log
  or shown.
- **Rule 56** — The list of paths reaches Git in a file, not on the command
  line, so a very large commit does not fail for length.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester unticks what they are not ready to send.
2. The tester types a message.
3. The tester opens the arrow beside **Commit & Push** and chooses **Commit**.
4. The dialog closes.
5. A bar reads *Preparing the branch*, then another reads *Committing to local
   Git*.
6. Testin adds the ticked paths and their markers, and commits.
7. A message titled **Committed** names the commit.

## What Testin refuses

**If no row is ticked** — the button is gray.

**If the message is blank** — its gray hint turns red and the box takes the
cursor. Nothing is committed and the dialog stays open.

**If Git does not know who the tester is** — the identity dialog opens instead,
and the commit follows it. That is
[UC-SHARE-008](setGitIdentity.md).

**If the commit fails for any other reason** — a message titled **Commit
Failed** reads *Failed to commit changes:* and then the reason.

## Why the markers go too

A test set carries a small marker file saying what it is. A test case committed
without the marker of the test set above it arrives in a colleague's clone as a
file in a folder Testin does not recognize, so the test case is invisible there.
Committing the markers is not optional, and Testin does it whether they were
ticked or not.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
