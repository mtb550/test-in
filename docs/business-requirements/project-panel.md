[Documentation](../README.md) › [Business requirements](business-requirements.md) › Project panel

# Project panel — business requirements

The panel is the tree on the left of the IDE. It holds every test project, test
set and test run. Everything in Testin starts here.

| | |
|---|---|
| **Area** | [Business requirements](business-requirements.md) |
| **Part of Testin** | The project panel, the tree on the left |
| **Numbering** | Its rules are numbered `BR-PP-01` and up, and its use cases `UC-PP-01` and up. The `PP` is short for the project panel, so a number from this document can never be confused with one from another |
| **Answers** | What the panel is for, what a tester can do in it, and the rules that always hold |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every rule read from the code that enforces it |
| **Read with** | [System requirements](../system-requirements/project-panel.md) — the same use cases, step by step, with every key · [Design](../design/project-panel.md) — every screen |
| **Written to** | [How a document is written](../standard.md). No keys here. Keys are in the system requirements |

---

## What the panel is for

A tester's work is a tree:

- a test project holds test sets
- a test set holds test cases
- a test run records one pass through some of those test cases

The panel is that tree. It sits on the left, where the IDE keeps every other
tree. It is always one click away.

Its job is small and strict. The panel shows one test project. It is the one
this repository is bound to. The panel lets the tester shape that test project:
create, name, group, order, retire and remove the things in it.

Reading and executing test cases happens in editors. The panel opens those
editors. The panel itself never shows a test case.

---

## Rules that hold everywhere in the panel

These apply to every use case below. A use case cites them. It does not repeat
them.

- **BR-PP-01** — The panel shows exactly one test project. It is the one this
  repository is bound to. There is no list of test projects in the tree.
- **BR-PP-02** — **Test Cases** and **Test Runs** are fixed containers. They
  cannot be created, renamed, moved, copied or removed. They come with the test
  project and go with it.
- **BR-PP-03** — A node has a family and never leaves it. Test sets and test set
  packages live under **Test Cases**. Test runs and test run packages live under
  **Test Runs**. Nothing crosses.
- **BR-PP-04** — Two nodes under one parent cannot share a name. This holds
  whether the node is created, renamed, pasted or dropped.
- **BR-PP-05** — A node name is never empty.
- **BR-PP-06** — Removing, moving or copying asks first. Nothing in the tree
  changes until the tester confirms.
- **BR-PP-07** — A change confirms itself once, in the past tense. The tester
  sees *Created*, *Renamed* or *Removed*. Several changes at once confirm once,
  with a count: the tester sees *Removed 4*, never four messages. Looking at
  something confirms nothing.
- **BR-PP-08** — A retired node keeps everything inside it. Retired means a
  **Deprecated** test set or an **Archived** package. It is drawn gray. It sorts
  after live nodes. It is not offered when a test run is created.
- **BR-PP-09** — A test run that is **Completed** or **Closed** is signed off.
  Its test cases, verdicts and configuration can no longer change.
- **BR-PP-10** — Siblings are shown in one order:
  1. live nodes, then retired ones
  2. the number the tester gave the node
  3. the date the node was created
  4. the name

  A node with no number comes after every node with one.
- **BR-PP-11** — A removed node goes to the desktop's recycle bin. It can be put
  back from the tree.
- **BR-PP-12** — A test case is not a node in this tree. It is reached by opening
  its test set.
- **BR-PP-13** — Nodes move only within one test project. Nothing cut in one test
  project can be pasted into another.

---

## Use cases

### Getting to the tree

#### UC-PP-01 · Open the panel and reach the tree

**As a** tester, **I want** the panel to show my tree, or tell me what to do
first, **so that** I am never stuck on an empty window.

The panel shows one of five things. It checks in this order:

1. No Testin folder is set. The panel shows a link to the settings.
2. The test project is found. The panel shows the tree.
3. The test project is missing, and it can be cloned. The panel shows a link to
   clone it.
4. There is no test project yet. The panel shows a link to create the first one.
5. Otherwise, the panel shows a list of test projects to choose from.

**Rules**

- **BR-PP-14** — The panel never opens on its own when the IDE starts.
- **BR-PP-15** — A repository can name no test project. If exactly one test
  project exists under the Testin folder, the repository is bound to it without
  asking.

#### UC-PP-02 · Create a test project

