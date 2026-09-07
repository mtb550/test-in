[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-009

# UC-SHARE-009: Put the test project under Git

**As a** tester, **I want** to start versioning a test project that is not under
Git yet, **so that** the team can review and pull it like code.

There is no key for this. The link is on the message that says there is no
repository.

## Rules

- **Rule-SHARE-040** — Testin offers to make the repository at the moment it
  finds there is none.
- **Rule-SHARE-041** — Nothing is committed by making the repository. Only the
  repository itself is made.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester selects a test project and chooses **View Pending Commits**.
2. Testin finds the folder is not a Git repository.
3. A message titled **Git repository not found** reads *The selected project*,
   then the folder's name, then *is not a Git repository.*
4. The message carries a link reading **Initialize Git (git init)**.
5. The tester clicks it.
6. Testin makes the repository.
7. A message reads *Git initialized*.
8. The tester chooses **View Pending Commits** again, and every file is listed
   as new.

## What Testin refuses

**If the repository cannot be made** — a message titled **Git Init Failed**
carries the reason.

**If the IDE has no Git plugin** — neither menu entry is there, and nothing says
why. That is difference 19 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

## What comes next

A new repository has no remote. The first push asks for one, which is
[UC-SHARE-013](commitAndPush.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
