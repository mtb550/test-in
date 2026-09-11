[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-028

# UC-TREE-PANEL-028: Use the buttons at the top of the panel

> **Only one of the seven buttons has a key.** Search is **`Ctrl+Alt+F`**,
> and **`Cmd+Alt+F`** on a Mac.

**As a** tester, **I want** the panel's own buttons within reach of the tree,
**so that** the things I do to the whole panel are not hidden in a menu
somewhere else.

These seven buttons act on the whole panel, not on one node.

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
- **Rule-TREE-PANEL-089** — **Select Test Project** and **New Test Project** are
  gray until a Testin folder is set. Nothing else on the toolbar is ever gray.
- **Rule-TREE-PANEL-090** — Every button says what it does when the tester
  hovers over it.

## The toolbar

It is the row along the top of the panel, to the right of the word **Testin**.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin              [1]  [2]  [3]  [4]  [5]  [6]  [7]                     │
├────────────────────────────────────────────────────────────────────────────┤
│  [ main            v ]                                                     │
│                                                                            │
│  v Demo                                                                    │
│    v Test Cases                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

Seven buttons, always in this order. The last column is the page that says what
happens when the tester presses one.

| | Button | Hovering says | Where it is written |
|---|---|---|---|
| 1 | **Search Test Project** | *Find a test case, test set, package or run and go to it* | [UC-INTERNAL-001](../internal/globalSearch.md) |
| 2 | **Settings** | *Configure Testin settings* | Below |
| 3 | **Expand All** | *Expand all nodes* | Below |
| 4 | **Collapse All** | *Collapse all nodes* | Below |
| 5 | **Refresh** | *Re-index and reload tree* | [UC-TREE-PANEL-025](refreshTree.md) |
| 6 | **Select Test Project** | *Choose the test project this repository exercises* | [UC-TREE-PANEL-004](chooseTestProject.md) |
| 7 | **New Test Project** | *Create or Clone test project* | [UC-TREE-PANEL-002](createTestProject.md) |

**Only the search button has a key.** It is `Ctrl+Alt+F`, and `Cmd+Alt+F` on a
Mac. It works anywhere in the IDE. The button is how a tester finds out the
search exists at all. Nothing else on screen mentions it.

## Main flow

**Settings**

1. The tester presses **Settings**.
2. The IDE's settings open on the Testin page, where the Testin folder, the
   tester's name and the rest are set.

**Expand All**

1. The tester presses **Expand All**.
2. Every node opens. Retired nodes stay closed. Retired means a **Deprecated**
   test set or an **Archived** package.

**Collapse All**

1. The tester presses **Collapse All**.
2. Every row under the test project closes.
3. The test project, **Test Cases** and **Test Runs** stay visible.

## What Testin refuses

**If no Testin folder is set** — **Select Test Project** and **New Test
Project** are gray. The other five still work.

---

[Documentation](../README.md) › [The tree panel](main.md)