**As a** tester, **I want** to create a test project by name, or clone one from
a Git address, **so that** a new product under test has a place before any test
is written.

**Rules**

- **BR-PP-16** — A test project is a folder directly under the Testin folder.
  Any other folder there is ignored.
- **BR-PP-17** — Creating a test project binds this repository to it.
- **BR-PP-18** — Cloning needs two things. It needs the Git plugin. It also needs
  this repository to already name the test project it is cloning.

#### UC-PP-03 · Choose which test project this repository uses

**As a** tester, **I want** to point this repository at a different test project
under the same Testin folder, **so that** one machine can serve several products.

**Rules**

- **BR-PP-19** — The binding is written into the repository. A colleague who
  clones the repository gets the same test project, with no setup.
- **BR-PP-20** — If the binding cannot be written, nothing announces that it was
  done.

---

### Nodes

#### UC-PP-04 · Open a test set or a test run

**As a** tester, **I want** to open a test set or a test run from the tree,
**so that** I can read and work its test cases.

**Rules**

- **BR-PP-21** — Only a test set and a test run open in an editor. A package has
  nothing to open. A container has nothing to open. Neither of them says
  anything.
- **BR-PP-22** — The tester opens a node that is already open. Its tab comes
  forward. It does not open a second time.

#### UC-PP-05 · Create a test set or a test set package

**As a** tester, **I want** to add a test set, or a package to group test sets,
under **Test Cases** or under another package, **so that** the tree grows the way
the product is organized.

**Rules**

- **BR-PP-23** — Under **Test Cases** or a test set package, only a test set or a
  test set package can be created. The dialog offers nothing else.
- **BR-PP-24** — Nothing can be created directly under the test project, under a
  test set, or under a test run.
- **BR-PP-25** — A new test set opens in its editor at once. Its automation class
  is generated where the Java plugin allows it. A new package does neither.

#### UC-PP-06 · Create a test run or a test run package

**As a** tester, **I want** to start a test run over the test cases I choose,
**so that** a pass through the product is recorded on its own.

**Rules**

- **BR-PP-26** — Under **Test Runs** or a test run package, only a test run or a
  test run package can be created.
- **BR-PP-27** — A test run needs at least one test case. It cannot be created
  empty.
- **BR-PP-28** — Three things are not offered when a test run is created: a
  retired test set, anything under an **Archived** package, and an empty test
  set. (BR-PP-08)
- **BR-PP-29** — A new test run starts as **Created**. Every test case in it
  starts **Pending**.

#### UC-PP-07 · Rename a node

**As a** tester, **I want** to rename a test set, a package or a test run,
**so that** the tree says what things are called now.

**Rules**

- **BR-PP-30** — The test project and the two containers cannot be renamed from
  the tree.
- **BR-PP-31** — Renaming a test set or a test set package renames its automation
  code with it. The test case stays runnable.
- **BR-PP-32** — A rename can be undone.

#### UC-PP-08 · Remove a node

**As a** tester, **I want** to remove a test set, a package, a test run or a
whole test project, **so that** the tree holds only what is current.

**Rules**

- **BR-PP-33** — The confirmation says what will go. For one node, it says what
  the node holds and where it is. For several nodes, it says how many.
- **BR-PP-34** — Removing a test set, a test set package or a test project
  removes its automation code with it.
- **BR-PP-35** — A removal can be undone. (BR-PP-11)
- **BR-PP-36** — The two containers are never removed. This holds even when they
  are selected together with nodes that are removed.

---

### Arranging the tree

#### UC-PP-09 · Move or copy nodes

**As a** tester, **I want** to move a node into another package, or copy it,
by cutting and pasting or by dragging, **so that** the tree can be reorganized
without recreating anything.

**Rules**

- **BR-PP-37** — A node lands only where its kind belongs. A test set or a test
  set package lands under **Test Cases** or under a test set package. A test run
  or a test run package lands under **Test Runs** or under a test run package.
  (BR-PP-03)
- **BR-PP-38** — Nothing can be pasted or dropped into a test set or into a test
  run. Neither of them holds nodes.
- **BR-PP-39** — A node cannot land on itself, inside itself, or in the folder it
  already sits in.
- **BR-PP-40** — Dragging moves a node. Dragging with the copy key held copies
  it. Cut then paste moves. Copy then paste copies.
- **BR-PP-41** — A copied test case is a new test case, with a new identity.
  Editing the copy never changes the original.
