[Documentation](../README.md) › [Business requirements](business-requirements.md) › Project panel

# Project panel — business requirements

The panel is the tree on the left of the IDE. It holds every test project, test
set and test run. Everything in Testin starts here.

| | |
|---|---|
| **Area** | [Business requirements](business-requirements.md) |
| **Part of Testin** | The project panel, the tree on the left |
| **Numbering** | Rules are numbered 1 to 76, and use cases 1 to 20. They belong to the project panel. Every other part of Testin starts its own numbers again |
| **Answers** | What the panel is for, what a tester can do in it, and the rules that always hold |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every rule read from the code that enforces it |
| **Read with** | [System requirements](../system-requirements/project-panel.md) — the same use cases, step by step, with every key · [Design](../design/project-panel.md) — every screen |
| **Written to** | [How a document is written](../standard.md). No keys here. Keys are in the system requirements |

---

## What the panel is for

A tester's work is arranged like a tree:

- a test project holds test sets
- a test set holds test cases
- a test run records one pass through some of those test cases

The panel is that tree. It sits on the left, where the IDE keeps every other
tree. It is always one click away.

**Three words, before the rules use them.**

- A **node** is anything the tree shows: a test project, a folder, a test set or
  a test run. A test case is not a node, because the tree does not show one.
- **Bound** means this code project is set to use one test project. The choice
  is written into the code project, so everyone who opens it gets the same test
  project.
- **Signed off** means a test run is **Completed** or **Closed**. Nothing in it
  can change again.

The panel's job is small and strict. It shows one test project, the one this
code project is bound to. It lets the tester shape that test project: create,
name, group, order, retire and remove the things in it.

Reading and executing test cases happens in editors. The panel opens those
editors. The panel itself never shows a test case.

---

## Rules that hold everywhere in the panel

These apply to every use case below. A use case points at them by number. It
does not repeat them.

- **Rule 1** — The panel shows exactly one test project. It is the one this
  repository is bound to. There is no list of test projects in the tree.
- **Rule 2** — **Test Cases** and **Test Runs** are fixed containers. They
  cannot be created, renamed, moved, copied or removed. They come with the test
  project and go with it.
- **Rule 3** — The tree has two sides, and nothing moves between them. Test sets
  and test set packages live under **Test Cases**. Test runs and test run
  packages live under **Test Runs**.
- **Rule 4** — Two nodes under one parent cannot share a name. This holds
  whether the node is created, renamed, pasted or dropped.
- **Rule 5** — A node name is never empty.
- **Rule 6** — Removing, moving or copying asks first. Nothing in the tree
  changes until the tester confirms.
- **Rule 7** — When something changes, Testin says so once, in the past tense.
  The tester
  sees *Created*, *Renamed* or *Removed*. Several changes at once confirm once,
  with a count: the tester sees *Removed 4*, never four messages. Looking at
  something confirms nothing.
- **Rule 8** — A retired node keeps everything inside it. Retired means a
  **Deprecated** test set or an **Archived** package. It is drawn gray. It sorts
  after live nodes. It is not offered when a test run is created.
- **Rule 9** — A test run that is **Completed** or **Closed** is signed off.
  Its test cases, verdicts and configuration can no longer change.
- **Rule 10** — Siblings are shown in one order:
  1. live nodes, then retired ones
  2. the number the tester gave the node
  3. the date the node was created
  4. the name

  A node with no number comes after every node with one.
- **Rule 11** — A removed node goes to the desktop's recycle bin. It can be put
  back from the tree.
- **Rule 12** — A test case is not a node in this tree. It is reached by opening
  its test set.
- **Rule 13** — Nodes move only within one test project. Nothing cut in one test
  project can be pasted into another.

---

## Use cases

### Getting to the tree

#### Use case 1 · Open the panel and reach the tree

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

- **Rule 14** — The panel never opens on its own when the IDE starts.
- **Rule 15** — A code project does not have to name a test project. If it names
  none, and exactly one test project exists in the Testin folder, Testin binds it
  to that one without asking.

