[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-010

# UC-SETTING-010: Name the key file this machine offers

**As a** tester, **I want** to point Testin at my private key,
**so that** a server that wants a key gets one and never asks me for a password.

There is no key for this. It is the **SFTP key file** row.

## Rules

- **Rule-SETTING-001** — One page for the whole IDE. Every code project open in
  it reads the same values.
- **Rule-SETTING-002** — Nothing on this page is checked. A folder that does not
  exist is stored exactly as typed.
- **Rule-SETTING-003** — Nothing on this page raises a message when it is saved.
- **Rule-SETTING-004** — Only a changed Testin folder makes Testin read the disk
  again. Every other setting is read where it is used, when it is used.
- **Rule-SETTING-005** — A password is never on this page. It is asked for when
  it is needed and kept in the IDE's password store.
- **Rule-SETTING-006** — Nothing on this page has a key of its own.
- **Rule-SETTING-034** — A key file named here is the way this machine proves
  who it is.
- **Rule-SETTING-035** — An agent already holding identities is tried before the
  key file itself.
- **Rule-SETTING-036** — A key file's passphrase is asked for only when it is
  going to be used, and is kept in the IDE's password store.

## Main flow

1. The tester presses the browse button on the **SFTP key file** row.
2. The tester picks their private key file.
3. The tester presses **Apply**.
4. The next sync offers that key rather than asking for a password.

## The order Testin tries

| Order | What is tried | When |
|---|---|---|
| 1 | A running agent holding identities | A key file is named |
| 2 | The key file itself, with its passphrase | A key file is named and no agent has it |
| 3 | The password the tester just typed | No key file is named |
| 4 | A running agent | No key file and no password typed |
| 5 | The password kept from last time | Nothing else answered |

## What Testin refuses

Nothing on this page. A path that names no file is stored exactly as typed, and
the server does the refusing.

## Where the passphrase goes

Never into `testin.yml`, never onto a marker file, and never into the log. It is
kept in the IDE's own password store, under a name that says which server and
which account it belongs to and holds no part of the secret itself.

---

[Documentation](../README.md) › [The settings page](main.md)
