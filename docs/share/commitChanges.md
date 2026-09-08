[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-012

# UC-SHARE-012: Commit without pushing

**As a** tester, **I want** to record my work locally,
**so that** I can decide about the team's copy later.

A commit records the work in Git on this machine. Nothing is sent to the team
until it is pushed.

There is no key for this. **Commit** is behind the arrow of the split button.

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
  repository in a state nobody asked for. The export, import and report ones
  cannot be canceled either.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-054** — The marker files above every committed test case are
  committed too, whether they were ticked or not.
- **Rule-SHARE-055** — Only a path that is really on disk is added. A path that
  is gone is committed as the removal it is.
- **Rule-SHARE-056** — No Git command Testin runs may open an editor.
- **Rule-SHARE-057** — A password inside a remote address is never written to
  the log or shown.
- **Rule-SHARE-058** — The list of paths reaches Git in a file, not on the
  command line, so a very large commit does not fail for length.

## The screen

The button sits at the bottom right of the review dialog.
[UC-SHARE-010](reviewChanges.md) draws the whole dialog.

```
  [ Commit & Push   v ]
  ┌────────────────────┐
  │  Commit            │
  └────────────────────┘
```

1. **Commit & Push** — the face of the button. A click runs it.
2. **The arrow** — opens the one other answer.
3. **Commit** — records the work on this machine, and sends nothing.

## Main flow

1. The tester unticks what they are not ready to send.
2. The tester types a message.
3. The tester opens the arrow beside **Commit & Push** and chooses **Commit**.
4. The dialog closes.
5. A bar reads *Preparing the branch*. A second bar reads *Committing to
   local Git*.
6. Testin adds the ticked paths and their markers, and commits.
7. A message titled **Committed** names the commit.

## What Testin refuses

**If no row is ticked** — the button is gray.

**If the message is blank** — the gray hint turns red and the box takes the
cursor. Nothing is committed. The dialog stays open.

**If Git does not know who the tester is** — the identity dialog opens instead,
and the commit follows it. That is
[UC-SHARE-008](setGitIdentity.md).

**If the commit fails for any other reason** — a message titled **Commit
Failed** reads *Failed to commit changes:* and then the reason.

## Why the markers go too

A test set carries a small marker file that says what it is. Now picture a test
case committed without that marker. In a colleague's copy it lands in a folder
Testin does not know, so the test case is invisible there. Committing the
markers is therefore not optional, and Testin does it whether they were ticked
or not.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