- **BR-PP-42** — A move can be undone. A copy cannot. To take back a copy, remove
  it.
- **BR-PP-43** — Nodes drop onto a node, never between two. Position is set by
  ordering (UC-PP-10), not by dragging.

#### UC-PP-10 · Order nodes among their siblings

**As a** tester, **I want** to give a node a number that fixes its place among
its siblings, **so that** the tree reads in the order the work is done.

**Rules**

- **BR-PP-44** — A number is 1 or higher. An empty number means date order.
- **BR-PP-45** — Two siblings may carry the same number. The older one comes
  first.
- **BR-PP-46** — A retired node sorts after every live sibling. Its number does
  not change that. (BR-PP-08, BR-PP-10)
- **BR-PP-47** — The test project and the two containers cannot be ordered.

#### UC-PP-11 · Undo and redo a change to the tree

**As a** tester, **I want** to take back the last change I made to the tree,
**so that** a wrong move, rename or removal costs nothing.

**Rules**

- **BR-PP-48** — The tree remembers its own last 20 changes. It remembers them
  separately from any editor.
- **BR-PP-49** — Four things can be undone: a move, a rename, a removal, and an
  edit of a test run. Three cannot: an order number, a copy, and a status change.
- **BR-PP-50** — Making a new change forgets everything that was undone.

---

### Statuses

#### UC-PP-12 · Retire and reactivate

**As a** tester, **I want** to mark a test set **Deprecated**, a package
**Archived**, or a whole test project **Inactive** or **Archived**, **so that**
old work stays for its history without getting in the way of current work.

**Rules**

- **BR-PP-51** — Retiring deletes nothing. (BR-PP-08)
- **BR-PP-52** — A test project that is not **Active** shows nothing under it.
- **BR-PP-53** — An **Archived** test project is not opened at all on the next
  load. The panel says so, and offers the other test projects.
- **BR-PP-54** — A status is set on one node at a time. The status a node already
  has is not offered.

#### UC-PP-13 · Set a test run's status

**As a** tester, **I want** to mark a test run **Assigned**, **Completed** or
**Closed** from the tree, **so that** the test run's place in its life is visible
without opening it.

**Rules**

- **BR-PP-55** — **Completed** and **Closed** are final. The test run accepts no
  more verdicts. Every test case still **Pending** becomes **Untested**.
  (BR-PP-09)
- **BR-PP-56** — A tester sets **Assigned**, **Completed** and **Closed**.
  **Created** and **In Progress** are the test run's own record of itself.

**Not decided** — see Q-PP-01 and Q-PP-02.

#### UC-PP-14 · Re-create a test run

**As a** tester, **I want** to make the next cycle from a finished test run, with
the same test cases, the same configuration and no verdicts, **so that** a
regression pass takes one step instead of a rebuild.

**Rules**

- **BR-PP-57** — Re-create works on a test run in any status, including a
  signed-off one. That is what it is for.
- **BR-PP-58** — Only the test cases and the configuration are carried over.
  Verdicts, durations and failure details start fresh.
- **BR-PP-59** — The next name is suggested by counting up. *cycle-1* becomes
  *cycle-2*. A name already taken is skipped.

#### UC-PP-15 · Edit a test run

**As a** tester, **I want** to change which test cases a test run covers, its
name and its configuration, **so that** a test run can be corrected without being
recreated.

**Rules**

- **BR-PP-60** — A signed-off test run cannot be edited. (BR-PP-09)
- **BR-PP-61** — Removing a test case from a test run drops everything that test
  case recorded in that test run. Adding a test case adds it as **Pending**.
- **BR-PP-62** — An edit can be undone, as one step. (BR-PP-49)

---

### Working from the tree

#### UC-PP-16 · Run the automation for everything a node holds

**As a** tester, **I want** to run every automated test case under a test set, a
package or **Test Cases**, **so that** a whole area runs in one gesture.

**Rules**

- **BR-PP-63** — Running from a parent skips retired branches. Running a retired
  test set directly still runs it. (BR-PP-08)
- **BR-PP-64** — A node with no test cases to run says so. It runs nothing.
- **BR-PP-65** — Running needs the automation plugin. Without it, the item is
  not offered.

#### UC-PP-17 · Find anything in the project

**As a** tester, **I want** to type part of a name, a step or an id and jump to
it, **so that** a large tree is never a place to scroll.

**Rules**

