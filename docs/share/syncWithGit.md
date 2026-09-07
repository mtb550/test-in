[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-016

# UC-SHARE-016: Send my changes and take the team's

**As a** tester, **I want** one gesture that brings the team's test cases in and
sends mine out, **so that** I do not have to remember two.

A sync takes the team's changes first, then sends the tester's. One press does
both, so nothing is left behind.

There is no key for this. The menu entry is **Sync With Remote**.

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
- **Rule-SHARE-068** — The branch that is synced is the one that is checked out.
- **Rule-SHARE-069** — A sync pulls and then pushes, so a sync that succeeded
  never leaves the tester's commits only here.
- **Rule-SHARE-070** — The push is skipped when nothing is waiting, so a sync
  pressed out of habit costs no network.
- **Rule-SHARE-071** — The pull rebases, and stashes anything uncommitted first.
- **Rule-SHARE-072** — The progress bar can be canceled.
- **Rule-SHARE-073** — Afterwards the working folder, Testin's own reading of
  it, and the tree are all read again.

## What the tester sees

No window opens. A progress bar named *Syncing with remote* runs in the status
bar, and its line changes as the sync works. At the end a message titled
**Synced** appears at the bottom right and fades.

## Main flow

1. The tester selects the test project and chooses **Sync With Remote**.
2. A bar titled *Syncing with remote* opens, and can be canceled.
3. The bar reads *Checking remote configuration...*.
4. The bar reads *Pulling latest changes from*, then the branch.
5. If anything is waiting, the bar reads *Pushing what is committed here...*.
6. The bar reads *Refreshing files...*.
7. A message titled **Synced** reads *Up to date with the remote*, or *Pushed*
   and the count.

## What Testin refuses

**If nothing that is a test project is selected** — the menu entry is gray.

**If the folder is not a Git repository** — a message titled **Nothing to
Sync** names the folder, then reads *is not under Git yet. Open Pending Commits
to create the repository.*

**If no remote address is set** — a message titled **Sync Aborted** reads *No
remote URL is configured for this project. Push a commit first to configure the
remote.*

**If Git cannot name a branch** — the sync fails, and the message says so.

**If the pull stops on a conflict** — the conflict offer appears instead of
anything else. That is [UC-SHARE-017](resolveConflicts.md).

**If anything else fails** — a message titled **Sync Failed** reads *Could not
sync with the remote:*. The reason is on the next line.

**If the IDE has no Git plugin** — the menu entry is not there, and nothing says
why.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