#### Use case 2 · Create a test project

**As a** tester, **I want** to create a test project by name, or clone one from
a Git address, **so that** a new product under test has a place before any test
is written.

**Rules**

- **Rule 16** — A test project is a folder directly under the Testin folder.
  Any other folder there is ignored.
- **Rule 17** — Creating a test project binds this repository to it.
- **Rule 18** — Cloning needs two things. It needs the Git plugin. It also needs
  this repository to already name the test project it is cloning.

#### Use case 3 · Choose which test project this repository uses

**As a** tester, **I want** to point this repository at a different test project
under the same Testin folder, **so that** one machine can serve several products.

**Rules**

- **Rule 19** — The binding is written into the repository. A colleague who
  clones the repository gets the same test project, with no setup.
- **Rule 20** — If the binding cannot be written, Testin says so. It never
  reports the choice as saved.

---

### Nodes

#### Use case 4 · Open a test set or a test run

**As a** tester, **I want** to open a test set or a test run from the tree,
**so that** I can read and work its test cases.

**Rules**

- **Rule 21** — Only a test set and a test run open in an editor. A package has
  nothing to open. A container has nothing to open. Neither of them says
  anything.
- **Rule 22** — The tester opens a node that is already open. Its tab comes
  forward. It does not open a second time.

#### Use case 5 · Create a test set or a test set package

**As a** tester, **I want** to add a test set, or a package to group test sets,
under **Test Cases** or under another package, **so that** the tree grows the way
the product is organized.

**Rules**

- **Rule 23** — Under **Test Cases** or a test set package, only a test set or a
  test set package can be created. The dialog offers nothing else.
- **Rule 24** — Nothing can be created directly under the test project, under a
  test set, or under a test run.
- **Rule 25** — A new test set opens in its editor at once. Its automation class
  is generated where the Java plugin allows it. A new package does neither.

#### Use case 6 · Create a test run or a test run package

**As a** tester, **I want** to start a test run over the test cases I choose,
**so that** a pass through the product is recorded on its own.

**Rules**

- **Rule 26** — Under **Test Runs** or a test run package, only a test run or a
  test run package can be created.
- **Rule 27** — A test run needs at least one test case. It cannot be created
  empty.
- **Rule 28** — Three things are not offered when a test run is created: a
  retired test set, anything under an **Archived** package, and an empty test
  set. (rule 8)
- **Rule 29** — A new test run starts as **Created**. Every test case in it
  starts **Pending**.

#### Use case 7 · Rename a node

**As a** tester, **I want** to rename a test set, a package or a test run,
**so that** the tree says what things are called now.

**Rules**

- **Rule 30** — The test project and the two containers cannot be renamed from
  the tree.
- **Rule 31** — Renaming a test set or a test set package renames its automation
  code with it. The test case stays runnable.
- **Rule 32** — A rename can be undone.

#### Use case 8 · Remove a node

**As a** tester, **I want** to remove a test set, a package, a test run or a
whole test project, **so that** the tree holds only what is current.

**Rules**

- **Rule 33** — The confirmation says what will go. For one node, it says what
  the node holds and where it is. For several nodes, it says how many.
- **Rule 34** — Removing a test set, a test set package or a test project
  removes its automation code with it.
- **Rule 35** — A removal can be undone. (rule 11)
- **Rule 36** — The two containers are never removed. This holds even when they
  are selected together with nodes that are removed.

---

### Arranging the tree

#### Use case 9 · Move or copy nodes

**As a** tester, **I want** to move a node into another package, or copy it,
by cutting and pasting or by dragging, **so that** the tree can be reorganized
without recreating anything.

**Rules**

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
- **Rule 41** — A copied test case is a new test case, with a new identity.
  Editing the copy never changes the original.
- **Rule 42** — A move can be undone. A copy cannot. To take back a copy, remove
  it.
- **Rule 43** — Nodes drop onto a node, never between two. Position is set by
  ordering (use case 10), not by dragging.
