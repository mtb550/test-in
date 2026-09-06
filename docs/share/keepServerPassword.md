[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-020

# UC-SHARE-020: Have my password kept for next time

**As a** tester, **I want** to type my password once,
**so that** the next sync does not ask again.

Nothing starts this. It happens when a password is typed in the account window.

## Rules

- **Rule 89** — A password is kept in the IDE's own password store. Never in
  `testin.yml`, never on a marker file, never in the log.
- **Rule 90** — It is kept for one server and one account, so two servers, or
  two accounts on one, do not overwrite each other.
- **Rule 91** — The name it is kept under says which server and which account,
  and holds no part of the secret.
- **Rule 92** — A password the tester has just typed is preferred over one kept
  from before, so a corrected password works on the attempt it was corrected on.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester types a password in the account window and presses `Enter`.
2. The sync starts in the background.
3. Off the main thread, the password is written to the IDE's password store.
4. On a later sync with no key file, no password typed and no agent, the stored
   password is read back and used.

## What Testin refuses

**If this machine's keychain will not keep it** — a message titled **Password
Not Kept** reads *This machine's keychain refused it, so the next sync asks
again.* The sync itself carries on.

**If the keychain cannot be read** — nothing is said. The empty password is
used, and the server does the refusing.

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
