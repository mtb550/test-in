[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-017

# UC-TREE-PANEL-017: Redo a change to the tree

> **`Ctrl+Y`**. On the menu: **Actions → Redo**, which names what it will put
> back.

**As a** tester, **I want** to put back a change I undid, **so that** changing
my mind twice costs no more than changing it once.

Redo is the opposite of undo.

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
- **Rule-TREE-PANEL-061** — Making a new change forgets everything that was
  undone.

Rule-TREE-PANEL-059 and Rule-TREE-PANEL-060 hold here too. They say what the
tree remembers and what can be taken back, and they are on
[UC-TREE-PANEL-016](undoChange.md).

## What the tester sees

Redo opens no screen of its own. On the **Actions** menu, the entry names what
it will put back. After `Ctrl+Y` the tree redraws with the change applied again,
and *Redone* shows above the status bar at the bottom right of the IDE.

## Main flow

1. Something was just undone.
2. The tester presses `Ctrl+Y`, or chooses **Actions → Redo \<what\>**.
3. Testin applies the change again, and shows *Redone*.

## What Testin refuses

**If a new change was made after the undo** — **Redo** is gray. The undone
change is forgotten. (Rule-TREE-PANEL-061)

**If nothing has been undone** — **Redo** is gray.

---

[Documentation](../README.md) › [The tree panel](main.md)
