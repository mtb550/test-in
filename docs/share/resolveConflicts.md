[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-017

# UC-SHARE-017: Resolve the conflicts a pull stopped on

**As a** tester, **I want** to settle a file two of us changed,
**so that** the pull can finish and neither of us loses work.

A conflict is a file two people changed since they last agreed. The pull stops
until somebody says which change wins.

There is no key for this. The offer appears on the message.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. A Git step that
  only reads can be canceled; one that is writing to the repository or to the
  remote cannot, because a push or a rebase stopped half way leaves the
  repository in a state nobody asked for. The export, import and report ones can
  be canceled: none of them leaves anything the tester cannot see.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-074** — A merged test case Git will not stage is reported to the
  tester, with what to do about it. The sync cannot go on without it.

- **Rule-SHARE-075** — A pull that stops leaves the repository part way through,
  and Testin says so rather than leaving the tester to find out.
- **Rule-SHARE-076** — Three answers are offered: let Testin settle the test
  cases field by field and then carry on, carry on without merging anything, or
  roll the whole pull back.
- **Rule-SHARE-077** — Rolling back keeps everything that was here before the
  pull.
- **Rule-SHARE-078** — Where both sides changed different fields of one test
  case, Testin merges them and asks only about the fields that really disagree.
- **Rule-SHARE-109** — Two fields are settled rather than asked about, and the
  dialog says so. Who changed the test case last, and when, take the later of
  the two edits; the order takes the remote's. Neither is a question a tester
  can usefully answer about a merge, so the answer is given and named rather
  than asked for.
- **Rule-SHARE-079** — A pull that will not move on is reported only to the log.

## What the tester sees

No window opens first. A warning message appears at the bottom right of the
IDE, titled **Git Conflicts**, naming the files both sides changed. It waits in
the IDE's notification list, and three links sit under it: **Resolve**,
**Continue rebase** and **Abort rebase**.

## Main flow

1. The tester syncs, and the pull stops on a conflict.
2. A message titled **Git Conflicts** reads *Both sides changed*, names the
   files, then *Resolve the conflict, then continue - or abort to roll the pull
   back and keep what is here.*
3. The tester chooses to carry on.
4. Testin merges each conflicting test case, one field at a time.
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
names them again, and the pull is not carried on.

**If the conflicted file is not a test case** — a conflicted test run, a marker
or anything else is named in that same message and left as it is. Testin merges
test cases field by field, and a test run's result is a list of verdicts per
test case: merging one means asking about each of them, which is its own thing
to design rather than a variation of the case merge. The tester settles those by
hand, in files the IDE may not draw as conflicted, because a Testin root is not
a version control root.

**If the repository has no remote** — the rebase is carried to the end and the
push is refused, reading *This repository has no remote, so there is nothing to
push to*.

**If a merged file cannot be written** — a message titled **Merge Failed** reads
*Could not write*, the path, then the reason.

**If a merged file Git will not take** — a message titled **Merge Not
Accepted** names the file and says what to do: resolve that file in Git, then
sync again. The sync cannot go on without it.

## Two fields are settled without asking, and the dialog says which

The order of a test case takes the remote's value. Who changed it last, and
when, take the later of the two edits. Neither is a question a tester can
usefully answer about a merge - a position is not something either of them
chose, and who edited last is already in the two timestamps.

So they are not asked, and the dialog says what was decided:

*Both changed Updated At, Updated By and Order, and Testin settled them without
asking: the later edit for who changed it and when, and the remote's position
for the order.*

Only when it happened. A field one side never touched is settled by the ordinary
three-way rule, which is nobody's decision, and nothing is said about it.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
