[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-008

# UC-SETTING-008: Turn the shortcut hints off

**As a** tester, **I want** the strip of key hints gone from the bottom of every
Testin dialog, **so that** the dialog is shorter once I know the keys.

There is no key for this. It is the checkbox at the bottom of the page.

## Rules

- **Rule 27** — The hints are on until the tester turns them off.
- **Rule 28** — One answer covers every Testin dialog at once.
- **Rule 29** — The setting is read as each strip is drawn, so it takes effect
  on the next dialog, not the next IDE.
- **Rule 30** — A dialog may still leave its own strip out for its own reasons.
  The setting can only take a strip away, never add one.

Rules 1 to 6 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

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
