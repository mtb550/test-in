[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-004

# UC-SETTING-004: Give my name

**As a** tester, **I want** my name recorded on the work I do,
**so that** a colleague reading a test case or a verdict can see who last
touched it.

Testin writes this name onto everything this machine saves.

There is no key for this. It is the **Tester name** row.

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
- **Rule-SETTING-018** — The name is read at the moment it is stamped, not
  remembered from when the IDE started. Changing it takes effect at once.
- **Rule-SETTING-019** — An empty name is allowed. Testin then stamps nothing,
  which means the file did not say rather than that nobody chose.

## The screen

The row is the third one on the page.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Tester name:  [ muteb                                       ]           │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The caption** — **Tester name**.
2. **The box** — a plain text box. It has no gray example in it and no browse
   button beside it.

The whole page is drawn on [the settings page](main.md#the-page).

## Where the name is stamped

| What the tester does | What carries the name |
|---|---|
| Creates any node in the tree | Who created it |
| Renames a node | Who last changed it |
| Changes a test project's, a test set's or a package's status | Who last changed it |
| Saves a test case | Who created it, or who last changed it |
| Records a verdict in a test run | Who ran it |
| Starts a sync with the team's server | Who is syncing, so others can see |

The name appears on the Details popup of any node, and on the **Created By** and
**Updated By** rows of the view panel.

## Main flow

1. The tester types their name into the **Tester name** row.
2. The tester presses **Apply**.
3. From that moment, everything this machine writes carries the name.

## What Testin refuses

Nothing. An empty name is accepted, and every stamp is then left empty.

## What it does not do

The name is not sent anywhere. It is also not the name Git records on a commit.
Git is told who the tester is in its own place, and that is
[UC-SHARE-008](../share/setGitIdentity.md).

---

[Documentation](../README.md) › [The settings page](main.md)