- **Rule 76** — Canceling a cut empties the clipboard. Nothing is left waiting
  to be pasted.

#### Use case 10 · Order nodes among their siblings

**As a** tester, **I want** to give a node a number that fixes its place among
its siblings, **so that** the tree reads in the order the work is done.

**Rules**

- **Rule 44** — A number is 1 or higher. Leave it empty and Testin sorts by date
  instead.
- **Rule 45** — Two siblings may carry the same number. The older one comes
  first.
- **Rule 46** — A retired node sorts after every live sibling. Its number does
  not change that. (rule 8, rule 10)
- **Rule 47** — The test project and the two containers cannot be ordered.

#### Use case 11 · Undo and redo a change to the tree

**As a** tester, **I want** to take back the last change I made to the tree,
**so that** a wrong move, rename or removal costs nothing.

**Rules**

- **Rule 48** — The tree remembers its own last 20 changes. It remembers them
  separately from any editor.
- **Rule 49** — Four things can be undone: a move, a rename, a removal, and an
  edit of a test run. Three cannot: an order number, a copy, and a status change.
- **Rule 50** — Making a new change forgets everything that was undone.

---

### Statuses

#### Use case 12 · Retire and reactivate

**As a** tester, **I want** to mark a test set **Deprecated**, a package
**Archived**, or a whole test project **Inactive** or **Archived**, **so that**
old work stays for its history without getting in the way of current work.

**Rules**

- **Rule 51** — Retiring deletes nothing. (rule 8)
- **Rule 52** — A test project that is not **Active** shows nothing under it.
- **Rule 53** — An **Archived** test project is not opened at all on the next
  load. The panel says so, and offers the other test projects.
- **Rule 54** — A status is set on one node at a time. The status a node already
  has is not offered.

#### Use case 13 · Set a test run's status

**As a** tester, **I want** to mark a test run **Assigned**, **Completed** or
**Closed** from the tree, **so that** the test run's place in its life is visible
without opening it.

**Rules**

- **Rule 55** — **Completed** and **Closed** are final. The test run accepts no
  more verdicts. Every test case still **Pending** becomes **Untested**.
  (rule 9)
- **Rule 56** — A tester sets **Assigned**, **Completed** and **Closed**.
  **Created** and **In Progress** are the test run's own record of itself.

**Not decided** — see question 1 and question 2.

#### Use case 14 · Re-create a test run

**As a** tester, **I want** to make the next cycle from a finished test run, with
the same test cases and settings and no verdicts, **so that** starting the next
round of testing takes one step, instead of building the whole test run again by
hand.

**Rules**

- **Rule 57** — Re-create works on a test run in any status, including a
  signed-off one. That is what it is for.
- **Rule 58** — Only the test cases and the configuration are carried over.
  Verdicts, durations and failure details start fresh.
- **Rule 59** — The next name is suggested by counting up. *cycle-1* becomes
  *cycle-2*. A name already taken is skipped.

#### Use case 15 · Edit a test run

**As a** tester, **I want** to change which test cases a test run covers, its
name and its configuration, **so that** a test run can be corrected without being
recreated.

**Rules**

- **Rule 60** — A signed-off test run cannot be edited. (rule 9)
- **Rule 61** — Removing a test case from a test run drops everything that test
  case recorded in that test run. Adding a test case adds it as **Pending**.
- **Rule 62** — An edit can be undone, as one step. (rule 49)

---

### Working from the tree

#### Use case 16 · Run the automation for everything a node holds

**As a** tester, **I want** to run every automated test case under a test set, a
package or **Test Cases**, **so that** a whole area runs in one gesture.

**Rules**

- **Rule 63** — Running from a parent skips retired branches. Running a retired
  test set directly still runs it. (rule 8)
- **Rule 64** — A node with no test cases to run says so. It runs nothing.
- **Rule 65** — Running needs the automation plugin. Without it, the item is
  not offered.

#### Use case 17 · Find anything in the project

