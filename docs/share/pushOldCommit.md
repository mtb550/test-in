[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-015

# UC-SHARE-015: Push a commit that never left this machine

**As a** tester, **I want** to be told when I have committed something and not
sent it, **so that** work I think the team has is not sitting on my laptop.

A commit only records work on this machine. A push is what sends it. This says
when a push is still owed.

There is no key for this. The message appears when the review finds nothing else
to do.

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
- **Rule-SHARE-066** — Testin counts what is committed here and not on the
  remote, and says so rather than reporting no changes.
- **Rule-SHARE-067** — The message counts commits, not files.

## What the tester sees

No window opens. A warning message appears at the bottom right of the IDE,
titled **Not Pushed**, and it waits in the IDE's notification list. One link
sits under it, reading **Push**.

## Main flow

1. The tester committed yesterday and the push failed.
2. Today the tester chooses **View Pending Commits**.
3. Nothing has changed since, so there is nothing to review.
4. A message titled **Not Pushed** reads the count, then *commits are committed
   here and not on the remote.*
5. The tester clicks **Push** on the message, or chooses **Sync With Remote**.
   Either one sends them.

## What Testin refuses

**If nothing has changed and nothing is unpushed** — a message reads *No
changes*, with no title.

## Why this exists

The review dialog answers one question: what have I not sent. A tester whose
push failed has nothing to review, and nothing to review reads as everything
sent. So Testin counts the commits the remote has not got, and says so
instead.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
