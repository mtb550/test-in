[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-013

# UC-TREE-PANEL-013: Move nodes

> **`Ctrl+X`** cuts, **`Ctrl+V`** pastes, and **`Escape`** takes the gray off.
> Dragging a node onto a folder moves it too. On the menu: **Actions → Cut** and
> **Paste**.

**As a** tester, **I want** to move a node into another folder, **so that** the
tree can be reorganized without building anything again.

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
- **Rule-TREE-PANEL-043** — A node lands only where its kind belongs. A test set
  or a test set package lands under **Test Cases** or under a test set package.
  A test run or a test run package lands under **Test Runs** or under a test run
  package. (Rule-TREE-PANEL-003)
- **Rule-TREE-PANEL-044** — Nothing can be pasted or dropped into a test set or
  into a test run. Neither of them holds nodes.
- **Rule-TREE-PANEL-045** — A node cannot land on itself, inside itself, or in
  the folder it already sits in.
- **Rule-TREE-PANEL-046** — Cut then paste moves. Dragging moves. Dragging with
  the copy key held copies instead, which is [UC-TREE-PANEL-014](copyNodes.md).
- **Rule-TREE-PANEL-047** — A move can be undone.
- **Rule-TREE-PANEL-048** — Moving a test set or a test set package moves its
  automation code with it, so the test cases stay runnable.
- **Rule-TREE-PANEL-049** — Nodes drop onto a node, never between two. Position
  is set by ordering, in [UC-TREE-PANEL-015](orderNodes.md), not by dragging.
- **Rule-TREE-PANEL-050** — Canceling a cut empties the clipboard. Nothing is
  left waiting to be pasted.

## The Paste, Move and Copy dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Paste                                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Move 'Login' into 'Smoke'?                             (1)  │
│                                                              │
│  From:  C:\Testin\Demo\Test Cases\Accounts             (2)   │
│  To:    C:\Testin\Demo\Test Cases\Smoke                      │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Move    Escape Cancel                            (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — says the verb and the names. The verb is *Move* after a
   cut. It is *Copy* after a copy.
2. **From** and **To** — in gray. They are full paths on disk, not trails of
   node names, and they let the tester catch a wrong drop before pressing
   `Enter`. With nodes cut from several folders, **From** names only the first
   one's folder.
3. The confirm key is named for the verb.

From a paste, the title is **Paste**. From a drop, the title is the verb:
**Move** or **Copy**. The dialog is the same otherwise.

## Main flow

**Cut and paste**

1. The tester selects one or more test sets, packages or test runs.
2. The tester presses `Ctrl+X`, or chooses **Actions → Cut**. The nodes are
   drawn gray, and Testin shows *Cut*, or *Cut N* for several.
3. The tester selects a folder that can hold them: **Test Cases**, **Test
   Runs**, or a package of the right family.
4. The tester presses `Ctrl+V`, or chooses **Actions → Paste**.
5. The **Paste** dialog asks *Move '\<name\>' into '\<folder\>'?*, with *From*
   and *To* rows.
6. The tester presses `Enter`. The nodes move, and Testin shows *Moved*, or
   *Moved N*.

**Drag and drop**

1. The tester drags one or more nodes. A small rounded label follows the mouse,
   reading *'\<name\>'* or *N items*.
2. Over a row that cannot take them, the mouse shows the no-entry pointer, and
   the row does not light up.
3. The tester releases on a folder that can hold them. The **Move** dialog asks
   the same question, and `Enter` moves them.
4. Nothing can be dropped between two rows.

**Canceling a cut**

1. The tester presses `Escape` in the tree.
2. The gray comes off the nodes.

## What Testin refuses

**If the selected node cannot take what is on the clipboard** — nothing moves,
and *Select a folder* is shown in red. That covers a test set or a test run, the
node being pasted or something inside it, the folder it already sits in, a node
of the other family, and a node in another test project. On a test project or a
test set, **Paste** is gray already, so nothing happens at all.

**If the destination already holds a node with the same name** — that node stays
where it is, and *'\<name\>' already exists in '\<folder\>'* is shown in red,
or *N items already exist in '\<folder\>'*. The other nodes in the same paste
still move.

**If nodes were cut in one test project and the code project was then bound to
another** — *Select a folder* is shown, and nothing moves.

**If the move fails on disk** — an IDE notification titled *Move Failed* stays
in the notification log, reading *Operation failed:* and the reason, or *Could
not find source or target path on disk.*

**If the test project or a container is selected** — **Copy** and **Cut** are
gray. Neither can be moved or copied.

**If several rows are selected** — **Paste** stays black and pastes into the
first of them.

**If the Java plugin is not installed** — the first move in the project shows
*Java Plugin Not Available*. The move still happens.

> **A paste does not empty the clipboard.** After moving nodes, **Paste** stays
> live, and `Ctrl+V` on another folder offers to move the same nodes again.

> **After `Escape`, the nodes stay on the clipboard.** A later `Ctrl+V` still
> offers to move them. This breaks Rule-TREE-PANEL-050, and is difference 6 on
> [the tree panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The tree panel](main.md)
