[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-012

# UC-TREE-PANEL-012: Remove a node

> **`Delete`**, with one or more nodes selected. On the menu:
> **Actions → Remove**.

**As a** tester, **I want** to remove a test set, a package, a test run or a
whole test project, **so that** the tree holds only what is current.

The node goes to the recycle bin, so nothing is lost for good.

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
- **Rule-TREE-PANEL-038** — The confirmation says what will go. For one node, it
  says what the node holds and where it is. For several nodes, it says how many.
- **Rule-TREE-PANEL-039** — Removing a test set, a test set package or a test
  project removes its automation code with it.
- **Rule-TREE-PANEL-040** — A removal can be undone. (Rule-TREE-PANEL-011)
- **Rule-TREE-PANEL-041** — A node whose copy could not be kept aside is still
  removed, and Testin says at that moment that it cannot be undone. `Ctrl+Z`
  then answers for that removal, and does not take back the change before it.
- **Rule-TREE-PANEL-042** — The two containers are never removed. This holds
  even when they are selected together with nodes that are removed.
- **Rule-TREE-PANEL-094** — A test run can be removed whatever its status. A
  signed-off run still refuses a rename, a new number and a drag, because a run
  that is renamed or moved is still named in a report and now described wrongly
  - but a removed run is not misdescribed, it is gone, and a reader who cannot
  find it knows exactly that.

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
   Packages inside the node are never counted. So a node holding only packages
   shows no line.
3. **Where it is** — in gray. It is the node's full path on disk. It uses the
   operating system's own separators, not a trail of node names.
4. The confirm key is named for what it does. It reads **Remove**, not **OK**.

For several nodes, the question is *Remove these N items?*. It shows no counts
and no path.

## Main flow

1. The tester selects one or more removable nodes.
2. The tester presses `Delete`, or chooses **Actions → Remove**.
3. The **Confirm Removing** dialog opens. For one node it asks *Remove
   '\<name\>'?*. It then says how many test sets, test cases and test runs the
   node holds, and shows where it is after *From:*. For several nodes it asks
   *Remove these N items?*, where N is how many.
4. The tester presses `Enter`.
5. Testin closes each node's editor and keeps a copy for undo.
6. The node goes to the desktop's recycle bin, and its automation code goes with
   it. On a desktop with no recycle bin the node is deleted outright.
7. The tree rebuilds. Testin shows *Removed*, or *Removed N* for several. The
   count is only what actually went.

## What Testin refuses

**If the tester presses `Escape`** — the dialog closes and nothing is removed.

**If only Test Cases or Test Runs is selected** — **Remove** is gray, and
`Delete` does nothing.

**A completed or closed test run is not refused.** It refuses a rename, a new
number and a drag, and it can still be removed — see Rule-TREE-PANEL-094 for why
those are different questions.

**If a container is selected together with a test set** — only the test set is
removed. The container is left out, and not counted.

**If nothing could be removed** — the tree rebuilds and Testin says nothing at
all.

**If a node cannot be deleted on disk** — an IDE notification titled *Delete
Failed* stays in the notification log, reading *Could not delete file:* and the
reason.

**If a node's copy could not be kept aside** — the node is still removed, and a
message titled *Cannot Be Undone* says so at that moment. It names how many
could not be copied aside.

---

[Documentation](../README.md) › [The tree panel](main.md)
