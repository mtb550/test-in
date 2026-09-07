[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-009

# UC-SETTING-009: Name my account on the team's server

**As a** tester, **I want** to say who I am on the team's server,
**so that** a sync connects as me without asking every time.

This is the name the tester logs in with on the team's server.

There is no key for this. It is the **SFTP account** row.

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
- **Rule-SETTING-031** — The account belongs to this machine and this person. It
  is never written into the file the team shares.
- **Rule-SETTING-032** — An empty account means the tester has not said. The
  sync then asks.
- **Rule-SETTING-033** — The sync can write this row too, so a tester who
  answers the sync's question never has to visit this page.

## The screen

The row is the sixth one on the page.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  SFTP account:  [ muteb                                      ]           │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The caption** — **SFTP account**.
2. **The box** — a plain text box. There is no password box beside it.

The whole page is drawn on [the settings page](main.md#the-page).

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
half that is not the same for everyone.

Someone may write an account into the address in that file. Testin ignores it
and says so in the log. The file is shared with everyone, and an account is one
person's.

## The password is not here either

There is no password row. The sync asks for a password only when nothing else
can prove who this is. It then keeps the password in the IDE's password store.
That is [UC-SHARE-020](../share/keepServerPassword.md).

---

[Documentation](../README.md) › [The settings page](main.md)
