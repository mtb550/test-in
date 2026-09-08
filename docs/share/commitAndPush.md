[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-013

# UC-SHARE-013: Commit and push

**As a** tester, **I want** my work recorded and sent in one press,
**so that** the team has it without a second gesture I might forget.

A commit records the work here. A push sends it to the team's copy, which Git
calls the remote. This does both in one press.

There is no key for this. **Commit & Push** is the face of the split button.

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
- **Rule-SHARE-059** — The push happens only after the commit succeeded.
- **Rule-SHARE-060** — A repository with no remote asks for one, once.
- **Rule-SHARE-061** — The message about a push stays in the IDE's notification
  list, because a push finishes on its own time.
- **Rule-SHARE-062** — Any password inside the remote address is taken out of
  anything Testin shows or logs.

## What the tester sees

The button opens no window of its own. The dialog closes and a progress bar
named *Committing and pushing* runs in the status bar. When it ends, a message
titled **Pushed** appears at the bottom right. It waits in the IDE's
notification list, because a push can finish while the tester is reading
something else.

## Main flow

1. The tester ticks what to send, types a message, and presses **Commit &
   Push**.
2. Testin commits, exactly as
   [UC-SHARE-012](commitChanges.md) describes.
3. Testin reads the remote address.
4. Testin pushes the branch.
5. A message titled **Pushed** reads *Commit*, then the short identifier, then
   *is on*, then the remote and the branch.

## What Testin refuses

**If no remote is set** — a window titled **Configure Remote** asks for one. It
reads *No remote repository is configured for this project.*, then asks for the
address.

**If the tester cancels that window** — a message titled **Push Aborted** reads
*A remote URL is required to push.* The commit was already made.

**If the address cannot be added** — a message titled **Git Error** reads
*Failed to add remote:* and then the reason.

**If the remote cannot be read** — a message titled **Git Error** reads *Could
not read the Git remote:* and then the reason.

**If the push fails** — a message titled **Push Failed** carries the reason.

## Where the plugin breaks its own rules

**The remote address is never checked.** Any text is taken. The failure arrives
later in Git's own words. That is difference 18 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

**A commit made and not pushed leaves work on this machine.** If the push
fails, the commit still stands. The work is recorded here and not sent. The
next review offers to push it. That is [UC-SHARE-015](pushOldCommit.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
