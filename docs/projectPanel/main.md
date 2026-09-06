[Documentation](../README.md) › The project panel

# The project panel

The panel is the tree on the left of the IDE. It holds every test project, test
set and test run. Everything in Testin starts here.

| | |
|---|---|
| **Part of Testin** | The project panel, the tree on the left |
| **Answers** | What the panel is for, what a tester can do in it, exactly what happens step by step, and what every screen looks like |
| **Numbering** | Use cases are UC-001 to UC-020. Rules are numbered 1 to 76. They belong to the project panel. Every other part of Testin starts its own numbers again |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every rule, key, label and message read from the code |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

Each one is a page of its own: the story, its rules, its screens and its steps.

| | What the tester does | |
|---|---|---|
| | **Getting to the tree** | |
| **UC-001** | [Open the panel and reach the tree](reachTheTree.md) | |
| **UC-002** | [Create a test project](createTestProject.md) | |
| **UC-003** | [Choose which test project this code project uses](chooseTestProject.md) | |
| | **Nodes** | |
| **UC-004** | [Open a test set or a test run](openNode.md) | |
| **UC-005** | [Create a test set or a test set package](createTestSet.md) | |
| **UC-006** | [Create a test run or a test run package](createTestRun.md) | |
| **UC-007** | [Rename a node](renameNode.md) | |
| **UC-008** | [Remove a node](removeNode.md) | |
| | **Arranging the tree** | |
| **UC-009** | [Move or copy nodes](moveOrCopyNodes.md) | |
| **UC-010** | [Order nodes among their siblings](orderNodes.md) | |
| **UC-011** | [Undo and redo a change to the tree](undoAndRedo.md) | |
| | **Statuses** | |
| **UC-012** | [Retire and reactivate](retireAndReactivate.md) | |
| **UC-013** | [Set a test run's status](setTestRunStatus.md) | |
| **UC-014** | [Re-create a test run](reCreateTestRun.md) | |
| **UC-015** | [Edit a test run](editTestRun.md) | |
| | **Working from the tree** | |
| **UC-016** | [Run the automation for everything a node holds](runTests.md) | |
| **UC-017** | [Find anything in the project](searchProject.md) | |
| **UC-018** | [Refresh the tree from disk](refreshTree.md) | |
| **UC-019** | [Switch the Git branch of the test project](switchBranch.md) | |
| **UC-020** | [See what a node holds](nodeDetails.md) | |

---

## Where the panel sits in the IDE

Testin puts two tool windows in IntelliJ IDEA. The project panel is docked on
the left, beside the places the IDE keeps its own trees. The view panel is
docked on the right.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin  -  IntelliJ IDEA                                       - □ ×      │
├──────────────────┬─────────────────────────────────┬───────────────────────┤
│  Testin     (1)  │  Accounts  ×  cycle-2  ×   (2)  │  Testin        (3)    │
│                  ├─────────────────────────────────┤  Details  History     │
│  [ main     v ]  │                                 │  Open Bugs            │
│                  │                                 │                       │
│  v Demo          │                                 │                       │
│    v Test Cases  │                                 │                       │
│      v Accounts  │         Editor panel            │                       │
│          Login   │                                 │                       │
│        Checkout  │                                 │                       │
│    v Test Runs   │                                 │                       │
│      v Sprint 7  │                                 │                       │
│          cycle-2 │                                 │                       │
├──────────────────┴─────────────────────────────────┴───────────────────────┤
│  Testin   Git   Terminal   Problems                             (4)        │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The project panel** — the tree. Everything on this page is about it.
2. **Editor panel** — where a test set or a test run opens when the tester
   opens one from the tree. One tab each. The project panel itself never shows a
   test case.
3. **The view panel** — details, history and open bugs for the test case the
   tester has selected. It is a part of Testin of its own, and its pages are not
   written yet.
4. **The tool window bar** — the tester clicks **Testin** here to show and hide
   a panel.

---

## What the panel is for

A tester's work is arranged like a tree:

- a test project holds test sets
- a test set holds test cases
- a test run records one pass through some of those test cases

The panel is that tree. It sits on the left, where the IDE keeps every other
tree. It is always one click away.

**Four words, before the rules use them.**

- A **node** is anything the tree shows: a test project, a folder, a test set or
  a test run. A test case is not a node, because the tree does not show one.
- The **Testin folder** is the one folder that holds every test project. The
  settings page calls it *Testin source root*, and two of Testin's own messages
  call it *the Testin root*.
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

---

---

## How to read these pages

Each use case page has the same shape. **Rules** are the things that must always
be true. **Main flow** is what happens, step by step, when nothing goes wrong.
**What Testin refuses** is every way it can go wrong, and what the tester sees
each time.

Words like *\<name\>* stand for whatever the tester is working on. Testin puts
the real name there.

**In every drawing:**

Each screen is drawn with text, so it can live beside the words that explain
it. In every drawing:

- `v` is the little triangle that opens and closes a row
- `+` `@` `*` are the status icons a test run shows
- `[set]`, `[dir]`, `[edit]`, `[find]` and the like stand for icons
- `(1)` `(2)` `(3)` point at the numbered notes under the drawing

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

## Every key, in one place

A key works only while the tester is clicked into the tree. The search key is
the exception. It works anywhere in the IDE.

| Key | Does | Page |
|---|---|---|
| `Enter` | Opens the selected test set or test run | [UC-004](openNode.md) |
| `Ctrl+M` | Creates a node under the selected one | [UC-005](createTestSet.md) |
| `Shift+F6` | Renames the selected node | [UC-007](renameNode.md) |
| `Delete` | Removes the selected nodes | [UC-008](removeNode.md) |
| `Ctrl+C` | Copies the selected nodes | [UC-009](moveOrCopyNodes.md) |
| `Ctrl+X` | Cuts the selected nodes | [UC-009](moveOrCopyNodes.md) |
| `Ctrl+V` | Pastes into the selected node | [UC-009](moveOrCopyNodes.md) |
| `Escape` | Takes the gray off nodes the tester cut | [UC-009](moveOrCopyNodes.md) |
| `Ctrl+Z` | Undoes the last tree change | [UC-011](undoAndRedo.md) |
| `Ctrl+Y` | Redoes it | [UC-011](undoAndRedo.md) |
| `1` `2` `3` | Inside the status popup: Assigned, Completed, Closed | [UC-013](setTestRunStatus.md) |
| `Ctrl+Alt+F` | Opens search, from anywhere in the IDE | [UC-017](searchProject.md) |
| The menu key, beside the right `Ctrl` | Opens the node menu | Below, under **What the tree shows** |

Six things a tester might expect have **no key** on the tree. **Order**, the
statuses, **Re-create**, **Edit Run**, **Run Tests** and **Details** are menu
items only. Each one is a decision the tester thinks about, not a reflex, and
none is used often enough to need a key. `F2` renames in the editors, not here.

---

## The panel

The panel is the Testin window that sits on the left of the IDE. The buttons
along its top are the IDE's own. The branch box and the tree are Testin's.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin                    [1] [2] [3] [3] [4] [5] [5]                     │
├────────────────────────────────────────────────────────────────────────────┤
│  [ main            v ]                                             (6)     │
├────────────────────────────────────────────────────────────────────────────┤
│  v Demo                                                            (7)     │
│    v Test Cases                                                            │
│      v Accounts                                                    (8)     │
│          Login                                                             │
│          Registration                                                      │
│        Checkout                                                            │
│        Legacy sign-in                            (deprecated: gray) (10)   │
│    v Test Runs                                                             │
│      v Sprint 7                                                            │
│          + cycle-1  Created                                        (9)     │
│          @ cycle-2  In Progress                                            │
│          * cycle-3  Completed                                              │
│        2025                                        (archived: gray)        │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Search Test Project** — opens search. It is the only header button with a
   key. The key works anywhere in the IDE.
2. **Settings** — the Testin page of the IDE settings.
3. **Expand All** and **Collapse All** — **Expand All** leaves retired rows
   closed. Retired means a **Deprecated** test set or an **Archived** package.
   **Collapse All** leaves the test project and its two folders visible.
4. **Refresh** — checks again which test project this code project uses, then
   reads it again from the folder.
5. **Select Test Project** and **New Test Project** — grayed until a Testin
   folder is set.
6. **The branch box** — shown only for a test project shared through Git. Its
   placeholder reads *Loading branches...* until Git answers.
7. **The test project row** — drawn bold, with the IDE's project icon. The two
   folders under it are bold too, and there are always exactly two.
8. **A package** — a folder icon. **A test set** — the icon the IDE uses for a
   changelist. Neither is bold.
9. **A test run** — draws its **status** as its icon. The status word follows
   the name in gray. So the tester reads the status of every test run without
   opening one. A test run never draws a kind icon.
10. **Gray text** — means retired, or cut and not yet pasted. Both look the
    same on purpose. Both mean the same thing to a tester: not part of the work
    in front of them.

Retired nodes sit at the bottom of their folder. Their number does not change
that.

## What the tree shows

**A row.** The test project is the top row, drawn bold, with the IDE's project
icon. Under it sit exactly two bold rows, **Test Cases** and **Test Runs**, each
with a bookmark icon. A package shows a folder icon. A test set shows the icon
the IDE uses for a changelist. A test run shows the icon of its status instead
of an icon for what it is, and after its name comes its status word in gray:
*Created*, *In Progress*, *Assigned*, *Completed* or *Closed*.

**Gray text.** A **Deprecated** test set, an **Archived** package, or a node cut
and not yet pasted, is drawn in gray. A retired node sits after every live
sibling. (rule 8)

**The order of siblings.** Live nodes come first, and retired ones last. Inside
each of those two groups, nodes with a number come first, smallest first. Then
come the nodes without a number, by the date they were created. Then by name.
(rule 10)

**A test project that is not Active** shows no children at all. (rule 52)

**Right-click.** Right-clicking a row that is not selected makes it the only
selection, and opens the menu at the pointer. Right-clicking inside a
multi-selection keeps the selection, and the menu acts on all of it.

**The menu key**, the one beside the right `Ctrl`, opens the node menu over the
selected row.

**A folder that cannot be read** shows one child row with the error icon,
reading, in red, *Could not load '\<folder name\>'*.

**What stays open.** After a paste, a rename or a refresh of the same test
project, the rows that were expanded stay expanded, and the selection stays. The
first time a different test project is shown, the whole tree is expanded, except
retired branches.

---

## The menu

The tester right-clicks any row, or presses the menu key beside the right
`Ctrl`. The menu opens. Which items are gray depends on the row. Which items
appear at all depends on which plugins are installed.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Open                     Enter  (1)   ┌ Actions ───────────────────┐      │
│  Create                  Ctrl+M        │  Activate                  │      │
│  Actions                     >   (2)   │  Deactivate                │      │
│  ──────────────────────────────        │  Archive                   │      │
│  Run Tests                       (3)   │  ──────────────────────    │      │
│  ──────────────────────────────        │  Undo Move 'Login'  Ctrl+Z │      │
│  Export                          (4)   │  Redo               Ctrl+Y │      │
│  Import                                │  Re-create                 │      │
│  ──────────────────────────────        │  Remove             Delete │      │
│  Sync With Remote                (5)   │  Rename           Shift+F6 │      │
│  View Pending Commits                  │  Order                     │      │
│  ──────────────────────────────        │  Copy               Ctrl+C │      │
│  Sync With SFTP                  (6)   │  Cut                Ctrl+X │      │
│  ──────────────────────────────        │  Paste              Ctrl+V │      │
│  Edit Run                        (7)   └────────────────────────────┘      │
│  Set Status                                                                │
│  ──────────────────────────────                                            │
│  Generate Report          Ctrl+P (8)                                       │
│  Details                                                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Open** and **Create** — first, because they are the two things done most.
2. **Actions** — a submenu. It holds everything that changes a node in place:
   - its status
   - undo and redo
   - re-create
   - remove
   - rename
   - order
   - the clipboard

   Status entries appear only for the kind selected. The three rows shown are a
   test project's.
3. **Run Tests** — only when the automation plugin is present.
4. **Export**, **Import** — these belong to reports, export, import and sync.
5. **Sync With Remote**, **View Pending Commits** — only when the Git plugin is
   present. Otherwise the whole section disappears, dividing line included.
6. **Sync With SFTP** — always present. Grayed unless the test project is
   shared over SFTP.
7. **Edit Run** and **Set Status** — a test run's own two entries. Grayed once
   the test run is signed off.
8. **Generate Report** and **Details** — last.

Every entry that changes something confirms itself once. The confirmation is
one past-tense word. Every entry that only shows something confirms nothing.

---

## Also on this menu

Six items on the tree's menu belong to another part of Testin. They are
documented there:

| Item | Belongs to |
|---|---|
| **Export**, **Import** | Reports, export, import and sync |
| **Generate Report** (`Ctrl+P`) | The same |
| **Sync With Remote**, **View Pending Commits** | The same, the Git half |
| **Sync With SFTP** | The same, the SFTP half |

---

## Why the panel is built this way

### The tree shows one test project, not a list of them

The panel shows the one test project this code project is set to use, and no
other. With a list of test projects in the tree, three things could disagree
about which one is open:

- the tree
- the reports
- the test runs

The choice is written into the code project. So a colleague who copies that
project down gets the same test project, with no setup.

### A test run shows its status instead of a plain icon

Every other row shows what it *is*. A test run shows how far it has *got*: its
status icon, and the status word. That is the one thing a tester scanning the
tree wants to know about a test run. Without it, they would open each one to
find out.

### Retired and cut look the same

Both are gray. These three all mean "not part of current work right now":

- a **Deprecated** test set
- an **Archived** package
- a node cut but not yet pasted

One color for one meaning is easier to learn than three.

### Every removal, move and copy asks first

The confirmation names what will happen, as in *Remove 'Login'?* or *Move
'Login' into 'Smoke'?*. It shows where from and where to. The confirm key is
named for the verb. Nothing in the tree changes until the tester presses
`Enter`.

### The menu is short at the top and deep in Actions

The top level holds what is done often: open and create. It also holds what
belongs to other parts of Testin. Everything that changes a node where it sits
is one level down, under **Actions**. So the first thing the tester sees is
short, and anything that removes or moves something takes a second, deliberate
step.

### Order is a dialog, not a drag

Dropping between rows to reorder is not offered. A number is better in three
ways:

- it is written down
- it stays right when Testin reads the folder again
- it is the same number the automation uses to decide the running order

Dragging would be quicker, but it would leave nothing written down.

---

## Where the plugin breaks its own rules

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
| **Question 1** | May a test run go backwards, from **Assigned** to **Created**, or from **In Progress** to **Created**? | Nothing prevents it today (difference 5). The same question for the whole product is [question 1 in the product's own document](../product.md#9-undecided). |
| **Question 2** | Should a signed-off test run be locked in the tree, as well as in its editor? | Today it is not (difference 3). Its own description says its name must not change. The tree's rename does not check. |
| **Question 3** | What should the paste refusal say? | *Select a folder* is shown for six different reasons. Four of them are: the wrong side of the tree, the node itself, another test project, and a test run. Choosing a different folder only fixes one of the six. |
| **Question 4** | On a Mac, should cut, paste, undo and redo in the tree use the Mac's own key for those actions, the way copy already does? | Today copy does. The other four do not. The keys are named in the key table above. |

---

[Documentation](../README.md) › **The project panel**
