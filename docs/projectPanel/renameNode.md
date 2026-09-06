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

1. **One field**, filled in with the current name. Its gray hint text reads
   *set new name...*. The cursor sits after the name, so typing **adds to it**
   rather than replacing it. `Ctrl+A` selects it all. Spaces at either end are
   dropped before anything is done.

## Main flow

1. The tester selects a test set, a package or a test run.
2. The tester presses `Shift+F6`, or chooses **Actions → Rename**.
3. The **Rename** dialog opens, with the current name filled in and selected.
4. The tester types the new name and presses `Enter`.
5. Testin closes the node's open editor, whatever kind it is.
6. For a test set or a test set package, and only with the Java plugin
   installed, Testin renames the automation code next.
7. Testin renames the folder.
8. Testin refreshes the tree and shows *Renamed*.
9. `Ctrl+Z` puts the old name back, and Testin shows *Undone*.

## What Testin refuses

**If the name is empty** — the gray hint text turns red, and the dialog stays
open.

**If the name is unchanged** — the dialog closes and nothing happens, silently.
Renaming `Accounts` to `  Accounts  ` counts as unchanged, because the spaces
are dropped first.

**If a sibling already has the new name** — nothing is renamed, and *\<name\>
Already Exists* is shown in red.

**If the test project or a container is selected** — **Rename** is gray, and
`Shift+F6` does nothing.

**If the folder cannot be renamed on disk** — nothing is renamed and no *Renamed*
is shown. An IDE notification titled *Rename Failed* stays in the notification
log, reading *Operation failed:* and the reason, or *Could not find path on
disk:* and the path. The other refusals above are fading balloons; this one is
not.

**If several rows are selected** — **Rename** stays black and renames the first
of them, saying nothing about the rest.

**If the Java plugin is not installed** — the first rename in the project shows
*Java Plugin Not Available*, reading *Automation code generation and navigation
require the Java plugin, which is not available in this IDE.* The rename still
happens. It is said once per project.

> **A rename that failed is still on the undo history.** After *Rename Failed*,
> **Actions → Undo Rename** is offered and does nothing useful.

---

[Documentation](../README.md) › [The project panel](main.md)
