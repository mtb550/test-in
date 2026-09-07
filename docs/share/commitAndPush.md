[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-013

# UC-SHARE-013: Commit and push

**As a** tester, **I want** my work recorded and sent in one press,
**so that** the team has it without a second gesture I might forget.

There is no key for this. **Commit & Push** is the face of the split button.

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
- **Rule-SHARE-059** — The push happens only after the commit succeeded.
- **Rule-SHARE-060** — A repository with no remote asks for one, once.
- **Rule-SHARE-061** — The message about a push stays in the IDE's notification
  list, because a push finishes on its own time.
- **Rule-SHARE-062** — Any password inside the remote address is taken out of
  anything Testin shows or logs.

## Main flow

1. The tester ticks what to send, types a message, and presses **Commit &
   Push**.
2. Testin commits, exactly as
   [UC-SHARE-012](commitChanges.md) describes.
3. Testin reads the remote address.
4. Testin pushes the branch.
5. A message titled **Pushed** reads *Commit*, the short identifier, *is on*,
   then the remote and the branch.

## What Testin refuses

**If no remote is set** — a window titled **Configure Remote** asks for one. Its
text reads *No remote repository is configured for this project.* and then asks
for the address.

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

**A commit made and not pushed leaves work on this machine.** If the push half
fails, the commit half stands. The tester's work is recorded and not sent, and
the next review will offer to push it. That is
[UC-SHARE-015](pushOldCommit.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