**As a** tester, **I want** to type part of a name, a step or an id and jump to
it, **so that** a large tree is never a place to scroll.

**Rules**

- **Rule 66** — Nodes are found by name, from the first character. Test cases
  are searched from the second character, in every field they have.
- **Rule 67** — Choosing a result opens the tree at it. It opens its editor too.

#### Use case 18 · Refresh the tree from disk

**As a** tester, **I want** to reload the tree after something changed outside
the IDE, **so that** the tree shows what is on disk. A pull, a sync or a hand
edit all change the tree from outside.

**Rules**

- **Rule 68** — Refresh re-reads the repository's binding first. So a test
  project changed by hand, or changed by a branch switch, is picked up.
- **Rule 69** — Editors on a node that no longer exists are closed. The other
  editors are reloaded, unless they are busy.
- **Rule 70** — Only one refresh runs at a time. A second request while one is
  running is ignored.

#### Use case 19 · Switch the Git branch of the test project

**As a** tester, **I want** to switch the test project's branch from the panel,
**so that** the tree follows the branch I am testing.

**Rules**

- **Rule 71** — The branch box appears only for a test project shared through
  Git.
- **Rule 72** — Switching with uncommitted changes asks first. Switching never
  loses them.
- **Rule 73** — A switch that succeeds reloads the tree from the new branch.

#### Use case 20 · See what a node holds

**As a** tester, **I want** to see a node's counts, dates, status and verdict
breakdown without opening anything, **so that** I can size a branch of the tree
at a glance.

**Rules**

- **Rule 74** — Opening **Details** changes nothing, so Testin says nothing.
  (rule 7)
- **Rule 75** — Testin counts what a node holds when the tester asks. It never
  saves the number.

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
this document. None of them has a bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | Rule 7 — one past-tense word | Creating a test run says *Run created*. Creating a test project says *Project created* or *Project cloned*. Every other creation says *Created*. |
| **Difference 2** | Rule 38 — nothing lands in a test run | **Paste** is offered on a test run. It always refuses, with *Select a folder*. It should be grayed, as it is on a test set. |
| **Difference 3** | Rule 9 — a signed-off test run does not change | A **Completed** or **Closed** test run can still be renamed, moved, reordered and removed from the tree. Only editing, running and its status are blocked. |
| **Difference 4** | Rule 23, rule 26 — the dialog offers the kinds | The create dialog's two rows show no kind name. They show only their hints, *Holds test cases* and *Groups test sets*. Someone removed them while tidying up the labels, and nobody noticed. |
| **Difference 5** | Rule 56 — Created and In Progress are the test run's own | The status popup offers **Created** and **In Progress** as choices. It also lets a test run go backwards, from **Assigned** to **Created**. |
| **Difference 6** | Rule 76 — canceling a cut empties the clipboard | The tester cuts nodes, then presses the key that clears the gray. The nodes stay on the clipboard. The next paste still moves them, though the tester canceled the cut. |

---

## Not decided

| | Question | Why it is open |
|---|---|---|
| **Question 1** | May a test run go backwards, from **Assigned** to **Created**, or from **In Progress** to **Created**? | Nothing prevents it today (difference 5). The same question for the whole product is [question 1 in the product's own document](product.md#9-undecided). |
| **Question 2** | Should a signed-off test run be locked in the tree, as well as in its editor? | Today it is not (difference 3). Its own description says its name must not change. The tree's rename does not check. |
| **Question 3** | What should the paste refusal say? | *Select a folder* is shown for six different reasons. Four of them are: the wrong side of the tree, the node itself, another test project, and a test run. Choosing a different folder only fixes one of the six. |
| **Question 4** | On a Mac, should cut, paste, undo and redo in the tree use the Mac's own key for those actions, the way copy already does? | Today copy does. The other four do not. The [system requirements](../system-requirements/project-panel.md) name the keys. |

---

[Documentation](../README.md) › [Business requirements](business-requirements.md) › **Project panel** — read with the [system requirements](../system-requirements/project-panel.md) and the [design](../design/project-panel.md)
