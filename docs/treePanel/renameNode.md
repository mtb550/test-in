[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-011

# UC-TREE-PANEL-011: Rename a node

> **`Shift+F6`**, with a test set, a package or a test run selected. On the
> menu: **Actions → Rename**.

**As a** tester, **I want** to rename a test set, a package or a test run,
**so that** the tree says what things are called now.

This changes the name of one node. Nothing inside it moves.

## Rules

- **Rule-TREE-PANEL-001** — The panel shows exactly one test project. It is the
  one this repository is bound to. There is no list of test projects in the
  tree.
- **Rule-TREE-PANEL-002** — **Test Cases** and **Test Runs** are fixed
  containers. They cannot be created, renamed, moved, copied or removed. They
  come with the test project and go with it.
- **Rule-TREE-PANEL-003** — The tree has two sides, and nothing moves between
  them. Test sets and test set packages live under **Test Cases**. Test runs and
  test run packages live under **Test Runs**.
- **Rule-TREE-PANEL-004** — Two nodes under one parent cannot share a name. This
  holds whether the node is created, renamed, pasted or dropped.
- **Rule-TREE-PANEL-005** — A node name is never empty.
- **Rule-TREE-PANEL-006** — Removing, moving or copying asks first. Nothing in
  the tree changes until the tester confirms.
- **Rule-TREE-PANEL-007** — When something changes, Testin says so once, in the
  past tense. The tester sees *Created*, *Renamed* or *Removed*. Several changes
  at once confirm once, with a count: the tester sees *Removed 4*, never four
  messages. Looking at something confirms nothing.
- **Rule-TREE-PANEL-008** — A retired node keeps everything inside it. Retired
  means a **Deprecated** test set or an **Archived** package. It is drawn gray.
  It sorts after live nodes. It is not offered when a test run is created.
- **Rule-TREE-PANEL-009** — A test run that is **Completed** or **Closed** is
  signed off. Its test cases, verdicts and configuration can no longer change.
- **Rule-TREE-PANEL-010** — Siblings are shown in one order:
  1. live nodes, then retired ones
  2. the number the tester gave the node
  3. the date the node was created
  4. the name
- **Rule-TREE-PANEL-011** — A removed node goes to the desktop's recycle bin. It
  can be put back from the tree.
- **Rule-TREE-PANEL-012** — A test case is not a node in this tree. It is
  reached by opening its test set.
- **Rule-TREE-PANEL-013** — Nodes move only within one test project. Nothing cut
  in one test project can be pasted into another.
- **Rule-TREE-PANEL-099** — A node says its status beside its name when the status
  is not active. An inactive test project, a deprecated test set and an archived
  package each say which they are, in gray; a node in current work says nothing,
  because "Active" on every name is a word read a hundred times and needed
  never. A test run always says its status, because where a cycle stands is what
  the tree is read for.
- **Rule-TREE-PANEL-035** — The test project and the two containers cannot be
  renamed from the tree.
- **Rule-TREE-PANEL-036** — Renaming a test set or a test set package renames
  its automation code with it, so the test case stays runnable.
- **Rule-TREE-PANEL-037** — A rename can be undone.

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
   *set new name...*. The cursor sits after the name. So typing **adds to the
   name** instead of replacing it. `Ctrl+A` selects the whole name. Spaces at
   either end are dropped before anything else happens.

## Main flow

1. The tester selects a test set, a package or a test run.
2. The tester presses `Shift+F6`, or chooses **Actions → Rename**.
3. The **Rename** dialog opens, with the current name filled in.
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

**If the name cannot be a Java package** — the dialog stays open with the name
still in the box, and *'\<name\>' cannot name a Java package* is shown in red.
`New`, `Class` and `Import` are the usual ones: Java keeps those words for
itself. (Rule-TREE-PANEL-095) Only the test project and the test set
packages are asked: a test set's name becomes the class, which always ends in
`Test`, and a test run generates no code at all.

**If the test project or a container is selected** — **Rename** is gray, and
`Shift+F6` does nothing.

**If the folder cannot be renamed on disk** — nothing is renamed and no
*Renamed* is shown. An IDE notification titled *Rename Failed* stays in the
notification log, reading *Operation failed:* and the reason, or *Could not find
path on disk:* and the path. The other refusals above are balloons that fade.
This one is not.

**If several rows are selected** — **Rename** stays black and renames the first
of them, saying nothing about the rest.

**If the Java plugin is not installed** — the first rename in the project shows
*Java Plugin Not Available*, reading *Automation code generation and navigation
require the Java plugin, which is not available in this IDE.* The rename still
happens. Testin says this once per project.

> **A rename that failed is still on the undo history.** After *Rename Failed*,
> **Actions → Undo Rename** is offered and does nothing useful.

---

[Documentation](../README.md) › [The tree panel](main.md)
