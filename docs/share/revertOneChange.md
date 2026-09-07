[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-011

# UC-SHARE-011: Put one changed field back

**As a** tester, **I want** to undo one field I changed by mistake,
**so that** I do not have to throw away a morning's work to lose one typo.

Right click the row in the review.

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
- **Rule-SHARE-051** — Only a change to a test case's field can be put back.
- **Rule-SHARE-052** — Only that one field is put back. Everything else on the
  test case stays as it is.
- **Rule-SHARE-053** — The change is written to disk at once, not on the commit.

## Main flow

1. The tester is reading the **Pending Changes** table.
2. One row shows a description they did not mean to change.
3. The tester right-clicks that row.
4. A menu offers **Revert this change**.
5. The tester chooses it.
6. The field goes back to what it was before.
7. A message reads *Reverted*.
8. The row leaves the table.

## What Testin refuses

**If the row is not a change to a test case** — a message reads *Only a test
case change can be reverted*.

**If the kind of change cannot be put back** — a message reads *A change to*,
then what kind it is, then *cannot be reverted*. A new file and a removed file
are two of those.

**If the test case is no longer in the test project** — a message reads *That
test case is no longer in the project*.

**If the write fails** — a message titled **Revert Failed** carries the reason.

## What cannot be put back this way

A whole file. A new test case. A removed test case. A change to a test run, a
marker or anything Testin did not recognize as a test case. For those, Git's own
tools are the answer.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
