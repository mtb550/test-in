[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-019

# UC-TREE-PANEL-019: Bring a retired node back

> **No key.** On the menu: **Actions → Activate** for a test project, or
> **Mark Active** for a test set or a package.

**As a** tester, **I want** to make a retired test set, package or test project
current again, **so that** work I put aside can be picked up without building it
again.

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
- **Rule-TREE-PANEL-066** — Bringing a node back undoes nothing but the status.
  Everything inside it is exactly as it was left.

Rule-TREE-PANEL-065 holds here too. It says a status is set on one node at a
time, and it is on [UC-TREE-PANEL-018](retireNode.md).

## Main flow

1. The tester selects exactly one retired node.
2. The tester opens **Actions**.
3. The tester chooses **Activate** for a test project, or **Mark Active** for a
   test set or a package.
4. Testin writes the status, refreshes the tree, and shows *Active*.
5. The node is no longer gray. It sorts among the live nodes again, by its own
   number. (Rule-TREE-PANEL-010)
6. A test set that is **Active** again is offered when a test run is created.

An **Archived** test project cannot be brought back from the tree, because the
tree does not open it. The panel offers it in the list of test projects, and
choosing it is [UC-TREE-PANEL-004](chooseTestProject.md).

## What Testin refuses

**If the node is already Active** — the entry is gray. (Rule-TREE-PANEL-065)

**If the status could not be written** — Testin says it could not, and nothing
changes. The messages are on [UC-TREE-PANEL-018](retireNode.md).

---

[Documentation](../README.md) › [The tree panel](main.md)
