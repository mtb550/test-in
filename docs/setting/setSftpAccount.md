[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-009

# UC-SETTING-009: Name my account on the team's server

**As a** tester, **I want** to say who I am on the team's server,
**so that** a sync connects as me without asking every time.

There is no key for this. It is the **SFTP account** row.

## Rules

- **Rule 31** — The account belongs to this machine and this person. It is never
  written into the file the team shares.
- **Rule 32** — An empty account means the tester has not said. The sync then
  asks.
- **Rule 33** — The sync can write this row too, so a tester who answers the
  sync's question never has to visit this page.

Rules 1 to 6 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

## Main flow

1. The tester types their account name into the **SFTP account** row.
2. The tester presses **Apply**. Spaces around the name are removed.
3. The next sync connects with that account, and asks nothing.

## What Testin refuses

Nothing on this page. Every refusal happens at the sync, and those are on
[UC-SHARE-019](../share/syncWithServer.md).

## The address is not here

The server's address is not on this page. It lives in `testin.yml`, in the code
repository, because it is the same for everyone on the team. This row is the
half that is not.

If someone writes an account into the address in that file, Testin ignores it
and says so in the log. The file is shared with everyone, and an account is one
person's.

## The password is not here either

There is no password row. The sync asks for a password when nothing else can
prove who this is, and keeps it in the IDE's password store. That is
[UC-SHARE-020](../share/keepServerPassword.md).

---

[Documentation](../README.md) › [The settings page](main.md)
