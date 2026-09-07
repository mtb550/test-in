[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-020

# UC-SHARE-020: Have my password kept for next time

**As a** tester, **I want** to type my password once,
**so that** the next sync does not ask again.

The password is kept in the IDE's own password store, which is the safe place
this machine already has for secrets.

Nothing starts this. It happens when a password is typed in the account window.

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
- **Rule-SHARE-092** — A password is kept in the IDE's own password store. Never
  in `testin.yml`, never on a marker file, never in the log.
- **Rule-SHARE-093** — It is kept for one server and one account, so two
  servers, or two accounts on one, do not overwrite each other.
- **Rule-SHARE-094** — The name it is kept under says which server and which
  account, and holds no part of the secret.
- **Rule-SHARE-095** — A password the tester has just typed is preferred over
  one kept from before, so a corrected password works on the attempt it was
  corrected on.

## What the tester sees

Nothing new opens, and nothing says the password was kept. The tester types it
in the password box of the **Connect to** window, which
[UC-SHARE-019](syncWithServer.md) draws, and the sync starts. Only a refusal is
reported, as a small message titled **Password Not Kept**.

## Main flow

1. The tester types a password in the account window and presses `Enter`.
2. The sync starts in the background.
3. In the background, the password is written to the IDE's password store.
4. A later sync has no key file, no typed password and no agent. The stored
   password is read back and used.

## What Testin refuses

**If this machine's keychain will not keep it** — a message titled **Password
Not Kept** reads *This machine's keychain refused it, so the next sync asks
again.* The sync itself carries on.

**If the keychain cannot be read** — nothing is said. An empty password is
used, and the server is the one that refuses.

## The order Testin tries to prove who this is

| Order | What is tried |
|---|---|
| 1 | A running agent, when a key file is named |
| 2 | The key file itself, with its passphrase |
| 3 | The password the tester just typed |
| 4 | A running agent |
| 5 | The password kept from last time |

Naming a key file is [UC-SETTING-010](../setting/setSftpKeyFile.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
