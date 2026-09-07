[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-008

# UC-SETTING-008: Turn the shortcut hints off

**As a** tester, **I want** the strip of key hints gone from the bottom of every
Testin dialog, **so that** the dialog is shorter once I know the keys.

There is no key for this. It is the checkbox at the bottom of the page.

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
- **Rule-SETTING-027** — The hints are on until the tester turns them off.
- **Rule-SETTING-028** — One answer covers every Testin dialog at once.
- **Rule-SETTING-029** — The setting is read as each strip is drawn, so it takes
  effect on the next dialog, not the next IDE.
- **Rule-SETTING-030** — A dialog may still leave its own strip out for its own
  reasons. The setting can only take a strip away, never add one.

## The screen

The strip runs along the bottom of a Testin dialog. It is a keyboard picture,
then each key and what it does.

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Case                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   set description                                            │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Save       Escape Cancel       Ctrl+D Description│
└──────────────────────────────────────────────────────────────┘
```

1. **The keyboard picture** — always first.
2. **Each hint** — the key in bold, one space, then what it does.
3. **The gaps** — seven spaces between one hint and the next.

The hints change as the tester moves between fields, so the strip always shows
the keys that work right now.

## Main flow

1. The tester clears the **Show keyboard shortcuts in dialogs** checkbox.
2. The tester presses **Apply**.
3. The next Testin dialog opens without the strip.

## What Testin refuses

Nothing.

## Why it is on to start with

The keys are the point of Testin. A tester who never finds `P`, `F` and `B` runs
a test run with the mouse and never learns why the plugin is faster than a
spreadsheet. So every dialog says what its keys are, until the tester says they
have learned them.

---

[Documentation](../README.md) › [The settings page](main.md)