- **BR-PP-66** — Nodes are found by name, from the first character. Test cases
  are searched from the second character, in every field they have.
- **BR-PP-67** — Choosing a result opens the tree at it. It opens its editor too.

#### UC-PP-18 · Refresh the tree from disk

**As a** tester, **I want** to reload the tree after something changed outside
the IDE, **so that** the tree shows what is on disk. A pull, a sync or a hand
edit all change the tree from outside.

**Rules**

- **BR-PP-68** — Refresh re-reads the repository's binding first. So a test
  project changed by hand, or changed by a branch switch, is picked up.
- **BR-PP-69** — Editors on a node that no longer exists are closed. The other
  editors are reloaded, unless they are busy.
- **BR-PP-70** — Only one refresh runs at a time. A second request while one is
  running is ignored.

#### UC-PP-19 · Switch the Git branch of the test project

**As a** tester, **I want** to switch the test project's branch from the panel,
**so that** the tree follows the branch I am testing.

**Rules**

- **BR-PP-71** — The branch box appears only for a test project shared through
  Git.
- **BR-PP-72** — Switching with uncommitted changes asks first. Switching never
  loses them.
- **BR-PP-73** — A switch that succeeds reloads the tree from the new branch.

#### UC-PP-20 · See what a node holds

**As a** tester, **I want** to see a node's counts, dates, status and verdict
breakdown without opening anything, **so that** I can size a branch of the tree
at a glance.

**Rules**

- **BR-PP-74** — Details changes nothing, and confirms nothing. (BR-PP-07)
- **BR-PP-75** — Counts are counted when asked. They are never stored.

---

## Also on this menu

Six items on the tree's menu belong to another part of Testin. They are
documented there:

| Item | Belongs to |
|---|---|
| **Export**, **Import** | Reports, export, import and sync |
| **Generate Report** | The same |
| **Sync With Remote**, **View Pending Commits** | The same, the Git half |
| **Sync With SFTP** | The same, the SFTP half |

---

## Where the product breaks its own rules today

Stated, not hidden. Each one is a real gap, found while reading the code for
this document. None of them has an issue yet.

| | Rule broken | What a tester sees |
|---|---|---|
| **D1** | BR-PP-07 — one past-tense word | Creating a test run says *Run created*. Creating a test project says *Project created* or *Project cloned*. Every other creation says *Created*. |
| **D2** | BR-PP-38 — nothing lands in a test run | **Paste** is offered on a test run. It always refuses, with *Select a folder*. It should be grayed, as it is on a test set. |
| **D3** | BR-PP-09 — a signed-off test run does not change | A **Completed** or **Closed** test run can still be renamed, moved, reordered and removed from the tree. Only editing, running and its status are blocked. |
| **D4** | BR-PP-23, BR-PP-26 — the dialog offers the kinds | The create dialog's two rows show no kind name. They show only their hints, *Holds test cases* and *Groups test sets*. The names went missing in a caption sweep, and nobody noticed. |
| **D5** | BR-PP-56 — Created and In Progress are the test run's own | The status popup offers **Created** and **In Progress** as choices. It also lets a test run go backwards, from **Assigned** to **Created**. |
| **D6** | BR-PP-07 — a change confirms once | The tester cuts nodes, then presses the key that clears the gray. The nodes stay on the clipboard. The next paste still moves them, though the tester canceled the cut. |

---

## Not decided

| | Question | Why it is open |
|---|---|---|
| **Q-PP-01** | May a test run go backwards, from **Assigned** to **Created**, or from **In Progress** to **Created**? | Nothing prevents it today (D5). The same question for the whole product is [Q-01 in the product's own document](product.md#9-undecided). |
| **Q-PP-02** | Should a signed-off test run be locked in the tree, as well as in its editor? | Today it is not (D3). Its own description says its name must not change. The tree's rename does not check. |
| **Q-PP-03** | What should the paste refusal say? | *Select a folder* is shown for six different reasons. Among them: the wrong family, its own subtree, another test project, and a test run. Only one of the six is cured by selecting a different folder. |
| **Q-PP-04** | On a Mac, should the tree's cut, paste, undo and redo keys follow the platform key the way copy does? | Today copy follows it. The other four do not. |

---

[Documentation](../README.md) › [Business requirements](business-requirements.md) › **Project panel** — read with the [system requirements](../system-requirements/project-panel.md) and the [design](../design/project-panel.md)
