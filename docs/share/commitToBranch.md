[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-014

# UC-SHARE-014: Commit onto a different branch

**As a** tester, **I want** to put these changes on another branch,
**so that** work for the next release does not land on the one being tested now.

There is no key for this. The **Branch** box is in the review.

## Rules

- **Rule-SHARE-061** — The box lists the branches on this machine, with the one
  checked out chosen.
- **Rule-SHARE-062** — The box can also be typed into. A name that is not a
  branch yet starts one.
- **Rule-SHARE-063** — If the branch cannot be checked out, nothing at all is
  committed.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester opens the **Pending Changes** dialog.
2. The **Branch** box shows the branch they are on.
3. The tester types a name that does not exist yet.
4. The tester types a message and presses **Commit & Push**.
5. A bar reads *Preparing the branch*.
6. Testin makes the branch and checks it out.
7. The commit is made on it, and pushed.

## What Testin refuses

**If the branch cannot be checked out** — a message titled **Branch Not
Switched** reads the branch's name, then *could not be checked out, so nothing
was committed. The changes are still here and still yours.* It carries a link
reading **Review Changes**, which opens the dialog again.

**If the branch cannot be prepared for any other reason** — a message titled
**Git Error** reads *Could not prepare*, the branch, then the reason.

## Why nothing is committed on a failure

A commit that landed on the wrong branch would be worse than no commit. So the
branch is prepared first, and everything stops if it cannot be. The tester's
changes are untouched, and the message says so in those words.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
