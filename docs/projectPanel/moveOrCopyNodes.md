[Documentation](../README.md) › [The project panel](main.md) › UC-009

# UC-009: Move or copy nodes

> **`Ctrl+C`** copies, **`Ctrl+X`** cuts, **`Ctrl+V`** pastes, and
> **`Escape`** takes the gray off. Dragging works too. On the menu:
> **Actions → Copy**, **Cut** and **Paste**.

**As a** tester, **I want** to move a node into another package, or copy it,
by cutting and pasting or by dragging, **so that** the tree can be reorganized
without recreating anything.

## Rules

- **Rule 37** — A node lands only where its kind belongs. A test set or a test
  set package lands under **Test Cases** or under a test set package. A test run
  or a test run package lands under **Test Runs** or under a test run package.
  (rule 3)
- **Rule 38** — Nothing can be pasted or dropped into a test set or into a test
  run. Neither of them holds nodes.
- **Rule 39** — A node cannot land on itself, inside itself, or in the folder it
  already sits in.
- **Rule 40** — Dragging moves a node. Dragging with the copy key held copies
  it. Cut then paste moves. Copy then paste copies.
- **Rule 41** — A copied test case is a new test case, with its own id. Editing
  the copy never changes the original.
- **Rule 42** — A move can be undone. A copy cannot. To take back a copy, remove
  it.
- **Rule 43** — Nodes drop onto a node, never between two. Position is set by
  ordering, in [UC-010](orderNodes.md), not by dragging.
- **Rule 76** — Canceling a cut empties the clipboard. Nothing is left waiting
  to be pasted.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Paste, Move and Copy dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Paste                                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Move 'Login' into 'Smoke'?                             (1)  │
│                                                              │
│  From:  Demo > Test Cases > Accounts                    (2)  │
│  To:    Demo > Test Cases > Smoke                            │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Move    Escape Cancel                            (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — says the verb and the names. The verb is *Move* after a
   cut. It is *Copy* after a copy.
2. **From** and **To** — in gray. They let the tester catch a wrong drop
   before pressing `Enter`.
3. The confirm key is named for the verb.

From a paste, the title is **Paste**. From a drop, the title is the verb:
**Move** or **Copy**. The dialog is the same otherwise.

## Main flow

**Cut or copy, then paste**

1. The tester selects one or more test sets, packages or test runs.
2. The tester presses `Ctrl+C` to copy, or `Ctrl+X` to cut. The menu offers
   **Actions → Copy** and **Actions → Cut**.
3. After a copy, nothing in the tree changes, and Testin shows *Copied*, or
   *Copied N* for several. After a cut, the nodes are drawn gray, and Testin
   shows *Cut*, or *Cut N*.
4. The tester selects a folder that can hold them: **Test Cases**, **Test
   Runs**, or a package of the right family.
5. The tester presses `Ctrl+V`, or chooses **Actions → Paste**.
6. The **Paste** dialog opens with *From* and *To* rows. After a cut it asks
   *Move '\<name\>' into '\<folder\>'?*. After a copy it asks *Copy N items
   into '\<folder\>'?*.
7. The tester presses `Enter`. After a cut the nodes move, and Testin shows
   *Moved*, or *Moved N*. After a copy they are duplicated, and Testin shows
   *Pasted*, or *Pasted N*.
8. A pasted copy of a test set gets a new id for every test case in it, and the
   copy is selected in the tree.

**Drag and drop**

1. The tester drags one or more nodes. A small rounded label follows the mouse,
   reading *'\<name\>'* or *N items*.
2. Over a row that cannot take them, the mouse shows the no-entry pointer, and
   the row does not light up.
3. The tester releases on a folder that can hold them. The **Move** dialog asks
   the same question as a paste, and `Enter` moves them.
4. Holding `Ctrl` while releasing titles the dialog **Copy**, and `Enter` copies
   them instead.
5. Nothing can be dropped between two rows.

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

> **After `Escape`, the nodes stay on the clipboard.** A later `Ctrl+V` still
> offers to move them. This breaks rule 76, and is difference 6 on
> [the project panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The project panel](main.md)
