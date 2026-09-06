[Documentation](../README.md) › [The project panel](main.md) › UC-011

# UC-011: Rename a node

> **`Shift+F6`**, with a test set, a package or a test run selected. On the
> menu: **Actions → Rename**.

**As a** tester, **I want** to rename a test set, a package or a test run,
**so that** the tree says what things are called now.

## Rules

- **Rule 30** — The test project and the two containers cannot be renamed from
  the tree.
- **Rule 31** — Renaming a test set or a test set package renames its automation
  code with it, so the test case stays runnable.
- **Rule 32** — A rename can be undone.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Rename dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Rename                                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [edit]  Accounts                                       (1)  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    Escape Cancel                              │
└──────────────────────────────────────────────────────────────┘
```

1. The current name, filled in and selected. Typing replaces it.

## Main flow

1. The tester selects a test set, a package or a test run.
2. The tester presses `Shift+F6`, or chooses **Actions → Rename**.
3. The **Rename** dialog opens, with the current name filled in and selected.
4. The tester types the new name and presses `Enter`.
5. For a test set or a test set package with the Java plugin installed, Testin
   closes the open editor, renames the automation code, and only then renames
   the folder.
6. Testin refreshes the tree and shows *Renamed*.
7. `Ctrl+Z` puts the old name back, and Testin shows *Undone*.

## What Testin refuses

**If the name is empty** — the gray hint text turns red, and the dialog stays
open.

**If the name is unchanged** — the dialog closes and nothing happens, silently.

**If a sibling already has the new name** — nothing is renamed, and *\<name\>
Already Exists* is shown in red.

**If the test project or a container is selected** — **Rename** is gray, and
`Shift+F6` does nothing.

**If the folder cannot be renamed on disk** — nothing is renamed, no *Renamed* is
shown, and an error titled *Rename Failed* says why.

---

[Documentation](../README.md) › [The project panel](main.md)
