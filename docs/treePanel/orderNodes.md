[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-015

# UC-TREE-PANEL-015: Order nodes among their siblings

> **No key.** On the menu: **Actions → Order**.

**As a** tester, **I want** to give a node a number that fixes its place among
its siblings, **so that** the tree reads in the order the work is done.

A smaller number sits higher in the list.

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
- **Rule-TREE-PANEL-099** — A node that is retired says which kind of retired it
  is. A deprecated test set, an archived package and a test run all draw their
  status in gray beside the name - a run always, because a cycle state is what
  the tree is read for, and everything else only when it is retired, so the word
  appears where it means something.
- **Rule-TREE-PANEL-054** — A number Testin cannot hold is refused, and the box
  stays open. It never clears the number the node already had.
- **Rule-TREE-PANEL-055** — A number is 1 or higher. Leave it empty and Testin
  sorts by date instead.
- **Rule-TREE-PANEL-056** — Two siblings may carry the same number. The older
  one comes first.
- **Rule-TREE-PANEL-057** — A retired node sorts after every live sibling. Its
  number does not change that. (Rule-TREE-PANEL-008, Rule-TREE-PANEL-010)
- **Rule-TREE-PANEL-058** — The test project and the two containers cannot be
  ordered.

## The Order dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Order                                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [edit]  1, 2, 3... or empty for date order             (1)  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    Escape Cancel                              │
└──────────────────────────────────────────────────────────────┘
```

1. **One field** — it shows the node's current number, or nothing. It accepts
   digits only, and no leading zero. Anything else is refused as it is typed. An
   empty field means date order. The cursor sits after the number, so typing
   **adds to the number**. A node numbered 3 whose tester types 5 ends up at 35.
   `Ctrl+A` selects the whole number.

## Main flow

1. The tester selects a test set, a package or a test run.
2. The tester chooses **Actions → Order**.
3. The **Order** dialog opens with one field. It shows the node's current
   number, or is empty, with the gray hint text *1, 2, 3... or empty for date
   order*.
4. The tester types a number from 1 up and presses `Enter`.
5. The node moves among its siblings, and Testin shows *Ordered*.
6. Emptying the field and pressing `Enter` removes the number. The node returns
   to date order, and Testin shows *Ordered*.

## What Testin refuses

**If the tester types a leading zero, a letter or a space** — the character does
not appear.

**If the test project or a container is selected** — **Order** is gray.
(Rule-TREE-PANEL-058)

**If several rows are selected** — **Order** stays black and orders the first of
them, saying nothing about the rest.

**If the number is too large** — nothing is saved. A message titled **Too
Large** says what the largest position is, and the dialog stays open with the
number still in it. The same happens to anything that is not a whole number.
(Rule-TREE-PANEL-055)

---

[Documentation](../README.md) › [The tree panel](main.md)
