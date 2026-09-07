[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-019

# UC-SHARE-019: Sync with the team's server

**As a** tester, **I want** to send the test project to the team's server and
take what is there, **so that** a team without Git still shares one set of test
cases.

There is no key for this. The menu entry is **Sync With SFTP**.

## Rules

- **Rule-SHARE-082** — The server's address comes from `testin.yml`, which the
  team shares. The account comes from this machine's settings.
- **Rule-SHARE-083** — An account is never written into the shared file. If one
  is found there it is ignored, and the log says so.
- **Rule-SHARE-084** — Only one machine syncs a test project at a time. A second
  is told who is syncing.
- **Rule-SHARE-085** — The server must already be known to this machine. One
  that is not is refused rather than trusted.
- **Rule-SHARE-086** — The sync sends and takes in one gesture.
- **Rule-SHARE-087** — A file both sides changed is kept as it is here, and
  nothing is sent for it.
- **Rule-SHARE-088** — Testin reads the test project again itself after a sync,
  because it ignores its own writes.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

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

1. **The title** — the word **Connect to**, then the server. A port other than
   the usual one is shown too.
2. **The account box** — filled with the stored account, or with this machine's
   own user name.
3. **The password box** — always empty when the window opens. An empty password
   is a real answer, meaning a key will do the proving.

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
this project** names who and when, then reads *Nothing was sent or fetched. Try
again when they have finished.*

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
