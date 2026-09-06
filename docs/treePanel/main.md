[Documentation](../README.md) › The tree panel

# The tree panel

The tree panel is the tree on the left of the IDE. It holds every test project, test
set and test run. Everything in Testin starts here.

| | |
|---|---|
| **Part of Testin** | The tree panel |
| **Answers** | What the panel is for, what a tester can do in it, exactly what happens step by step, and what every screen looks like |
| **Numbering** | Use cases are UC-TREE-PANEL-001 to UC-TREE-PANEL-028, apart from 024. Rules are numbered 1 to 90, apart from 66 and 67. The search took all three, and is now [UC-INTERNAL-001](../internal/globalSearch.md). They belong to the tree panel. Every other part of Testin starts its own numbers again |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every rule, key, label and message read from the code |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

Each one is a page of its own: the story, its rules, its screens and its steps.

| | What the tester does | |
|---|---|---|
| | **Getting to the tree** | |
| **UC-TREE-PANEL-001** | [Open the panel and reach the tree](reachTheTree.md) | |
| **UC-TREE-PANEL-002** | [Create a test project](createTestProject.md) | |
| **UC-TREE-PANEL-003** | [Import a test project that already exists](importTestProject.md) | |
| **UC-TREE-PANEL-004** | [Choose which test project this code project uses](chooseTestProject.md) | |
| | **Nodes** | |
| **UC-TREE-PANEL-005** | [Open a test set](openTestSet.md) | |
| **UC-TREE-PANEL-006** | [Open a test run](openTestRun.md) | |
| **UC-TREE-PANEL-007** | [Create a test set](createTestSet.md) | |
| **UC-TREE-PANEL-008** | [Create a test set package](createTestSetPackage.md) | |
| **UC-TREE-PANEL-009** | [Create a test run](createTestRun.md) | |
| **UC-TREE-PANEL-010** | [Create a test run package](createTestRunPackage.md) | |
| **UC-TREE-PANEL-011** | [Rename a node](renameNode.md) | |
| **UC-TREE-PANEL-012** | [Remove a node](removeNode.md) | |
| | **Arranging the tree** | |
| **UC-TREE-PANEL-013** | [Move nodes](moveNodes.md) | |
| **UC-TREE-PANEL-014** | [Copy nodes](copyNodes.md) | |
| **UC-TREE-PANEL-015** | [Order nodes among their siblings](orderNodes.md) | |
| **UC-TREE-PANEL-016** | [Undo a change to the tree](undoChange.md) | |
| **UC-TREE-PANEL-017** | [Redo a change to the tree](redoChange.md) | |
| | **Statuses** | |
| **UC-TREE-PANEL-018** | [Retire a test project, a test set or a package](retireNode.md) | |
| **UC-TREE-PANEL-019** | [Bring a retired node back](reactivateNode.md) | |
| **UC-TREE-PANEL-020** | [Set a test run's status](setTestRunStatus.md) | |
| **UC-TREE-PANEL-021** | [Re-create a test run](reCreateTestRun.md) | |
| **UC-TREE-PANEL-022** | [Edit a test run](editTestRun.md) | |
| | **Working from the tree** | |
| **UC-TREE-PANEL-023** | [Run the automation for everything a node holds](runTests.md) | |
| **UC-TREE-PANEL-025** | [Refresh the tree from disk](refreshTree.md) | |
| **UC-TREE-PANEL-026** | [Switch the Git branch of the test project](switchBranch.md) | |
| **UC-TREE-PANEL-027** | [See what a node holds](nodeDetails.md) | |
| **UC-TREE-PANEL-028** | [Use the buttons at the top of the panel](panelToolbar.md) | |

---

## Where the panel sits in the IDE

Testin puts two tool windows in IntelliJ IDEA. The tree panel is docked on
the left, beside the places the IDE keeps its own trees. The view panel is
docked on the right. **Both stripes read Testin**, so the side they are on is
the only thing that tells them apart.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin  -  IntelliJ IDEA                                       - □ ×      │
├──────────────────────────────────┬──────────────────────┬──────────────────┤
│  Testin                     (1)  │                 (2)  │             (3)  │
│                                  │                      │                  │
│  [ main                 v ]      │                      │                  │
│                                  │                      │                  │
│  v Demo                          │                      │                  │
│    v Test Cases                  │                      │                  │
│      v Accounts                  │                      │                  │
│          Login                   │                      │                  │
│          Registration            │    Editor panel      │    View panel    │
│        Checkout                  │                      │                  │
│        Legacy sign-in            │                      │                  │
│    v Test Runs                   │                      │                  │
│      v Sprint 7                  │                      │                  │
│          + cycle-1  Created      │                      │                  │
│          @ cycle-2  In Progress  │                      │                  │
│          * cycle-3  Completed    │                      │                  │
└──────────────────────────────────┴──────────────────────┴──────────────────┘
```

1. **The tree panel** — everything on this page is about it. It is
   drawn wide here so the tree can be read. In the IDE the tester sets its width.
2. **Editor panel** — where a test set or a test run opens when the tester opens
   one from the tree. The tree panel itself never shows a test case.
3. **View panel** — details, history and open bugs for the test case the tester
   has selected. It is a part of Testin of its own, and its pages are not written
   yet.

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
- **Signed off** means a test run is **Completed** or **Closed**. Its test
  cases, verdicts and settings can no longer change, though the tree can still
  rename, move and remove it.
- A **container** is **Test Cases** or **Test Runs**. There are always exactly
  two, and they come with the test project.
- A **verdict** is what a tester records against one test case in one test
  run: **Passed**, **Failed** or **Blocked**. **Pending** means the test run
  has not reached that test case yet. **Untested** means it never will,
  because the test run was signed off first.

The panel's job is small and strict. It shows one test project, the one this
code project is bound to. It lets the tester shape that test project: create,
name, group, order, retire and remove the things in it.

Reading and executing test cases happens in editors. The panel opens those
editors. The panel itself never shows a test case.

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

## Every key the tree answers to

A key works only while the tester is clicked into the tree. The search key is
the exception. It works anywhere in the IDE. A dialog's own keys are drawn on
its status bar, at the foot of the dialog.

| Key | Does | Page |
|---|---|---|
| `Enter` | Opens the selected test set or test run | [UC-TREE-PANEL-005](openTestSet.md) |
| `Context Menu` | Opens the menu on the selected node, without the mouse | [The menu](#the-menu) |
| `Ctrl+M` | Creates a node under the selected one | [UC-TREE-PANEL-007](createTestSet.md) |
| `Shift+F6` | Renames the selected node | [UC-TREE-PANEL-011](renameNode.md) |
| `Delete` | Removes the selected nodes | [UC-TREE-PANEL-012](removeNode.md) |
| `Ctrl+X` | Cuts the selected nodes, to move them | [UC-TREE-PANEL-013](moveNodes.md) |
| `Ctrl+C` | Copies the selected nodes | [UC-TREE-PANEL-014](copyNodes.md) |
| `Ctrl+V` | Pastes into the selected node | [UC-TREE-PANEL-013](moveNodes.md) |
| `Escape` | Takes the gray off nodes the tester cut | [UC-TREE-PANEL-013](moveNodes.md) |
| `Ctrl+Z` | Undoes the last tree change | [UC-TREE-PANEL-016](undoChange.md) |
| `Ctrl+Y` | Redoes it | [UC-TREE-PANEL-017](redoChange.md) |
| `1` `2` `3` | Inside the status popup: Assigned, Completed, Closed | [UC-TREE-PANEL-020](setTestRunStatus.md) |
| `Ctrl+Alt+F`, `Cmd+Alt+F` on a Mac | Opens search, from anywhere in the IDE | [UC-INTERNAL-001](../internal/globalSearch.md) |
| `Ctrl+P` | Generates a report on the selected node | Reports, export, import and sync, not written yet |
| The menu key, beside the right `Ctrl` | Opens the node menu | Below, under **What the tree shows** |

Six things a tester might expect have **no key** on the tree. **Order**, the
statuses, **Re-create**, **Edit Run**, **Run Tests** and **Details** are menu
items only. Each one is a decision the tester thinks about, not a reflex, and
none is used often enough to need a key.

The panel's own seven buttons have no keys either, except search. They are
[UC-TREE-PANEL-028](panelToolbar.md). `F2` renames in the editors, not here.

---

## Every message, and the use case it belongs to

**This is the exit criteria.** A tester knows an action finished by the message
Testin shows, and knows it did not by the refusal. Every message the panel can
show is here, quoted as the plugin writes it. A word in angle brackets is
filled in with whatever the tester is working on, and *N* with a count.

### When it worked

A success message fades on the status bar and leaves no trace. It is one word
in the past tense wherever one word will do. (rule 7)

| Message | Means | Use case |
|---|---|---|
| *Created* | A node was created | [UC-TREE-PANEL-007](createTestSet.md), [UC-TREE-PANEL-009](createTestRun.md) |
| *Project created* | A test project was created | [UC-TREE-PANEL-002](createTestProject.md) |
| *Project cloned* | A test project was brought down from Git | [UC-TREE-PANEL-003](importTestProject.md) |
| *Bound* | This code project now uses that test project. The name follows | [UC-TREE-PANEL-004](chooseTestProject.md) |
| *Run created* | A test run was created | [UC-TREE-PANEL-009](createTestRun.md), [UC-TREE-PANEL-021](reCreateTestRun.md) |
| *Renamed* | A node was renamed | [UC-TREE-PANEL-011](renameNode.md) |
| *Removed*, *Removed N* | One or more nodes went to the recycle bin | [UC-TREE-PANEL-012](removeNode.md) |
| *Copied*, *Copied N* | Nodes are on the clipboard, to copy | [UC-TREE-PANEL-013](moveNodes.md) |
| *Cut*, *Cut N* | Nodes are on the clipboard, to move | [UC-TREE-PANEL-013](moveNodes.md) |
| *Moved*, *Moved N* | The cut nodes have moved | [UC-TREE-PANEL-013](moveNodes.md) |
| *Pasted*, *Pasted N* | The copied nodes have been duplicated | [UC-TREE-PANEL-013](moveNodes.md) |
| *Ordered* | A node's number was set or cleared | [UC-TREE-PANEL-015](orderNodes.md) |
| *Undone* | The last change was taken back | [UC-TREE-PANEL-016](undoChange.md) |
| *Redone* | The undone change was put back | [UC-TREE-PANEL-016](undoChange.md) |
| *Active*, *Inactive*, *Archived*, *Deprecated* | The new status of a test project, package or test set | [UC-TREE-PANEL-018](retireNode.md) |
| *Assigned*, *Completed*, *Closed* | The new status of a test run | [UC-TREE-PANEL-020](setTestRunStatus.md) |
| *Updated* | A test run's name, test cases or settings were saved | [UC-TREE-PANEL-022](editTestRun.md) |
| *Running*, *Running N* | The automation started | [UC-TREE-PANEL-023](runTests.md) |
| *Refreshed* | The tree was read again from disk | [UC-TREE-PANEL-025](refreshTree.md) |
| *Switched to \<branch\>* | The test project is on another branch | [UC-TREE-PANEL-026](switchBranch.md) |

### When it refused

A refusal fades too. It says what stopped the action, and nothing was changed.

| Message | Means | Use case |
|---|---|---|
| *\<name\> Already Exists* | The name is taken where it was going | [UC-TREE-PANEL-002](createTestProject.md), [UC-TREE-PANEL-007](createTestSet.md), [UC-TREE-PANEL-009](createTestRun.md), [UC-TREE-PANEL-011](renameNode.md), [UC-TREE-PANEL-021](reCreateTestRun.md), [UC-TREE-PANEL-022](editTestRun.md) |
| *Select a folder* | The selected place cannot take what is on the clipboard | [UC-TREE-PANEL-013](moveNodes.md) |
| *'\<name\>' already exists in '\<folder\>'*, *N items already exist in '\<folder\>'* | The destination already holds that name. The rest of the paste still moves | [UC-TREE-PANEL-013](moveNodes.md) |
| *A test run needs a name* | The name box was emptied | [UC-TREE-PANEL-009](createTestRun.md), [UC-TREE-PANEL-021](reCreateTestRun.md), [UC-TREE-PANEL-022](editTestRun.md) |
| *'\<parent\>' no longer exists - test run not created* | The folder went away while the dialog was open | [UC-TREE-PANEL-009](createTestRun.md) |
| *'\<run\>' no longer exists - nothing saved* | The test run went away while the dialog was open | [UC-TREE-PANEL-022](editTestRun.md) |
| *'\<run\>' was \<status\> while this was open - nothing saved* | Someone signed the test run off while the dialog was open | [UC-TREE-PANEL-022](editTestRun.md) |
| *\<name\> has no test cases to run* | Nothing under the node can be run | [UC-TREE-PANEL-023](runTests.md) |
| *No Test Projects* | There is nothing to choose. It adds *Create one under the Testin root first* | [UC-TREE-PANEL-004](chooseTestProject.md) |
| *Java Test Source Not Found* | The IDE project has no Java test folder, so no automation code is written | [UC-TREE-PANEL-007](createTestSet.md) |

### When something failed

These stay in the IDE's notification log, because they are real failures rather
than feedback on what the tester just typed.

| Message | Means | Use case |
|---|---|---|
| *Not Bound* | The choice could not be written into the code project, so it will not be remembered | [UC-TREE-PANEL-004](chooseTestProject.md) |
| *No Test Project Named* | The code project does not say which test project it is about, so nothing can be cloned | [UC-TREE-PANEL-003](importTestProject.md) |
| *Clone Failed* | The repository could not be cloned. The reason follows | [UC-TREE-PANEL-003](importTestProject.md) |
| *Clone Error* | Something needed for the clone was missing | [UC-TREE-PANEL-003](importTestProject.md) |
| *Rename Failed* | The folder could not be renamed on disk. The reason follows | [UC-TREE-PANEL-011](renameNode.md) |
| *Undo Incomplete* | Some removed nodes could not be put back. It says how many of how many | [UC-TREE-PANEL-016](undoChange.md) |
| *Unable to update status to \<status\>* | A test project's status could not be written | [UC-TREE-PANEL-018](retireNode.md) |
| *Unable to mark test set \<status\>* | A test set's status could not be written | [UC-TREE-PANEL-018](retireNode.md) |
| *Unable to mark package \<status\>* | A package's status could not be written | [UC-TREE-PANEL-018](retireNode.md) |
| *Git Error* | The branches could not be read. The reason follows | [UC-TREE-PANEL-026](switchBranch.md) |
| *Git Fetch Warning* | The branch list may be out of date | [UC-TREE-PANEL-026](switchBranch.md) |

### Shown in the tree itself, not as a message

| What the tester sees | Means |
|---|---|
| *Could not load '\<folder name\>'*, in red, as a child row | That folder's contents could not be read |
| *\<name\> is archived, so it is not opened*, in red, above the list of test projects | The bound test project is **Archived**. See [UC-TREE-PANEL-001](reachTheTree.md) |
| *testin.yml names \<name\>, which is not under the Testin root*, in red | The code project names a test project the Testin folder does not hold. See [UC-TREE-PANEL-001](reachTheTree.md) |
| *testin.yml names \<name\>, which could not be read*, in red | The test project's folder is there, but Testin could not read it |

---

## The panel

The panel is the Testin window that sits on the left of the IDE. The buttons
along its top are the IDE's own. The branch box and the tree are Testin's.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin              [1] [2] [3] [4] [5] [6] [7]                           │
├────────────────────────────────────────────────────────────────────────────┤
│  [ main            v ]                                             (8)     │
├────────────────────────────────────────────────────────────────────────────┤
│  v Demo                                                            (9)     │
│    v Test Cases                                                            │
│      v Accounts                                                   (10)     │
│          Login                                                             │
│          Registration                                                      │
│        Checkout                                                            │
│        Legacy sign-in                            (deprecated: gray) (12)   │
│    v Test Runs                                                             │
│      v Sprint 7                                                            │
│          + cycle-1  Created                                       (11)     │
│          @ cycle-2  In Progress                                            │
│          * cycle-3  Completed                                              │
│        2025                                        (archived: gray)        │
└────────────────────────────────────────────────────────────────────────────┘
```

1. to 7. **The toolbar** — seven buttons, left to right: **Search Test
   Project**, **Settings**, **Expand All**, **Collapse All**, **Refresh**,
   **Select Test Project** and **New Test Project**. Each one is
   [UC-TREE-PANEL-028](panelToolbar.md).
8. **The branch box** — shown only for a test project shared through Git. Its
   placeholder reads *Loading branches...* until Git answers.
9. **The test project row** — drawn bold, with the IDE's project icon. The two
   folders under it are bold too, and there are always exactly two.
10. **A package** — a folder icon. **A test set** — the icon the IDE uses for a
   changelist. Neither is bold.
11. **A test run** — draws its **status** as its icon. The status word follows
   the name in gray. So the tester reads the status of every test run without
   opening one. A test run never draws a kind icon.
12. **Gray text** — means retired, or cut and not yet pasted. Both look the
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
multi-selection keeps the selection. Right-clicking below the last row, or on
a row that could not be read, opens no menu at all.

**What the menu does with several rows selected.** Three different things, and
two of them are quiet:

- **Open**, **Remove**, **Copy** and **Cut** act on all of them.
- **Rename**, **Order**, **Paste**, **Create** and **Details** stay black and
  act on the first row alone, saying nothing about the rest.
- **Re-create**, **Edit Run**, **Set Status**, **Export**, **Import** and every
  status entry go gray instead.

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
│  ──────────────────────────────        │  Deactivate                │      │
│  Actions                     >   (2)   │  Archive                   │      │
│  ──────────────────────────────        │  Undo Move 'Login'  Ctrl+Z │      │
│  Run Tests                       (3)   │  Redo               Ctrl+Y │      │
│  ──────────────────────────────        │  Re-create                 │      │
│  Export                          (4)   │  Remove             Delete │      │
│  Import                                │  Rename           Shift+F6 │      │
│  ──────────────────────────────        │  Order                     │      │
│  Sync With Remote                (5)   │  Copy               Ctrl+C │      │
│  View Pending Commits                  │  Cut                Ctrl+X │      │
│  ──────────────────────────────        │  Paste              Ctrl+V │      │
│  Sync With SFTP                  (6)   └────────────────────────────┘      │
│  ──────────────────────────────                                            │
│  Edit Run                        (7)                                       │
│  Set Status                                                                │
│  ──────────────────────────────                                            │
│  Generate Report          Ctrl+P (8)                                       │
│  Details                                                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Open** and **Create** — first, because they are the two things done most.
   A dividing line follows them.
2. **Actions** — a submenu. It holds everything that changes a node in place:
   - its status
   - undo and redo
   - re-create
   - remove
   - rename
   - order
   - the clipboard

   The submenu is one unbroken list. Status entries appear only for the kind
   selected, and the other kinds' entries are not there at all. The three rows
   shown are a test project's.
3. **Run Tests** — only when the TestNG plugin is present.
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

Stated, not hidden. Each one is a real gap, found by reading the code this
documentation describes. None of them has a bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | Rule 7 — one past-tense word | Creating a test run says *Run created*. Creating a test project says *Project created* or *Project cloned*. Every other creation says *Created*. |
| **Difference 2** | Rule 38 — nothing lands in a test run | **Paste** is offered on a test run. It always refuses, with *Select a folder*. It should be grayed, as it is on a test set. |
| **Difference 3** | Rule 9 — a signed-off test run does not change | A **Completed** or **Closed** test run can still be renamed, moved, reordered and removed from the tree. Only editing, running and its status are blocked. |
| **Difference 4** | Rule 23, rule 26 — the dialog offers the kinds | Neither create dialog names its two kinds. Both rows show only their hints: *Holds test cases* and *Groups test sets* on the test case side, *Records execution results* and *Groups test runs* on the test run side. Someone removed the names while tidying up the labels, and nobody noticed. |
| **Difference 5** | Rule 56 — Created and In Progress are the test run's own | The status popup offers **Created** and **In Progress** as choices. It also lets a test run go backwards, from **Assigned** to **Created**. |
| **Difference 6** | Rule 76 — canceling a cut empties the clipboard | The tester cuts nodes, then presses the key that clears the gray. The nodes stay on the clipboard. The next paste still moves them, though the tester canceled the cut. A paste that worked does not empty it either, so the same nodes can be offered again. |
| **Difference 7** | Rule 20 — a choice that was not saved is not reported as saved | Only the **Select Test Project** dialog checks. Creating a test project, cloning one, and clicking a test project on the welcome screen all report success without looking. If the write failed, the tester is told the work is done and the choice is gone next time the project opens. |
| **Difference 8** | Rule 77 — the two buttons come back once a Testin folder is set | **New Test Project** never comes back. Once it has been drawn without a folder it stays gray for the rest of the IDE session, however the setting changes. **Select Test Project** does come back. |
| **Difference 9** | Rule 89 — an edit changes what the tester chose | Saving an edit also deletes the rows of test cases that were deleted from their test set after the test run was made, with their verdicts and failure detail. The rows are not in the dialog, so the tester cannot see them or keep them, and it happens even when nothing else was changed. |
| **Difference 10** | Rule 7 — a change confirms itself once | Setting a test run's status from the tree does not tell that test run's open editor. It keeps showing the old status, and the rows that just became **Untested**, until it is reopened. |
| **Difference 11** | Rule 6 — nothing changes until the tester confirms | **Rename**, **Order**, **Paste**, **Create** and **Details** stay black with several rows selected, and act on the first row alone. Nothing says the other rows were passed over. |
| **Difference 12** | Rule 44 — a number is 1 or higher | A number too large to hold silently clears the order and puts the node back into date order, while Testin still says *Ordered*. |
| **Difference 13** | Rule 33 — the confirmation says what will go | The **Select Test Project** dialog's first column has no heading. The same caption sweep as difference 4. |
| **Difference 14** | Rule 7 — a change confirms itself once | The **Uncommitted Changes** dialog reads *1 change in this test project are not committed* when there is exactly one. |
| **Difference 15** | Rule 35 — a removal can be undone | A node whose copy could not be kept aside is still removed, and is not on the undo history. Nothing says so, and `Ctrl+Z` then takes back whatever change came before it. |

---

## Not decided

| | Question | Why it is open |
|---|---|---|
| **Question 1** | May a test run go backwards, from **Assigned** to **Created**, or from **In Progress** to **Created**? | Nothing prevents it today (difference 5). The same question for the whole product is [question 1 in the product's own document](../product.md#9-undecided). |
| **Question 2** | Should a signed-off test run be locked in the tree, as well as in its editor? | Today it is not (difference 3). Its own description says its name must not change. The tree's rename does not check. |
| **Question 3** | What should the paste refusal say? | *Select a folder* is shown for six different reasons. Four of them are: the wrong side of the tree, the node itself, another test project, and a test run. Choosing a different folder only fixes one of the six. |
| **Question 4** | On a Mac, should cut, paste, undo and redo in the tree use the Mac's own key for those actions, the way copy already does? | Today copy does. The other four do not. The keys are named in the key table above. |

---

[Documentation](../README.md) › **The tree panel**
