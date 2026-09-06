[Documentation](../README.md) › The project panel

# The project panel

The panel is the tree on the left of the IDE. It holds every test project, test
set and test run. Everything in Testin starts here.

| | |
|---|---|
| **Part of Testin** | The project panel, the tree on the left |
| **Answers** | What the panel is for, what a tester can do in it, exactly what happens for every gesture, and what every screen looks like |
| **Numbering** | Rules are numbered 1 to 76, use cases 1 to 20, and scenarios 1 to 75. They belong to the project panel. Every other part of Testin starts its own numbers again |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every rule, key, label and message read from the code |
| **Written to** | [How a document is written](../standard.md) |

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

## How to read a scenario

Each scenario has a number and a name, and says what happens in three steps:
**Given** what is true first, **When** what the tester does, and **Then** what
happens. **And** adds a line to whichever step it follows.

- A scenario with no **When** describes what the tester sees, without doing
  anything.
- The line under the name, *Keeps rule 8*, names the rule the scenario holds up.
- Words like *\<name\>* stand for whatever the tester is working on. Testin puts
  the real name there.

## How to read these drawings

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

Each row links to the scenario that owns the key. A key works only while the
tester is clicked into the tree. The search key is the exception. It works
anywhere in the IDE.

| Key | Does | Scenario |
|---|---|---|
| `Enter` | Opens the selected test set or test run | [scenario 19](#use-case-4--open-a-test-set-or-a-test-run) |
| `Ctrl+M` | Creates a node under the selected one | [scenario 22](#use-case-5--create-a-test-set-or-a-test-set-package) |
| `Shift+F6` | Renames the selected node | [scenario 32](#use-case-7--rename-a-node) |
| `Delete` | Removes the selected nodes | [scenario 36](#use-case-8--remove-a-node) |
| `Ctrl+C` | Copies the selected nodes | [scenario 40](#use-case-9--move-or-copy-nodes) |
| `Ctrl+X` | Cuts the selected nodes | [scenario 41](#use-case-9--move-or-copy-nodes) |
| `Ctrl+V` | Pastes into the selected node | [scenario 42](#use-case-9--move-or-copy-nodes) |
| `Escape` | Takes the gray off nodes the tester cut | [scenario 47](#use-case-9--move-or-copy-nodes) |
| `Ctrl+Z` | Undoes the last tree change | [scenario 51](#use-case-11--undo-and-redo-a-change-to-the-tree) |
| `Ctrl+Y` | Redoes it | [scenario 52](#use-case-11--undo-and-redo-a-change-to-the-tree) |
| `1` `2` `3` | Inside the status popup: Assigned, Completed, Closed | [scenario 57](#use-case-13--set-a-test-runs-status) |
| `Ctrl+Alt+F` | Opens search, from anywhere in the IDE | [scenario 67](#use-case-17--find-anything-in-the-project) |
| The menu key, beside the right `Ctrl` | Opens the node menu | [scenario 9](#the-tree-itself) |

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

## The tree itself

**Scenario 1 · What a row shows**
> **Given** the tree is showing a test project
> **Then** the test project is the top row, drawn bold, with the IDE's project icon
> **And** under it sit exactly two bold rows, **Test Cases** and **Test Runs**, each with a bookmark icon
> **And** a package shows a folder icon
> **And** a test set shows the icon the IDE uses for a changelist
> **And** a test run shows the icon of its status, instead of an icon for what it is
> **And** after a test run's name comes its status word in gray: *Created*, *In Progress*, *Assigned*, *Completed* or *Closed*

**Scenario 2 · A retired or cut node is gray**
Keeps rule 8.
> **Given** a test set is **Deprecated**, or a package is **Archived**, or a node was cut and not yet pasted
> **Then** its name is drawn in gray
> **And** a retired node sits after every live sibling

**Scenario 3 · Siblings are in one order**
Keeps rule 10.
> **Given** a folder holds several nodes
> **Then** live nodes come first, and retired ones last
> **And** inside each of those two groups, nodes with a number come first, smallest first
> **And** then come the nodes without a number, by the date they were created
> **And** then by name

**Scenario 4 · A test project that is not Active shows nothing under it**
Keeps rule 52.
> **Given** the test project's status is **Inactive** or **Archived**
> **Then** the test project row shows no children at all

**Scenario 5 · Right-click selects, then opens the menu**
> **Given** the tester right-clicks a row that is not selected
> **Then** that row becomes the only selection
> **And** the menu opens at the pointer
>
> **Given** the tester right-clicks a row inside a multi-selection
> **Then** the selection is kept
> **And** the menu acts on all of it

**Scenario 6 · Double-click opens**
> **Given** the tester double-clicks a test set or a test run
> **Then** it opens in its editor, the same as [scenario 19](#use-case-4--open-a-test-set-or-a-test-run)

**Scenario 7 · A folder that cannot be read says so**
> **Given** a folder's children cannot be loaded
> **Then** it shows one child row with the error icon
> **And** that row reads, in red, *Could not load '\<folder name\>'*

**Scenario 8 · The tree remembers what was open**
> **Given** the tree is rebuilt after a paste, a rename or a refresh of the same test project
> **Then** the rows that were expanded stay expanded
> **And** the selection stays
>
> **Given** a different test project is shown for the first time
> **Then** the whole tree is expanded, except retired branches

**Scenario 9 · The keyboard opens the menu**
> **Given** a row is selected and the tree has focus
> **When** the tester presses the menu key, the one beside the right `Ctrl`
> **Then** the node menu opens over the selected row

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

## Getting to the tree

### Use case 1 · Open the panel and reach the tree

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

Before there is a tree, the panel shows one of five screens. The screens come
in a fixed order. Each screen holds exactly one link. The link is the only step out of
that situation.

#### No Testin folder

```
┌────────────────────────────────────────────────────────────────────────────┐
│                                                                            │
│  Welcome to Testin                                                         │
│                                                                            │
│  The new awesome test management tool                                      │
│                                                                            │
│  By                                                                        │
│  Muteb almughyiri                                                          │
│                                                                            │
│                                                                            │
│  [gear]  Configure Testin settings                                 (1)     │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. The one link. Nothing else is offered, because nothing else can work yet.
   **Select Test Project** and **New Test Project** in the header are grayed.

#### Choose a project

```
┌────────────────────────────────────────────────────────────────────────────┐
│                                                                            │
│  testin.yml names Payments, which is not under the Testin root      (1)    │
│                                                                            │
│  [dir]  Demo  Active                                                (2)    │
│  [dir]  Mobile  Active                                                     │
│  [dir]  Legacy  Archived                                                   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The reason** — in red. It is shown when this code project named a test
   project Testin could not use. The reason is one of:
   - *not under the Testin root*
   - *archived*
   - *could not be read*
2. **One link per test project** — the name, then the status. The tester
   clicks a link. This code project is then set to use that test project, and
   the tree appears. If there are more than six test projects, Testin shows one
   link instead of a list. That link opens the **Select Test Project** dialog.

The other two screens follow the same shape: a gray line saying what is true,
then the one link. They are:

- *\<name\> is not on this machine yet*, with **Clone \<name\>**
- **Create your first test project**

**Scenario 10 · One of five screens, in a fixed order**
Keeps rule 14.
> **Given** the tester opens the **Testin** tool window
> **Then** the panel shows exactly one of these, checked in this order:
>
> | If | The panel shows |
> |---|---|
> | No Testin folder is set | *Welcome to Testin* and the link **Configure Testin settings** |
> | The bound test project is found | The tree |
> | This code project names a test project that is not on this machine, and gives its Git address | *\<name\> is not on this machine yet* and the link **Clone \<name\>** |
> | No test project exists under the Testin folder | The link **Create your first test project** |
> | Otherwise | One link per test project, showing its name and then **Active**, **Inactive** or **Archived**. With more than six test projects, one link instead: **Select the test project for this repository** |

**Scenario 11 · One test project binds itself**
Keeps rule 15.
> **Given** this repository names no test project, and exactly one test project exists under the Testin folder
> **Then** the panel binds to it without asking
> **And** the panel shows its tree

**Scenario 12 · An archived test project is refused with a reason**
Keeps rule 53.
> **Given** this repository is bound to a test project whose status is **Archived**
> **Then** the panel shows, in red, *\<name\> is archived, so it is not opened*
> **And** under that line sits the list of test projects to choose from

### Use case 2 · Create a test project

**As a** tester, **I want** to create a test project by name, or clone one from
a Git address, **so that** a new product under test has a place before any test
is written.

**Rules**

- **Rule 16** — A test project is a folder directly under the Testin folder.
  Any other folder there is ignored.
- **Rule 17** — Creating a test project binds this repository to it.
- **Rule 18** — Cloning needs two things. It needs the Git plugin. It also needs
  this repository to already name the test project it is cloning.

The screens are drawn under [use case 1](#use-case-1--open-the-panel-and-reach-the-tree).

**Scenario 13 · By name**
> **Given** the Testin folder is set
> **When** the tester presses the **New Test Project** button in the panel header, or the welcome link **Create your first test project**
> **And** types a name and presses `Enter`
> **Then** the test project folder is created under the Testin folder
> **And** this repository is bound to it
> **And** the tree appears
> **And** the message *Project created* is shown

**Scenario 14 · A name already used is refused**
Keeps rule 4.
> **Given** a folder with that name already exists under the Testin folder
> **When** the tester presses `Enter`
> **Then** nothing is created
> **And** the message *\<name\> Already Exists* is shown in red

**Scenario 15 · By Git address**
Keeps rule 18.
> **Given** the Git plugin is present, and this repository names the test project
> **When** the tester pastes a repository address instead of a name, and presses `Enter`
> **Then** the repository is cloned under that name
> **And** this repository is bound to it
> **And** the tree appears
> **And** the message *Project cloned* is shown
>
> **Given** this repository names no test project
> **Then** nothing is cloned
> **And** a warning titled *No Test Project Named* explains that the repository must say which test project it is about

### Use case 3 · Choose which test project this repository uses

**As a** tester, **I want** to point this repository at a different test project
under the same Testin folder, **so that** one machine can serve several products.

**Rules**

- **Rule 19** — The binding is written into the repository. A colleague who
  clones the repository gets the same test project, with no setup.
- **Rule 20** — If the binding cannot be written, Testin says so. It never
  reports the choice as saved.

The screens are drawn under [use case 1](#use-case-1--open-the-panel-and-reach-the-tree).

**Scenario 16 · Choose from the list**
Keeps rule 19.
> **Given** the Testin folder is set
> **When** the tester presses the **Select Test Project** button in the panel header
> **Then** the **Select Test Project** dialog lists every test project under the folder, with its status
> **And** the current test project is selected
> **When** the tester selects one and presses `Enter`
> **Then** the binding is written into this repository
> **And** the tree reloads on that test project
> **And** the message *Bound* is shown, with the test project's name

**Scenario 17 · No test projects to choose from**
> **Given** no test project folder exists under the Testin folder
> **When** the tester presses **Select Test Project**
> **Then** no dialog opens
> **And** the message *No Test Projects*, with the line *Create one under the Testin root first*, is shown in red

**Scenario 18 · Testin says when the choice cannot be saved**
Keeps rule 20.
> **Given** the repository's configuration file cannot be written
> **When** the tester chooses a test project
> **Then** an error titled *Not Bound* says the choice will not be remembered
> **And** the dialog stays open

---

## Nodes

### Use case 4 · Open a test set or a test run

**As a** tester, **I want** to open a test set or a test run from the tree,
**so that** I can read and work its test cases.

**Rules**

- **Rule 21** — Only a test set and a test run open in an editor. A package has
  nothing to open. A container has nothing to open. Neither of them says
  anything.
- **Rule 22** — The tester opens a node that is already open. Its tab comes
  forward. It does not open a second time.

**Scenario 19 · Open with Enter**
Keeps rule 21.
> **Given** a test set or a test run is selected
> **When** the tester presses `Enter`, or chooses **Open** from the menu
> **Then** it opens in its editor tab, named after the node
> **And** nothing is announced, because opening only moves the view (rule 7)

**Scenario 20 · Already open comes forward**
Keeps rule 22.
> **Given** the node's editor tab is already open
> **When** the tester presses `Enter`
> **Then** that tab comes to the front
> **And** no second tab opens

**Scenario 21 · Nothing to open**
Keeps rule 21.
> **Given** a package, a container or the test project is selected
> **Then** **Open** is grayed, and `Enter` does nothing

### Use case 5 · Create a test set or a test set package

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

#### Create Test Node

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Node                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [set]  set name...                                     (1)  │
│                                                              │
│  [set]  Test Set          Holds test cases              (2)  │
│  [pkg]  Test Set Package  Groups test sets                   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    ↑ ↓ Select    Escape Cancel           (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The name** — its gray hint text reads *set name...*. If the tester presses
   `Enter` with the box empty, the hint turns red and the cursor stays here.
2. **The two kinds** — each with a hint beside it. `↑` `↓` move between them,
   and the icon at the front of the name box changes to match. Clicking a row
   confirms straight away.
3. **The status bar** — every key this dialog answers to.

**Create Run Node** is the twin dialog on the test run side. It is identical
except for two things. Its kinds are *test run* (*Records execution results*)
and *test run package* (*Groups test runs*). Its hint text reads *set name,
like Sprint 3 Cycle 1...*.

> **Today the two kind names are blank.** The rows show only their hints.
> This is difference 4 in the business requirements.

**Scenario 22 · From the tree**
Keeps rule 23.
> **Given** **Test Cases** or a test set package is selected
> **When** the tester presses `Ctrl+M`, or chooses **Create**
> **Then** the **Create Test Node** dialog opens, with two kinds to pick from
> **And** beside *test set* it says *Holds test cases*, and beside *test set package* it says *Groups test sets*
> **And** the first kind is selected

**Scenario 23 · Name it and pick the kind**
> **Given** the dialog is open
> **When** the tester types a name, moves between the two kinds with `↑` and `↓`, and presses `Enter`
> **Then** the node is created under the selected parent
> **And** the tree refreshes
> **And** the message *Created* is shown

**Scenario 24 · A new test set opens and gets its code**
Keeps rule 25.
> **Given** the kind chosen was a test set
> **Then** it opens in its editor at once
> **And** where the Java plugin is present, its automation test class is generated

**Scenario 25 · A name is required**
Keeps rule 5.
> **Given** the name field is empty
> **When** the tester presses `Enter`
> **Then** the dialog stays open
> **And** the gray hint text turns red
> **And** the cursor stays in the box

**Scenario 26 · A sibling with that name refuses it**
Keeps rule 4.
> **Given** a node with that name already exists under the parent
> **When** the tester presses `Enter`
> **Then** the dialog closes, and nothing is created
> **And** *\<name\> Already Exists* is shown in red

**Scenario 27 · Nowhere to create**
Keeps rule 24.
> **Given** the test project, a test set or a test run is selected
> **Then** **Create** is grayed, and `Ctrl+M` does nothing

### Use case 6 · Create a test run or a test run package

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

#### Create Test Run

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Create Test Run                                                           │
├────────────────────────────────────────────────────────────────────────────┤
│  Test Run name:  [ cycle-2                            ]         (1)        │
│                                                                            │
│  v Configuration details                                        (2)        │
│    Change Log  [ Story-002 (register new user)...   ]                      │
│    Commit ID   [ 9f3c1ab                            ]                      │
│    Test Type   [ Functional Test  v ]   Platform  [ Web  v ]               │
│    Component   [ Frontend         v ]   Browser   [ Chrome v ]             │
│                                                                            │
│  [x] v Accounts                                                 (3)        │
│  [x]     Login                                                             │
│  [x]     Registration                                                      │
│  [ ]   Checkout                                                            │
│                                                                            │
│                                                    [ Create ]   (4)        │
├────────────────────────────────────────────────────────────────────────────┤
│  Tab Navigate    Space Check    Escape Cancel                              │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The name** — prefilled from the create dialog. Placeholder *Cycle-1*.
2. **Configuration details** — a section the tester can open and close. Its
   fields are:
   - change log
   - commit id
   - test type
   - platform
   - component
   - language
   - browser or device, only when the platform and component make it relevant
3. **The test cases** — a tree with a tick box for every test set still in use
   that has test cases in it. For a new test run, all are ticked. For a
   re-creation, only the ones the last cycle used.
4. **Create** — enabled only while at least one test case is ticked. `Enter`
   does nothing here. The button is the only way to confirm.

**Edit Test Run** is this same dialog with the button **Save**. It opens filled
with the test run's own:

- name
- test cases
- configuration

**Scenario 28 · From the tree**
Keeps rule 26.
> **Given** **Test Runs** or a test run package is selected
> **When** the tester presses `Ctrl+M`, or chooses **Create**
> **Then** the **Create Run Node** dialog opens, with two kinds
> **And** beside *test run* it says *Records execution results*, and beside *test run package* it says *Groups test runs*
> **When** the kind is a package, and the tester presses `Enter`
> **Then** the package is created, and *Created* is shown

**Scenario 29 · A test run is configured before it exists**
Keeps rule 27 and rule 28.
> **Given** the kind chosen was a test run
> **When** the tester presses `Enter`
> **Then** the **Create Test Run** dialog opens
> **And** it holds the typed name in *Test Run name*
> **And** it holds a *Configuration details* form
> **And** it holds a tree of every live, non-empty test set, with all test cases checked
> **And** retired test sets, anything under an **Archived** package, and empty test sets are not in that tree
> **When** the tester adjusts the checks with `Space`, moves with `Tab`, and presses the **Create** button
> **Then** the test run is written, with every checked test case **Pending** and the status **Created**
> **And** its editor opens
> **And** *Run created* is shown

**Scenario 30 · A test run needs test cases and a name**
Keeps rule 27 and rule 5.
> **Given** no test case is checked
> **Then** the **Create** button is disabled
>
> **Given** the name has been emptied
> **When** the tester presses **Create**
> **Then** the dialog stays open
> **And** *A test run needs a name* is shown in red

**Scenario 31 · The parent folder was removed while the dialog was open**
> **Given** the parent folder was removed while the dialog was open
> **When** the tester presses **Create**
> **Then** the dialog stays open
> **And** *'\<parent\>' no longer exists - test run not created* is shown in red

### Use case 7 · Rename a node

**As a** tester, **I want** to rename a test set, a package or a test run,
**so that** the tree says what things are called now.

**Rules**

- **Rule 30** — The test project and the two containers cannot be renamed from
  the tree.
- **Rule 31** — Renaming a test set or a test set package renames its automation
  code with it. The test case stays runnable.
- **Rule 32** — A rename can be undone.

#### Rename

```
┌──────────────────────────────────────────────────────────────┐
│  Rename                                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [edit]  Accounts                                       (1)  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    Escape Cancel                              │
└──────────────────────────────────────────────────────────────┘
```

1. The current name, filled in and selected. Typing replaces it.

**Scenario 32 · Rename with Shift+F6**
> **Given** a test set, a package or a test run is selected
> **When** the tester presses `Shift+F6`, or chooses **Actions → Rename**
> **Then** the **Rename** dialog opens, with the current name filled in and selected
> **When** the tester types the new name and presses `Enter`
> **Then** the node is renamed
> **And** the tree refreshes
> **And** *Renamed* is shown

**Scenario 33 · The code is renamed first**
Keeps rule 31.
> **Given** the node is a test set or a test set package, and the Java plugin is present
> **When** the rename runs
> **Then** its automation code is renamed before the folder is
> **And** its open editor, if it has one, is closed first

**Scenario 34 · When Testin will not rename**
Keeps rule 4, rule 5 and rule 30.
> **Given** the name is empty
> **Then** the gray hint text turns red, and the dialog stays open
>
> **Given** the name is unchanged
> **Then** the dialog closes and nothing happens, silently
>
> **Given** a sibling already has the new name
> **Then** nothing is renamed, and *\<name\> Already Exists* is shown in red
>
> **Given** the test project or a container is selected
> **Then** **Rename** is grayed, and `Shift+F6` does nothing
>
> **Given** the folder cannot be renamed on disk
> **Then** nothing is renamed, and no *Renamed* is shown
> **And** an error titled *Rename Failed* says why

**Scenario 35 · The tester can undo a rename**
Keeps rule 32.
> **Given** a node was just renamed
> **When** the tester presses `Ctrl+Z`
> **Then** the old name is back, and *Undone* is shown

### Use case 8 · Remove a node

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

#### Confirm Removing

```
┌──────────────────────────────────────────────────────────────┐
│  Confirm Removing                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Remove 'Accounts'?                                     (1)  │
│  Holds 2 test sets, 14 test cases and 0 test runs       (2)  │
│                                                              │
│  From:  Demo > Test Cases > Accounts                    (3)  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Remove    Escape Cancel                          (4)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — names the node.
2. **What it holds** — the line the tester reads before pressing `Enter`. It
   is omitted when every count is zero.
3. **Where it is** — in gray.
4. The confirm key is named for what it does. It reads **Remove**, not **OK**.

For several nodes, the question is *Remove these N items?*. It shows no counts
and no path.

**Scenario 36 · Remove asks first**
Keeps rule 6 and rule 33.
> **Given** one or more removable nodes are selected
> **When** the tester presses `Delete`, or chooses **Actions → Remove**
> **Then** the **Confirm Removing** dialog opens
> **And** for one node it asks *Remove '\<name\>'?*
> **And** it says how many test sets, test cases and test runs that node holds
> **And** it shows where the node is, after *From:*
> **And** for several nodes it asks *Remove these N items?*, where N is how many

**Scenario 37 · Confirm**
Keeps rule 11 and rule 34.
> **When** the tester presses `Enter`
> **Then** each node's editor is closed
> **And** a copy is kept for undo
> **And** the node goes to the desktop's recycle bin, and its automation code goes with it
> **And** the tree rebuilds
> **And** *Removed* is shown, or *Removed N* for several, counting only what actually went

**Scenario 38 · Cancel**
Keeps rule 6.
> **When** the tester presses `Escape`
> **Then** the dialog closes and nothing is removed

**Scenario 39 · Containers are never removed**
Keeps rule 36.
> **Given** only **Test Cases** or **Test Runs** is selected
> **Then** **Remove** is grayed, and `Delete` does nothing
>
> **Given** a container is selected together with a test set
> **Then** only the test set is removed
> **And** the container is left out, and not counted

---

## Arranging the tree

### Use case 9 · Move or copy nodes

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

#### Paste, Move and Copy

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

**Scenario 40 · Copy**
Keeps rule 7.
> **Given** one or more test sets, packages or test runs are selected
> **When** the tester presses `Ctrl+C`, or chooses **Actions → Copy**
> **Then** they are placed on the clipboard for copying
> **And** nothing in the tree changes
> **And** *Copied* is shown, or *Copied N* for several

**Scenario 41 · Cut**
> **When** the tester presses `Ctrl+X`, or chooses **Actions → Cut**
> **Then** they are placed on the clipboard for moving
> **And** they are drawn gray in the tree
> **And** *Cut* is shown, or *Cut N* for several

**Scenario 42 · Paste asks, then moves or copies**
Keeps rule 6 and rule 40.
> **Given** a folder that can hold the clipboard's nodes is selected: **Test Cases**, **Test Runs**, or a package of the right family
> **When** the tester presses `Ctrl+V`, or chooses **Actions → Paste**
> **Then** the **Paste** dialog opens, with *From* and *To* rows
> **And** after a cut it asks *Move '\<name\>' into '\<folder\>'?*
> **And** after a copy it asks *Copy N items into '\<folder\>'?*
> **When** the tester presses `Enter`
> **Then** after a cut the nodes move, and *Moved* is shown, or *Moved N*
> **And** after a copy the nodes are duplicated, and *Pasted* is shown, or *Pasted N*

**Scenario 43 · A copy is a new thing**
Keeps rule 41.
> **Given** a test set was pasted after a copy
> **Then** every test case in the copy has a new identity
> **And** the copy is selected in the tree

**Scenario 44 · Refused: wrong place**
Keeps rule 37, rule 38, rule 39 and rule 13.
> **Given** the selected node cannot take what is on the clipboard. It is one of these:
>
> - a test set, or a test run
> - the node being pasted, or something inside that node
> - the folder the node already sits in
> - a node of the other family
> - a node in another test project
>
> **When** the tester presses `Ctrl+V`
> **Then** nothing moves
> **And** *Select a folder* is shown in red
> **And** on a test project or a test set, **Paste** is gray already, so nothing happens at all

**Scenario 45 · Refused: name taken**
Keeps rule 4.
> **Given** the destination already holds a node with the same name
> **When** the tester presses `Ctrl+V`
> **Then** that node stays where it is
> **And** *'\<name\>' already exists in '\<folder\>'* is shown in red, or *N items already exist in '\<folder\>'*
> **And** the other nodes in the same paste still move

**Scenario 46 · Drag and drop**
Keeps rule 40 and rule 43.
> **Given** the tester drags one or more nodes
> **Then** a small rounded label follows the pointer, reading *'\<name\>'* or *N items*
> **And** over a row that cannot take them, the mouse shows the no-entry pointer, and the row does not light up
> **When** the tester releases on a folder that can hold them
> **Then** the **Move** dialog asks the same question as [scenario 42](#use-case-9--move-or-copy-nodes), and `Enter` moves them
> **When** the tester holds `Ctrl` while releasing
> **Then** the dialog is titled **Copy**, and `Enter` copies them
> **And** nothing can be dropped between two rows

**Scenario 47 · Escape clears the gray**
> **Given** nodes were cut and are drawn gray
> **When** the tester presses `Escape` in the tree
> **Then** the gray is removed
> **But** the nodes stay on the clipboard
> **And** a later `Ctrl+V` still offers to move them, which is difference 6 in the business requirements

**Scenario 48 · Only one test project**
Keeps rule 13.
> **Given** nodes were cut in one test project, and the repository was then bound to another
> **When** the tester presses `Ctrl+V`
> **Then** *Select a folder* is shown, and nothing moves

### Use case 10 · Order nodes among their siblings

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

#### Order

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

1. **One field** — shows the node's current number, or nothing. It accepts
   digits only, with no leading zero. Anything else is refused as it is typed.
   Empty means date order.

**Scenario 49 · Give a node its number**
Keeps rule 44.
> **Given** a test set, a package or a test run is selected
> **When** the tester chooses **Actions → Order**
> **Then** the **Order** dialog opens with one field
> **And** the field shows the node's current number, or is empty, with the placeholder *1, 2, 3... or empty for date order*
> **When** the tester types a number from 1 up and presses `Enter`
> **Then** the node moves among its siblings, and *Ordered* is shown
> **When** the tester empties the field and presses `Enter`
> **Then** the number is removed, the node returns to date order, and *Ordered* is shown

**Scenario 50 · The field refuses the wrong characters as the tester types**
Keeps rule 44.
> **Given** the field is focused
> **When** the tester types a leading zero, a letter or a space
> **Then** the character does not appear
>
> **Given** the test project or a container is selected
> **Then** **Order** is grayed (rule 47)

### Use case 11 · Undo and redo a change to the tree

**As a** tester, **I want** to take back the last change I made to the tree,
**so that** a wrong move, rename or removal costs nothing.

**Rules**

- **Rule 48** — The tree remembers its own last 20 changes. It remembers them
  separately from any editor.
- **Rule 49** — Four things can be undone: a move, a rename, a removal, and an
  edit of a test run. Three cannot: an order number, a copy, and a status change.
- **Rule 50** — Making a new change forgets everything that was undone.

**Scenario 51 · Undo**
Keeps rule 48 and rule 49.
> **Given** the last tree change was a move, a rename, a removal or an edit of a test run
> **When** the tester presses `Ctrl+Z`, or chooses **Actions → Undo \<what\>**
> **Then** the change is reversed
> **And** moved nodes go back
> **And** a renamed node gets its old name
> **And** removed nodes are restored from the copy kept aside
> **And** *Undone* is shown
> **And** the menu entry names what it will undo, as in *Undo Move 'Login'* or *Undo Remove 3 items*

**Scenario 52 · Redo**
Keeps rule 50.
> **Given** something was just undone
> **When** the tester presses `Ctrl+Y`, or chooses **Actions → Redo \<what\>**
> **Then** it is re-applied, and *Redone* is shown
>
> **Given** a new change was made after the undo
> **Then** **Redo** is grayed

**Scenario 53 · Nothing to undo**
> **Given** the tree's history is empty
> **Then** the entry reads plain **Undo**, and is grayed
> **And** `Ctrl+Z` does nothing

**Scenario 54 · A removal that cannot be fully restored says so**
Keeps rule 11.
> **Given** some removed nodes can no longer be put back
> **When** the tester presses `Ctrl+Z`
> **Then** the rest are restored
> **And** *Undo Incomplete* is shown in red, with a line saying how many of them could not be put back

---

## Statuses

### Use case 12 · Retire and reactivate

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

**Scenario 55 · The tester picks a status from a short list on the menu**
Keeps rule 54.
> **Given** exactly one node is selected
> **When** the tester opens **Actions**
> **Then** the status entries for that node's kind are there
> **And** the status the node already has is grayed:
>
> | Node | Entries | Statuses |
> |---|---|---|
> | Test project | **Activate**, **Deactivate**, **Archive** | Active, Inactive, Archived |
> | Test set | **Mark Active**, **Mark Deprecated** | Active, Deprecated |
> | Package | **Mark Active**, **Archive** | Active, Archived |
>
> **When** the tester chooses one
> **Then** the status is written
> **And** the tree refreshes
> **And** the new status word is shown: *Active*, *Inactive*, *Archived* or *Deprecated*

**Scenario 56 · What retiring does**
Keeps rule 8, rule 51, rule 52 and rule 53.
> **Given** a test set is now **Deprecated**, or a package is now **Archived**
> **Then** it is drawn gray
> **And** it sorts last
> **And** **Expand All** leaves it collapsed
> **And** it is not offered when a test run is created
> **And** its test cases are skipped when a parent is run
> **And** nothing inside it is deleted
>
> **Given** the test project is now **Inactive** or **Archived**
> **Then** the test project row shows no children
> **And** if it is **Archived**, the next load skips it, and the panel shows *\<name\> is archived, so it is not opened*

### Use case 13 · Set a test run's status

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

#### Set Test Run Status

```
┌──────────────────────────────────────────────────────────────┐
│  Set Test Run Status                                         │
├──────────────────────────────────────────────────────────────┤
│  > +  Created                                           (1)  │
│    @  In Progress                                            │
│    *  Completed                                     2   (2)  │
│    !  Assigned                                      1        │
│    x  Closed                                        3        │
├──────────────────────────────────────────────────────────────┤
│  ↑ ↓ Select    Enter Choose    Escape Cancel            (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **Five rows, one per status** — each with its icon. It is the same icon the
   tree draws for a test run in that status.
2. **A key beside three of them** — *Created* and *In Progress* have none,
   because they are the test run's own record. The keys are not in order down
   the list, because the rows follow the status order instead: **Assigned** is
   `1`, **Completed** is `2`, **Closed** is `3`.
3. The first row is selected when the popup opens. The tester chooses in one
   of three ways:
   - `↑` `↓` and `Enter`
   - a key
   - a click

**Scenario 57 · Pick a status, by key or by row**
Keeps rule 56.
> **Given** a test run that is **Created**, **In Progress** or **Assigned** is selected
> **When** the tester chooses **Set Status**
> **Then** the **Set Test Run Status** popup lists the five statuses
> **And** three of them carry a key: `1` **Assigned**, `2` **Completed**, `3` **Closed**
> **When** the tester presses that key, or moves with `↑` `↓` and presses `Enter`, or clicks a row
> **Then** the test run's status is written
> **And** its icon and its gray status word in the tree both change
> **And** Testin shows the new status word
> **When** the tester clicks outside the popup
> **Then** it closes, and nothing changes

**Scenario 58 · Completed and Closed sign the test run off**
Keeps rule 55 and rule 9.
> **When** the status set is **Completed** or **Closed**
> **Then** every test case still **Pending** in the test run becomes **Untested**
> **And** Testin records the time the test run finished
> **And** from then on **Set Status**, **Edit Run** and **Run Tests** are grayed on it

### Use case 14 · Re-create a test run

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

The dialog is drawn under [use case 6](#use-case-6--create-a-test-run-or-a-test-run-package).

**Scenario 59 · The next cycle from the last**
Keeps rule 57, rule 58 and rule 59.
> **Given** a test run in any status is selected
> **When** the tester chooses **Actions → Re-create**
> **Then** the **Create Test Run** dialog opens, with the next name suggested: *cycle-1* becomes *cycle-2*
> **And** the same test cases are checked, and the same configuration is filled in
> **And** a test case removed from its test set since the last test run is simply not there
> **When** the tester presses **Create**
> **Then** a new test run is written, with every checked test case **Pending**
> **And** its editor opens
> **And** *Run created* is shown

**Scenario 60 · The same three refusals as creating a test run**
Keeps rule 27, rule 5 and rule 4.
> **Given** no test case is checked
> **Then** the **Create** button is disabled
>
> **Given** the name is empty
> **When** the tester presses **Create**
> **Then** *A test run needs a name* is shown, and the dialog stays open with everything typed still in it
>
> **Given** the name is already used
> **When** the tester presses **Create**
> **Then** *\<name\> Already Exists* is shown, and the dialog stays open with everything typed still in it

### Use case 15 · Edit a test run

**As a** tester, **I want** to change which test cases a test run covers, its
name and its configuration, **so that** a test run can be corrected without being
recreated.

**Rules**

- **Rule 60** — A signed-off test run cannot be edited. (rule 9)
- **Rule 61** — Removing a test case from a test run drops everything that test
  case recorded in that test run. Adding a test case adds it as **Pending**.
- **Rule 62** — An edit can be undone, as one step. (rule 49)

The dialog is drawn under [use case 6](#use-case-6--create-a-test-run-or-a-test-run-package).

**Scenario 61 · Change its test cases, name or configuration**
Keeps rule 61.
> **Given** a test run that is not signed off is selected
> **When** the tester chooses **Edit Run**
> **Then** the **Edit Test Run** dialog opens on the test run's own name, with its test cases checked and its configuration filled in
> **And** test cases added to a test set since the test run was created appear unchecked
> **When** the tester changes any of it and presses **Save**
> **Then** the test run is rewritten
> **And** Testin removes any test case the tester unticked, with everything the test run recorded about it
> **And** a newly checked test case is added as **Pending**
> **And** the tree refreshes, and *Updated* is shown

**Scenario 62 · A rename here renames the folder too**
Keeps rule 31.
> **Given** the name was changed
> **Then** Testin closes the test run's editor
> **And** renames its folder
> **And** only then saves the test run

**Scenario 63 · When Testin will not edit a test run**
Keeps rule 60 and rule 9.
> **Given** the test run is **Completed** or **Closed**
> **Then** **Edit Run** is grayed
>
> **Given** the test run was signed off from its editor while the dialog was open
> **When** the tester presses **Save**
> **Then** the dialog stays open
> **And** *'\<run\>' was Completed while this was open - nothing saved* is shown in red
>
> **Given** the test run was removed while the dialog was open
> **Then** *'\<run\>' no longer exists - nothing saved* is shown in red

**Scenario 64 · Undoable as one step**
Keeps rule 62.
> **Given** a test run was just edited
> **When** the tester presses `Ctrl+Z` in the tree
> **Then** the previous name, test cases and configuration are all back
> **And** *Undone* is shown

---

## Working from the tree

### Use case 16 · Run the automation for everything a node holds

**As a** tester, **I want** to run every automated test case under a test set, a
package or **Test Cases**, **so that** a whole area runs in one gesture.

**Rules**

- **Rule 63** — Running from a parent skips retired branches. Running a retired
  test set directly still runs it. (rule 8)
- **Rule 64** — A node with no test cases to run says so. It runs nothing.
- **Rule 65** — Running needs the automation plugin. Without it, the item is
  not offered.

**Scenario 65 · Run from a node**
Keeps rule 63.
> **Given** **Test Cases**, a test set package or a test set is selected, and the automation plugin is present
> **When** the tester chooses **Run Tests**
> **Then** every test case under the node runs, skipping retired branches
> **And** *Running* is shown, or *Running N*
> **And** running a retired test set directly still runs its test cases

**Scenario 66 · Nothing to run**
Keeps rule 64 and rule 65.
> **Given** the node holds no test case that can run
> **When** the tester chooses **Run Tests**
> **Then** nothing runs
> **And** *\<name\> has no test cases to run* is shown in red
>
> **Given** the automation plugin is absent
> **Then** **Run Tests** is not in the menu

### Use case 17 · Find anything in the project

**As a** tester, **I want** to type part of a name, a step or an id and jump to
it, **so that** a large tree is never a place to scroll.

**Rules**

- **Rule 66** — Nodes are found by name, from the first character. Test cases
  are searched from the second character, in every field they have.
- **Rule 67** — Choosing a result opens the tree at it. It opens its editor too.

#### Search Test Project

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Search Test Project                                                       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  [find]  Go to a test set or run, or search for anything...      (1)       │
│                                                                            │
│  [set]  Login                                                    (2)       │
│         Demo > Test Cases > Accounts > Login                               │
│  [run]  cycle-2                                                            │
│         Demo > Test Runs > Sprint 7 > cycle-2                              │
│  [tc]   Sign in with a correct username and password                       │
│         Demo > Test Cases > Accounts > Login                               │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  Enter Go To    ↑ ↓ Select    Escape Cancel                                │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The field** — its gray hint text reads *Go to a test set or run, or search
   for anything...*. Before the tester types, the list holds every test set and
   test run.
2. **A result row** — shows:
   - the icon
   - the name
   - under them, the path through the tree
3. At most 50 rows.

**Scenario 67 · Search from anywhere**
Keeps rule 66.
> **When** the tester presses `Ctrl+Alt+F` anywhere in the IDE, or presses the **Search Test Project** button in the panel header
> **Then** the **Search Test Project** dialog opens
> **And** before anything is typed, it lists every test set and test run
> **When** the tester types
> **Then** Testin lists every node whose name contains what was typed, starting from the first character typed
> **And** it lists test cases too, starting from the second character, matching any part of the test case
> **And** at most 50 rows are shown
> **And** each row shows the icon, the name, and under it the path *\<project\> > Test Cases > …*

**Scenario 68 · Go to it**
Keeps rule 67.
> **When** the tester moves with `↑` `↓` and presses `Enter` on a node
> **Then** the panel comes forward
> **And** the tree expands to that node and selects it
> **And** its editor opens
> **When** the row is a test case
> **Then** its test set's editor opens, with that test case selected

### Use case 18 · Refresh the tree from disk

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

**Scenario 69 · Refresh**
Keeps rule 68 and rule 69.
> **When** the tester presses the **Refresh** button in the panel header
> **Then** the repository's binding is re-read
> **And** Testin reads the test project again, showing a progress bar reading *Testin indexing - \<project\>*
> **And** editors on nodes that are gone are closed, and the rest are reloaded
> **And** the tree redraws, with the same rows expanded
> **And** *Refreshed* is shown when it finishes

**Scenario 70 · One at a time**
Keeps rule 70.
> **Given** a refresh is already running
> **When** the tester presses **Refresh** again
> **Then** nothing happens

**Scenario 71 · Expand and collapse**
> **When** the tester presses **Expand All**
> **Then** every node opens, except retired ones, which stay collapsed
> **When** the tester presses **Collapse All**
> **Then** every row under the test project collapses
> **And** the test project, **Test Cases** and **Test Runs** stay visible

### Use case 19 · Switch the Git branch of the test project

**As a** tester, **I want** to switch the test project's branch from the panel,
**so that** the tree follows the branch I am testing.

**Rules**

- **Rule 71** — The branch box appears only for a test project shared through
  Git.
- **Rule 72** — Switching with uncommitted changes asks first. Switching never
  loses them.
- **Rule 73** — A switch that succeeds reloads the tree from the new branch.

#### Uncommitted Changes

```
┌──────────────────────────────────────────────────────────────┐
│  Uncommitted Changes                                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  3 changes in this test project are not committed.       (1) │
│  Switching does not leave them behind - they come with       │
│  you, and can be committed onto release by mistake.          │
│                                                              │
│  From:  main                                             (2) │
│  To:    release                                              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Switch Anyway   Shift+Enter Review Changes        (3) │
│  Escape Cancel                                               │
└──────────────────────────────────────────────────────────────┘
```

1. **How many changes** — and the one sentence that matters. Switching does
   not leave the changes behind. They come with the tester. They can be
   committed onto the wrong branch by mistake.
2. **From** and **To** — the branch the tester is on, and the branch they
   picked.
3. **Two confirms** — `Enter` switches anyway. `Shift+Enter` opens the review
   instead.

**Scenario 72 · The branch box**
Keeps rule 71.
> **Given** the test project is shared through Git, and its folder is a Git repository
> **Then** a drop-down box above the tree shows its branches, with the current one selected
>
> **Given** the test project is not shared through Git
> **Then** there is no box

**Scenario 73 · Switch**
Keeps rule 73.
> **When** the tester picks another branch
> **Then** the branch is checked out
> **And** the tree reloads from it
> **And** *Switched to \<branch\>* is shown

**Scenario 74 · Uncommitted changes ask first**
Keeps rule 72.
> **Given** the test project has uncommitted changes
> **When** the tester picks another branch
> **Then** the box goes back to the branch the tester was on
> **And** the **Uncommitted Changes** dialog says how many changes there are, and that they would move to the new branch too
> **When** the tester presses `Enter`, which is **Switch Anyway**
> **Then** the switch runs as above
> **When** the tester presses `Shift+Enter`, which is **Review Changes**
> **Then** the list of changes not yet committed opens instead
> **When** the tester presses `Escape`
> **Then** nothing changes

### Use case 20 · See what a node holds

**As a** tester, **I want** to see a node's counts, dates, status and verdict
breakdown without opening anything, **so that** I can size a branch of the tree
at a glance.

**Rules**

- **Rule 74** — Opening **Details** changes nothing, so Testin says nothing.
  (rule 7)
- **Rule 75** — Testin counts what a node holds when the tester asks. It never
  saves the number.

**Scenario 75 · Details**
Keeps rule 74 and rule 75.
> **Given** any node is selected
> **When** the tester chooses **Details**
> **Then** the **Details** dialog shows the node's name and its path
> **And** it shows who created it, who last changed it, and when
> **And** it shows its status, and what it holds, counted now and never stored
> **And** it shows a verdict chart
> **When** the tester presses `Escape`
> **Then** it closes. Nothing was changed, and nothing is announced

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
| **Question 1** | May a test run go backwards, from **Assigned** to **Created**, or from **In Progress** to **Created**? | Nothing prevents it today (difference 5). The same question for the whole product is [question 1 in the product's own document](product.md#9-undecided). |
| **Question 2** | Should a signed-off test run be locked in the tree, as well as in its editor? | Today it is not (difference 3). Its own description says its name must not change. The tree's rename does not check. |
| **Question 3** | What should the paste refusal say? | *Select a folder* is shown for six different reasons. Four of them are: the wrong side of the tree, the node itself, another test project, and a test run. Choosing a different folder only fixes one of the six. |
| **Question 4** | On a Mac, should cut, paste, undo and redo in the tree use the Mac's own key for those actions, the way copy already does? | Today copy does. The other four do not. The keys are named in the key table above. |

---

[Documentation](../README.md) › **The project panel**
