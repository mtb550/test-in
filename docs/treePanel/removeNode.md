[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-012

# UC-TREE-PANEL-012: Remove a node

> **`Delete`**, with one or more nodes selected. On the menu:
> **Actions → Remove**.

**As a** tester, **I want** to remove a test set, a package, a test run or a
whole test project, **so that** the tree holds only what is current.

## Rules

- **Rule 33** — The confirmation says what will go. For one node, it says what
  the node holds and where it is. For several nodes, it says how many.
- **Rule 34** — Removing a test set, a test set package or a test project
  removes its automation code with it.
- **Rule 35** — A removal can be undone. (rule 11)
- **Rule 36** — The two containers are never removed. This holds even when they
  are selected together with nodes that are removed.

Rules 1 to 13 hold everywhere in the panel. They are on
[the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Confirm Removing dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Confirm Removing                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Remove 'Accounts'?                                     (1)  │
│  Holds 2 test sets, 14 test cases and 0 test runs       (2)  │
│                                                              │
│  From:  C:\Testin\Demo\Test Cases\Accounts             (3)   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Remove    Escape Cancel                          (4)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — names the node.
2. **What it holds** — the line the tester reads before pressing `Enter`. It is
   left out when the node holds no test sets, no test cases and no test runs.
   Packages it holds are counted nowhere and never keep the line on screen.
3. **Where it is** — in gray. It is the node's full path on disk, with the
   operating system's own separators, not a trail of node names.
4. The confirm key is named for what it does. It reads **Remove**, not **OK**.

For several nodes, the question is *Remove these N items?*. It shows no counts
and no path.

## Main flow

1. The tester selects one or more removable nodes.
2. The tester presses `Delete`, or chooses **Actions → Remove**.
3. The **Confirm Removing** dialog opens. For one node it asks *Remove
   '\<name\>'?*, says how many test sets, test cases and test runs that node
   holds, and shows where it is after *From:*. For several nodes it asks *Remove
   these N items?*, where N is how many.
4. The tester presses `Enter`.
5. Testin closes each node's editor and keeps a copy for undo.
6. The node goes to the desktop's recycle bin, and its automation code goes with
   it. On a desktop with no recycle bin the node is deleted outright.
7. The tree rebuilds, and Testin shows *Removed*, or *Removed N* for several,
   counting only what actually went.

## What Testin refuses

**If the tester presses `Escape`** — the dialog closes and nothing is removed.

**If only Test Cases or Test Runs is selected** — **Remove** is gray, and
`Delete` does nothing.

**If a container is selected together with a test set** — only the test set is
removed. The container is left out, and not counted.

**If nothing could be removed** — the tree rebuilds and Testin says nothing at
all.

**If a node cannot be deleted on disk** — an IDE notification titled *Delete
Failed* stays in the notification log, reading *Could not delete file:* and the
reason.

> **A node whose copy could not be kept aside is still removed, and cannot be
> undone.** Nothing says so at the time. `Ctrl+Z` then takes back whatever
> change came before the removal instead.

---

[Documentation](../README.md) › [The tree panel](main.md)
