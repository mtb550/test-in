[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-001

# UC-SETTING-001: Open the settings page

**As a** tester, **I want** to reach Testin's settings quickly,
**so that** I can tell it where my test data is without searching the IDE's
own settings tree.

This page holds every Testin setting for this machine. It is one page, and it
is the only place most of these values are set.

There is no key for this. The fastest way in is the gear button on the tree
panel's toolbar.

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
- **Rule-SETTING-007** — The gear button on the tree panel opens this page
  directly.
- **Rule-SETTING-008** — **Apply** and **OK** stay gray until something on the
  page differs from what is stored.
- **Rule-SETTING-009** — Pressing **Apply** writes every field at once, not only
  the one that changed.

## The screen

The gear button sits on the tree panel's toolbar, near the left.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Testin                                                                  │
├──────────────────────────────────────────────────────────────────────────┤
│  [ search ] [ gear ] [ expand ] [ collapse ] [ refresh ] [ project ]     │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Demo                                                                   │
│     Test Cases                                                           │
│       Accounts                                                           │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The toolbar** — the row of buttons at the top of the tree panel.
2. **The gear** — the second button. Its tooltip reads **Configure Testin
   settings**.
3. **The tree** — under the toolbar. This gesture does not change it.

Pressing the gear opens the IDE's settings window on Testin's page. That page
is drawn on [the settings page](main.md#the-page).

## The four ways in

| The tester does this | Where it is |
|---|---|
| Presses the gear button | The tree panel's toolbar. Its tooltip reads **Configure Testin settings** |
| Opens **Settings**, then **Tools**, then **Testin** | The IDE's own settings window |
| Clicks **Open Settings** on the setup message | The message shown when no Testin folder is set |
| Clicks **Configure Testin settings** | The tree panel's empty state, when no Testin folder is set |

## Main flow

1. The tester presses the gear button on the tree panel's toolbar.
2. The IDE's settings window opens on Testin's page.
3. Every field is filled with what is stored now.
4. The tester changes a field. **Apply** and **OK** become live.
5. The tester presses **Apply**.
6. Every field is written to this machine's settings.
7. If the Testin folder changed, every code project that has opened the Testin
   panel reads the disk again.

## What Testin refuses

Nothing. No value on this page is checked, and pressing **Apply** never fails.

## Where the plugin breaks its own rules

Some code projects do not notice a new Testin folder. A code project reads the
disk again only if its Testin panel has been opened once. A project whose panel
was never opened keeps the old folder until the panel is opened. That is
difference 5 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The settings page](main.md)
