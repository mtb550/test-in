[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-009

# UC-SHARE-009: Put the test project under Git

**As a** tester, **I want** to start versioning a test project that is not under
Git yet, **so that** the team can review and pull it like code.

Git is the tool a team uses to keep every version of its files. A folder Git
watches is called a repository. This turns the test project's folder into one.

There is no key for this. The link is on the message that says there is no
repository.

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
- **Rule-SHARE-042** — Testin offers to make the repository at the moment it
  finds there is none.
- **Rule-SHARE-043** — Nothing is committed by making the repository. Only the
  repository itself is made.

## What the tester sees

No window opens. A warning message appears at the bottom right of the IDE,
titled **Git repository not found**, and it stays in the IDE's notification
list. One link sits under it, reading **Initialize Git (git init)**; clicking
it makes the repository, and a small message reads *Git initialized*.

## Main flow

1. The tester selects a test project and chooses **View Pending Commits**.
2. Testin finds the folder is not a Git repository.
3. A message titled **Git repository not found** reads *The selected project*,
   then the folder's name, then *is not a Git repository.*
4. The message carries a link reading **Initialize Git (git init)**.
5. The tester clicks it.
6. Testin makes the repository.
7. A message reads *Git initialized*.
8. The tester chooses **View Pending Commits** again. Every file is now listed
   as new.

## What Testin refuses

**If the repository cannot be made** — a message titled **Git Init Failed**
carries the reason.

**If the IDE has no Git plugin** — neither menu entry is there, and nothing
says why. That is difference 19 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

## What comes next

A new repository has no remote. A remote is the team's copy, the one everybody
pushes to. The first push asks for its address, which is
[UC-SHARE-013](commitAndPush.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
