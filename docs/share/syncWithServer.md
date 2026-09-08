[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-019

# UC-SHARE-019: Sync with the team's server

**As a** tester, **I want** to send the test project to the team's server and
take what is there, **so that** a team without Git still shares one set of test
cases.

SFTP is a safe way to copy files to a server. This sends the test project
there, and brings back what other testers put there.

There is no key for this. The menu entry is **Sync With SFTP**.

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
- **Rule-SHARE-085** — The server's address comes from `testin.yml`, which the
  team shares. The account comes from this machine's settings.
- **Rule-SHARE-086** — An account is never written into the shared file. If one
  is found there it is ignored, and the log says so.
- **Rule-SHARE-087** — Only one machine syncs a test project at a time. A second
  is told who is syncing.
- **Rule-SHARE-088** — The server must already be known to this machine. One
  that is not is refused rather than trusted.
- **Rule-SHARE-089** — The sync sends and takes in one gesture.
- **Rule-SHARE-090** — A test case both sides changed is merged field by
  field. What the merge cannot settle is kept as it is here, and nothing is sent
  for it.
- **Rule-SHARE-091** — Testin reads the test project again itself after a sync,
  because it ignores its own writes.

## The screen

The account is asked for when this machine cannot prove who it is.

```
┌──────────────────────────────────────────────────────────────┐
│  Connect to files.example.com                                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  The server address comes from testin.yml and is shared      │
│  with the team. The account is yours, and is kept on this    │
│  machine only.                                               │
│                                                              │
│  [ account on the server...                               ]  │
│                                                              │
│  [ password, if no key is set up...                       ]  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Connect       Escape Cancel                      │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the words **Connect to**, then the server. A port other
   than the usual one is shown too.
2. **The account box** — filled with the stored account, or with this machine's
   own user name.
3. **The password box** — always empty when the window opens. An empty
   password is a real answer. It means a key will prove who this is.

## Main flow

1. The tester selects a test project and chooses **Sync With SFTP**.
2. Testin reads the server address from `testin.yml`.
3. If it cannot prove who this is, the account window opens.
4. The tester types their account and presses `Enter`.
5. The account is stored on this machine at once.
6. A background task named *Syncing with*, then the server, starts.
7. Testin sends what is newer here and takes what is newer there.
8. A message titled **Synced** says what was sent, taken and merged.

## What Testin refuses

**If the test project is not reached over a server** — the menu entry is gray.

**If `testin.yml` names no server** — a message titled **No SFTP Server
Configured** reads *Set connection: sftp and sftpHost in testin.yml*.

**If no test project is selected** — a message titled **Nothing to Sync** reads
*Select a test project in the tree first.*

**If nothing can prove the account** — the account window opens again, rather
than showing the server's refusal.

**If somebody else is syncing** — a message titled **Somebody else is syncing
this project** names who, and when they started. It then reads *Nothing was
sent or fetched. Try again when they have finished.*

**If the connection fails** — a message titled **Sync Failed** reads *Could not
connect to*, the server, then the reason.

## What the message says afterwards

| What happened | The message |
|---|---|
| Nothing to do | **Synced** — *Already up to date* |
| Work moved | **Synced** — what was sent, taken, merged, left to the tester, and gone from the server |
| Conflicts were left | **Synced, with**, the count, **left to you** — and it stays in the notification list |

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
