[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-017

# UC-SHARE-017: Resolve the conflicts a pull stopped on

**As a** tester, **I want** to settle a file two of us changed,
**so that** the pull can finish and neither of us loses work.

There is no key for this. The offer appears on the message.

## Rules

- **Rule 72** — A pull that stops leaves the repository part way through, and
  Testin says so rather than leaving the tester to find out.
- **Rule 73** — Two answers are offered: carry on once it is settled, or roll
  the whole pull back.
- **Rule 74** — Rolling back keeps everything that was here before the pull.
- **Rule 75** — Where both sides changed different fields of one test case,
  Testin merges them and asks only about the fields that really disagree.
- **Rule 76** — A pull that will not move on is reported only to the log.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester syncs, and the pull stops on a conflict.
2. A message titled **Git Conflicts** reads *Both sides changed*, names the
   files, then *Resolve the conflict, then continue - or abort to roll the pull
   back and keep what is here.*
3. The tester chooses to carry on.
4. Testin merges each conflicting test case field by field.
5. For any field both sides rewrote, the tester is asked which one wins. That is
   [UC-SHARE-018](answerMergeQuestions.md).
6. The merged files are given back to Git and the pull carries on.
7. A message titled **Rebase continued** reads *Changes pushed to the remote*.

## What Testin refuses

**If the tester chooses to roll back** — a message titled **Rebase aborted**
reads *The pull was rolled back*, or *Nothing was pushed*. Everything that was
here before the pull is still here.

**If the roll back fails** — a message titled **Git Conflict Operation Failed**
reads *Could not abort the rebase.*

**If carrying on fails** — the same title, reading *Could not continue the
rebase.*

**If conflicts remain after the merge** — a message titled **Still Conflicting**
names them again.

**If a merged file cannot be written** — a message titled **Merge Failed** reads
*Could not write*, the path, then the reason.

**If a merged file Git will not take** — nothing is said. Only the log records
it, and the pull stops again with no conflict on screen to explain it. That is
difference 4 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

## Two fields are settled without asking

The order of a test case always takes the other side's value. Who changed it
last, and when, always take the later of the two edits. Neither is put to the
tester, and neither is reported afterwards. That is difference 6.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
