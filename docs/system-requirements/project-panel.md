[Documentation](../README.md) › [System requirements](system-requirements.md) › Project panel

# Project panel — system requirements

This document says what the panel does, step by step. Every key the panel
answers to is named here, once, in the step that presses it.

| | |
|---|---|
| **Area** | [System requirements](system-requirements.md) |
| **Part of Testin** | The project panel, the tree on the left |
| **Numbering** | Its scenarios are numbered `SR-PP-01` and up. The `PP` is short for the project panel, so a number from this document can never be confused with one from another |
| **Answers** | Exactly what happens, and what the tester sees, for every gesture in the tree |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every key, label and message read from the code that shows it |
| **Read with** | [Business requirements](../business-requirements/project-panel.md) — why, and every rule cited here · [Design](../design/project-panel.md) — every screen |
| **Written to** | [How a document is written](../standard.md). Every key the panel answers to is here, once |

---

## Every key, in one place

Each row links to the scenario that owns the key. A key works only while the
tree has focus. The search key is the exception. It works anywhere in the IDE.

| Key | Does | Scenario |
|---|---|---|
| `Enter` | Opens the selected test set or test run | [SR-PP-19](#uc-pp-04--open-a-test-set-or-a-test-run) |
| `Ctrl+M` | Creates a node under the selected one | [SR-PP-22](#uc-pp-05--create-a-test-set-or-a-test-set-package) |
| `Shift+F6` | Renames the selected node | [SR-PP-32](#uc-pp-07--rename-a-node) |
| `Delete` | Removes the selected nodes | [SR-PP-36](#uc-pp-08--remove-a-node) |
| `Ctrl+C` | Copies the selected nodes | [SR-PP-40](#uc-pp-09--move-or-copy-nodes) |
| `Ctrl+X` | Cuts the selected nodes | [SR-PP-41](#uc-pp-09--move-or-copy-nodes) |
| `Ctrl+V` | Pastes into the selected node | [SR-PP-42](#uc-pp-09--move-or-copy-nodes) |
| `Escape` | Clears the gray from cut nodes | [SR-PP-47](#uc-pp-09--move-or-copy-nodes) |
| `Ctrl+Z` | Undoes the last tree change | [SR-PP-51](#uc-pp-11--undo-and-redo-a-change-to-the-tree) |
| `Ctrl+Y` | Redoes it | [SR-PP-52](#uc-pp-11--undo-and-redo-a-change-to-the-tree) |
| `1` `2` `3` | Inside the status popup: Assigned, Completed, Closed | [SR-PP-57](#uc-pp-13--set-a-test-runs-status) |
| `Ctrl+Alt+F` | Opens search, from anywhere in the IDE | [SR-PP-67](#uc-pp-17--find-anything-in-the-project) |
| Context Menu key | Opens the node menu | [SR-PP-09](#the-tree-itself) |

Six things a tester might expect have **no key** on the tree. Order, the
statuses, Re-create, Edit Run, Run Tests and Details are menu items only. Each
one is a considered choice, not a reflex. None of them is pressed often enough
to earn a key. `F2` renames in the editors, not here.

---

## The tree itself

**SR-PP-01 · What a row shows**
> **Given** the tree is showing a test project
> **Then** the test project is the top row, drawn bold, with the IDE's project icon
> **And** under it sit exactly two bold rows, **Test Cases** and **Test Runs**, each with a bookmark icon
> **And** a package shows a folder icon
> **And** a test set shows a changelist icon
> **And** a test run shows the icon of its status, instead of a kind icon
> **And** after a test run's name comes its status word in gray: *Created*, *In Progress*, *Assigned*, *Completed* or *Closed*

**SR-PP-02 · A retired or cut node is gray** — BR-PP-08
> **Given** a test set is **Deprecated**, or a package is **Archived**, or a node was cut and not yet pasted
> **Then** its name is drawn in gray
> **And** a retired node sits after every live sibling

**SR-PP-03 · Siblings are in one order** — BR-PP-10
> **Given** a folder holds several nodes
> **Then** live nodes come first, and retired ones last
> **And** inside each of those two groups, nodes with a number come first, smallest first
> **And** then come the nodes without a number, by the date they were created
> **And** then by name

**SR-PP-04 · A test project that is not Active shows nothing under it** — BR-PP-52
> **Given** the test project's status is **Inactive** or **Archived**
> **Then** the test project row shows no children at all

**SR-PP-05 · Right-click selects, then opens the menu**
> **Given** the tester right-clicks a row that is not selected
> **Then** that row becomes the only selection
> **And** the menu opens at the pointer
>
> **Given** the tester right-clicks a row inside a multi-selection
> **Then** the selection is kept
> **And** the menu acts on all of it

**SR-PP-06 · Double-click opens**
> **Given** the tester double-clicks a test set or a test run
> **Then** it opens in its editor, the same as [SR-PP-19](#uc-pp-04--open-a-test-set-or-a-test-run)

**SR-PP-07 · A folder that cannot be read says so**
> **Given** a folder's children cannot be loaded
> **Then** it shows one child row with the error icon
> **And** that row reads, in red, *Could not load '\<folder name\>'*

**SR-PP-08 · The tree remembers what was open**
> **Given** the tree is rebuilt after a paste, a rename or a refresh of the same test project
> **Then** the rows that were expanded stay expanded
> **And** the selection stays
>
> **Given** a different test project is shown for the first time
> **Then** the whole tree is expanded, except retired branches

**SR-PP-09 · The keyboard opens the menu**
> **Given** a row is selected and the tree has focus
> **When** the tester presses the keyboard's Context Menu key
> **Then** the node menu opens over the selected row

---

## Getting to the tree

### UC-PP-01 · Open the panel and reach the tree

**SR-PP-10 · One of five screens, in a fixed order** — BR-PP-14
> **Given** the tester opens the **Testin** tool window
> **Then** the panel shows exactly one of these, checked in this order:
>
> | If | The panel shows |
> |---|---|
> | No Testin folder is set | *Welcome to Testin* and the link **Configure Testin settings** |
> | The bound test project is found | The tree |
> | This repository names a test project that is not on disk, and knows its Git address | *\<name\> is not on this machine yet* and the link **Clone \<name\>** |
> | No test project exists under the Testin folder | The link **Create your first test project** |
> | Otherwise | One link per test project, reading *\<name\>  \<Active\|Inactive\|Archived\>*. With more than six test projects, one link instead: **Select the test project for this repository** |

**SR-PP-11 · One test project binds itself** — BR-PP-15
> **Given** this repository names no test project, and exactly one test project exists under the Testin folder
> **Then** the panel binds to it without asking
> **And** the panel shows its tree

**SR-PP-12 · An archived test project is refused with a reason** — BR-PP-53
> **Given** this repository is bound to a test project whose status is **Archived**
> **Then** the panel shows, in red, *\<name\> is archived, so it is not opened*
> **And** under that line sits the list of test projects to choose from

### UC-PP-02 · Create a test project

**SR-PP-13 · By name**
> **Given** the Testin folder is set
> **When** the tester presses the **New Test Project** button in the panel header, or the welcome link **Create your first test project**
> **And** types a name and presses `Enter`
> **Then** the test project folder is created under the Testin folder
> **And** this repository is bound to it
> **And** the tree appears
> **And** the message *Project created* is shown

**SR-PP-14 · A name already used is refused** — BR-PP-04
> **Given** a folder with that name already exists under the Testin folder
> **When** the tester presses `Enter`
> **Then** nothing is created
> **And** the message *\<name\> Already Exists* is shown in red

**SR-PP-15 · By Git address** — BR-PP-18
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

### UC-PP-03 · Choose which test project this repository uses

**SR-PP-16 · Choose from the list** — BR-PP-19
> **Given** the Testin folder is set
> **When** the tester presses the **Select Test Project** button in the panel header
> **Then** the **Select Test Project** dialog lists every test project under the folder, with its status
> **And** the current test project is selected
> **When** the tester selects one and presses `Enter`
> **Then** the binding is written into this repository
> **And** the tree reloads on that test project
> **And** the message *Bound* is shown, with the test project's name

**SR-PP-17 · No test projects to choose from**
> **Given** no test project folder exists under the Testin folder
> **When** the tester presses **Select Test Project**
> **Then** no dialog opens
> **And** the message *No Test Projects*, with the line *Create one under the Testin root first*, is shown in red

**SR-PP-18 · A binding that cannot be written is not announced** — BR-PP-20
> **Given** the repository's configuration file cannot be written
> **When** the tester chooses a test project
> **Then** an error titled *Not Bound* says the choice will not be remembered
> **And** the dialog stays open

---

## Nodes

### UC-PP-04 · Open a test set or a test run

**SR-PP-19 · Open with Enter** — BR-PP-21
> **Given** a test set or a test run is selected
> **When** the tester presses `Enter`, or chooses **Open** from the menu
> **Then** it opens in its editor tab, named after the node
> **And** nothing is announced, because opening only moves the view (BR-PP-07)

**SR-PP-20 · Already open comes forward** — BR-PP-22
> **Given** the node's editor tab is already open
> **When** the tester presses `Enter`
> **Then** that tab comes to the front
> **And** no second tab opens

**SR-PP-21 · Nothing to open** — BR-PP-21
> **Given** a package, a container or the test project is selected
> **Then** **Open** is grayed, and `Enter` does nothing

### UC-PP-05 · Create a test set or a test set package

**SR-PP-22 · From the tree** — BR-PP-23
> **Given** **Test Cases** or a test set package is selected
> **When** the tester presses `Ctrl+M`, or chooses **Create**
> **Then** the **Create Test Node** dialog opens, with two kinds to pick from
> **And** the kinds are a test set, hinted *Holds test cases*, and a test set package, hinted *Groups test sets*
> **And** the first kind is selected

**SR-PP-23 · Name it and pick the kind**
> **Given** the dialog is open
> **When** the tester types a name, moves between the two kinds with `↑` and `↓`, and presses `Enter`
> **Then** the node is created under the selected parent
> **And** the tree refreshes
> **And** the message *Created* is shown

**SR-PP-24 · A new test set opens and gets its code** — BR-PP-25
> **Given** the kind chosen was a test set
> **Then** it opens in its editor at once
> **And** where the Java plugin is present, its automation test class is generated

**SR-PP-25 · A name is required** — BR-PP-05
> **Given** the name field is empty
> **When** the tester presses `Enter`
> **Then** the dialog stays open
> **And** the placeholder turns red
> **And** focus stays in the field

**SR-PP-26 · A sibling with that name refuses it** — BR-PP-04
> **Given** a node with that name already exists under the parent
> **When** the tester presses `Enter`
> **Then** the dialog closes, and nothing is created
> **And** *\<name\> Already Exists* is shown in red

**SR-PP-27 · Nowhere to create** — BR-PP-24
> **Given** the test project, a test set or a test run is selected
> **Then** **Create** is grayed, and `Ctrl+M` does nothing

### UC-PP-06 · Create a test run or a test run package

**SR-PP-28 · From the tree** — BR-PP-26
> **Given** **Test Runs** or a test run package is selected
> **When** the tester presses `Ctrl+M`, or chooses **Create**
> **Then** the **Create Run Node** dialog opens, with two kinds
> **And** the kinds are a test run, hinted *Records execution results*, and a test run package, hinted *Groups test runs*
> **When** the kind is a package, and the tester presses `Enter`
> **Then** the package is created, and *Created* is shown

**SR-PP-29 · A test run is configured before it exists** — BR-PP-27, BR-PP-28
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

**SR-PP-30 · A test run needs test cases and a name** — BR-PP-27, BR-PP-05
> **Given** no test case is checked
> **Then** the **Create** button is disabled
>
> **Given** the name has been emptied
> **When** the tester presses **Create**
> **Then** the dialog stays open
> **And** *A test run needs a name* is shown in red

**SR-PP-31 · The world moved while the dialog was open**
> **Given** the parent folder was removed while the dialog was open
> **When** the tester presses **Create**
> **Then** the dialog stays open
> **And** *'\<parent\>' no longer exists - test run not created* is shown in red

### UC-PP-07 · Rename a node

**SR-PP-32 · Rename with Shift+F6**
> **Given** a test set, a package or a test run is selected
> **When** the tester presses `Shift+F6`, or chooses **Actions → Rename**
> **Then** the **Rename** dialog opens, with the current name filled in and selected
> **When** the tester types the new name and presses `Enter`
> **Then** the node is renamed
> **And** the tree refreshes
> **And** *Renamed* is shown

**SR-PP-33 · The code is renamed first** — BR-PP-31
> **Given** the node is a test set or a test set package, and the Java plugin is present
> **When** the rename runs
> **Then** its automation code is renamed before the folder is
> **And** its open editor, if it has one, is closed first

**SR-PP-34 · Refusals** — BR-PP-04, BR-PP-05, BR-PP-30
> **Given** the name is empty
> **Then** the placeholder turns red, and the dialog stays open
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

**SR-PP-35 · Undoable** — BR-PP-32
> **Given** a node was just renamed
> **When** the tester presses `Ctrl+Z`
> **Then** the old name is back, and *Undone* is shown

### UC-PP-08 · Remove a node

**SR-PP-36 · Remove asks first** — BR-PP-06, BR-PP-33
> **Given** one or more removable nodes are selected
> **When** the tester presses `Delete`, or chooses **Actions → Remove**
> **Then** the **Confirm Removing** dialog opens
> **And** for one node it asks *Remove '\<name\>'?*, says *Holds N test sets, N test cases and N test runs*, and shows *From: \<path\>*
> **And** for several nodes it asks *Remove these N items?*

**SR-PP-37 · Confirm** — BR-PP-11, BR-PP-34
> **When** the tester presses `Enter`
> **Then** each node's editor is closed
> **And** a copy is kept for undo
> **And** the node goes to the desktop's recycle bin, and its automation code goes with it
> **And** the tree rebuilds
> **And** *Removed* is shown, or *Removed N* for several, counting only what actually went

**SR-PP-38 · Cancel** — BR-PP-06
> **When** the tester presses `Escape`
> **Then** the dialog closes and nothing is removed

**SR-PP-39 · Containers are never removed** — BR-PP-36
> **Given** only **Test Cases** or **Test Runs** is selected
> **Then** **Remove** is grayed, and `Delete` does nothing
>
> **Given** a container is selected together with a test set
> **Then** only the test set is removed
> **And** the container is left out, and not counted

---

## Arranging the tree

### UC-PP-09 · Move or copy nodes

**SR-PP-40 · Copy** — BR-PP-07
> **Given** one or more test sets, packages or test runs are selected
> **When** the tester presses `Ctrl+C`, or chooses **Actions → Copy**
> **Then** they are placed on the clipboard for copying
> **And** nothing in the tree changes
> **And** *Copied* is shown, or *Copied N* for several

**SR-PP-41 · Cut**
> **When** the tester presses `Ctrl+X`, or chooses **Actions → Cut**
> **Then** they are placed on the clipboard for moving
> **And** they are drawn gray in the tree
> **And** *Cut* is shown, or *Cut N* for several

**SR-PP-42 · Paste asks, then moves or copies** — BR-PP-06, BR-PP-40
> **Given** a folder that can hold the clipboard's nodes is selected: **Test Cases**, **Test Runs**, or a package of the right family
> **When** the tester presses `Ctrl+V`, or chooses **Actions → Paste**
> **Then** the **Paste** dialog opens, with *From* and *To* rows
> **And** after a cut it asks *Move '\<name\>' into '\<folder\>'?*
> **And** after a copy it asks *Copy N items into '\<folder\>'?*
> **When** the tester presses `Enter`
> **Then** after a cut the nodes move, and *Moved* is shown, or *Moved N*
> **And** after a copy the nodes are duplicated, and *Pasted* is shown, or *Pasted N*

**SR-PP-43 · A copy is a new thing** — BR-PP-41
> **Given** a test set was pasted after a copy
> **Then** every test case in the copy has a new identity
> **And** the copy is selected in the tree

**SR-PP-44 · Refused: wrong place** — BR-PP-37, BR-PP-38, BR-PP-39, BR-PP-13
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
> **And** on the test project or a test set, **Paste** is grayed before that

**SR-PP-45 · Refused: name taken** — BR-PP-04
> **Given** the destination already holds a node with the same name
> **When** the tester presses `Ctrl+V`
> **Then** that node stays where it is
> **And** *'\<name\>' already exists in '\<folder\>'* is shown in red, or *N items already exist in '\<folder\>'*
> **And** other nodes in the same paste that can land still do

**SR-PP-46 · Drag and drop** — BR-PP-40, BR-PP-43
> **Given** the tester drags one or more nodes
> **Then** a small rounded label follows the pointer, reading *'\<name\>'* or *N items*
> **And** over a row where nothing being dragged can land, the pointer shows no-drop, and no row highlights
> **When** the tester releases on a folder that can hold them
> **Then** the **Move** dialog asks the same question as [SR-PP-42](#uc-pp-09--move-or-copy-nodes), and `Enter` moves them
> **When** the tester holds `Ctrl` while releasing
> **Then** the dialog is titled **Copy**, and `Enter` copies them
> **And** nothing can be dropped between two rows

**SR-PP-47 · Escape clears the gray**
> **Given** nodes were cut and are drawn gray
> **When** the tester presses `Escape` in the tree
> **Then** the gray is removed
> **But** the nodes stay on the clipboard
> **And** a later `Ctrl+V` still offers to move them. This is D6 in the business requirements

**SR-PP-48 · Only one test project** — BR-PP-13
> **Given** nodes were cut in one test project, and the repository was then bound to another
> **When** the tester presses `Ctrl+V`
> **Then** *Select a folder* is shown, and nothing moves

### UC-PP-10 · Order nodes among their siblings

**SR-PP-49 · Give a node its number** — BR-PP-44
> **Given** a test set, a package or a test run is selected
> **When** the tester chooses **Actions → Order**
> **Then** the **Order** dialog opens with one field
> **And** the field shows the node's current number, or is empty, with the placeholder *1, 2, 3... or empty for date order*
> **When** the tester types a number from 1 up and presses `Enter`
> **Then** the node moves among its siblings, and *Ordered* is shown
> **When** the tester empties the field and presses `Enter`
> **Then** the number is removed, the node returns to date order, and *Ordered* is shown

**SR-PP-50 · The field refuses as you type** — BR-PP-44
> **Given** the field is focused
> **When** the tester types a leading zero, a letter or a space
> **Then** the character does not appear
>
> **Given** the test project or a container is selected
> **Then** **Order** is grayed (BR-PP-47)

### UC-PP-11 · Undo and redo a change to the tree

**SR-PP-51 · Undo** — BR-PP-48, BR-PP-49
> **Given** the last tree change was a move, a rename, a removal or an edit of a test run
> **When** the tester presses `Ctrl+Z`, or chooses **Actions → Undo \<what\>**
> **Then** the change is reversed
> **And** moved nodes go back
> **And** a renamed node gets its old name
> **And** removed nodes are restored from the copy kept aside
> **And** *Undone* is shown
> **And** the menu entry names what it will undo, as in *Undo Move 'Login'* or *Undo Remove 3 items*

**SR-PP-52 · Redo** — BR-PP-50
> **Given** something was just undone
> **When** the tester presses `Ctrl+Y`, or chooses **Actions → Redo \<what\>**
> **Then** it is re-applied, and *Redone* is shown
>
> **Given** a new change was made after the undo
> **Then** **Redo** is grayed

**SR-PP-53 · Nothing to undo**
> **Given** the tree's history is empty
> **Then** the entry reads plain **Undo**, and is grayed
> **And** `Ctrl+Z` does nothing

**SR-PP-54 · A removal that cannot be fully restored says so** — BR-PP-11
> **Given** some removed nodes can no longer be put back
> **When** the tester presses `Ctrl+Z`
> **Then** the rest are restored
> **And** *Undo Incomplete*, with the line *N of M could not be put back*, is shown in red

---

## Statuses

### UC-PP-12 · Retire and reactivate

**SR-PP-55 · A status is one of a fixed few, set from the menu** — BR-PP-54
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

**SR-PP-56 · What retiring does** — BR-PP-08, BR-PP-51, BR-PP-52, BR-PP-53
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

### UC-PP-13 · Set a test run's status

**SR-PP-57 · Pick a status, by key or by row** — BR-PP-56
> **Given** a test run that is **Created**, **In Progress** or **Assigned** is selected
> **When** the tester chooses **Set Status**
> **Then** the **Set Test Run Status** popup lists the five statuses
> **And** three of them carry a key: `1` **Assigned**, `2` **Completed**, `3` **Closed**
> **When** the tester presses that key, or moves with `↑` `↓` and presses `Enter`, or clicks a row
> **Then** the test run's status is written
> **And** its icon and its gray status word in the tree both change
> **And** the status word is shown
> **When** the tester clicks outside the popup
> **Then** it closes, and nothing changes

**SR-PP-58 · Completed and Closed sign the test run off** — BR-PP-55, BR-PP-09
> **When** the status set is **Completed** or **Closed**
> **Then** every test case still **Pending** in the test run becomes **Untested**
> **And** the test run's end time is stamped
> **And** from then on **Set Status**, **Edit Run** and **Run Tests** are grayed on it

### UC-PP-14 · Re-create a test run

**SR-PP-59 · The next cycle from the last** — BR-PP-57, BR-PP-58, BR-PP-59
> **Given** a test run in any status is selected
> **When** the tester chooses **Actions → Re-create**
> **Then** the **Create Test Run** dialog opens, with the next name suggested: *cycle-1* becomes *cycle-2*
> **And** the same test cases are checked, and the same configuration is filled in
> **And** a test case removed from its test set since the last test run is simply not there
> **When** the tester presses **Create**
> **Then** a new test run is written, with every checked test case **Pending**
> **And** its editor opens
> **And** *Run created* is shown

**SR-PP-60 · The same refusals as creating** — BR-PP-27, BR-PP-05, BR-PP-04
> The same three refusals apply here as when a test run is created. With no test case checked, the **Create** button is disabled. An empty name shows *A test run needs a name*. A name already used shows *\<name\> Already Exists*. Each one leaves the dialog open, with everything typed still in it.

### UC-PP-15 · Edit a test run

**SR-PP-61 · Change its test cases, name or configuration** — BR-PP-61
> **Given** a test run that is not signed off is selected
> **When** the tester chooses **Edit Run**
> **Then** the **Edit Test Run** dialog opens on the test run's own name, with its test cases checked and its configuration filled in
> **And** test cases added to a test set since the test run was created appear unchecked
> **When** the tester changes any of it and presses **Save**
> **Then** the test run is rewritten
> **And** an unchecked test case is dropped, with everything it recorded
> **And** a newly checked test case is added as **Pending**
> **And** the tree refreshes, and *Updated* is shown

**SR-PP-62 · A rename here renames the folder too** — BR-PP-31
> **Given** the name was changed
> **Then** the test run's editor is closed
> **And** its folder is renamed
> **And** only then is the test run written

**SR-PP-63 · Refusals** — BR-PP-60, BR-PP-09
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

**SR-PP-64 · Undoable as one step** — BR-PP-62
> **Given** a test run was just edited
> **When** the tester presses `Ctrl+Z` in the tree
> **Then** the previous name, test cases and configuration are all back
> **And** *Undone* is shown

---

## Working from the tree

### UC-PP-16 · Run the automation for everything a node holds

**SR-PP-65 · Run from a node** — BR-PP-63
> **Given** **Test Cases**, a test set package or a test set is selected, and the automation plugin is present
> **When** the tester chooses **Run Tests**
> **Then** every test case under the node runs, skipping retired branches
> **And** *Running* is shown, or *Running N*
> **And** running a retired test set directly still runs its test cases

**SR-PP-66 · Nothing to run** — BR-PP-64, BR-PP-65
> **Given** the node holds no test case that can run
> **When** the tester chooses **Run Tests**
> **Then** nothing runs
> **And** *\<name\> has no test cases to run* is shown in red
>
> **Given** the automation plugin is absent
> **Then** **Run Tests** is not in the menu

### UC-PP-17 · Find anything in the project

**SR-PP-67 · Search from anywhere** — BR-PP-66
> **When** the tester presses `Ctrl+Alt+F` anywhere in the IDE, or presses the **Search Test Project** button in the panel header
> **Then** the **Search Test Project** dialog opens
> **And** before anything is typed, it lists every test set and test run
> **When** the tester types
> **Then** nodes whose name contains the text are listed, matched from the first character
> **And** test cases are listed too, matched from the second character, in any field they have
> **And** at most 50 rows are shown
> **And** each row shows the icon, the name, and under it the path *\<project\> > Test Cases > …*

**SR-PP-68 · Go to it** — BR-PP-67
> **When** the tester moves with `↑` `↓` and presses `Enter` on a node
> **Then** the panel comes forward
> **And** the tree expands to that node and selects it
> **And** its editor opens
> **When** the row is a test case
> **Then** its test set's editor opens, with that test case selected

### UC-PP-18 · Refresh the tree from disk

**SR-PP-69 · Refresh** — BR-PP-68, BR-PP-69
> **When** the tester presses the **Refresh** button in the panel header
> **Then** the repository's binding is re-read
> **And** the test project is re-indexed, under a progress bar reading *Testin indexing - \<project\>*
> **And** editors on nodes that are gone are closed, and the rest are reloaded
> **And** the tree redraws, with the same rows expanded
> **And** *Refreshed* is shown when it finishes

**SR-PP-70 · One at a time** — BR-PP-70
> **Given** a refresh is already running
> **When** the tester presses **Refresh** again
> **Then** nothing happens

**SR-PP-71 · Expand and collapse**
> **When** the tester presses **Expand All**
> **Then** every node opens, except retired ones, which stay collapsed
> **When** the tester presses **Collapse All**
> **Then** every row under the test project collapses
> **And** the test project, **Test Cases** and **Test Runs** stay visible

### UC-PP-19 · Switch the Git branch of the test project

**SR-PP-72 · The branch box** — BR-PP-71
> **Given** the test project is shared through Git, and its folder is a Git repository
> **Then** a drop-down box above the tree shows its branches, with the current one selected
>
> **Given** the test project is not shared through Git
> **Then** there is no box

**SR-PP-73 · Switch** — BR-PP-73
> **When** the tester picks another branch
> **Then** the branch is checked out
> **And** the tree reloads from it
> **And** *Switched to \<branch\>* is shown

**SR-PP-74 · Uncommitted changes ask first** — BR-PP-72
> **Given** the test project has uncommitted changes
> **When** the tester picks another branch
> **Then** the box snaps back
> **And** the **Uncommitted Changes** dialog says how many changes there are, and that they would come along
> **When** the tester presses `Enter`, which is **Switch Anyway**
> **Then** the switch runs as above
> **When** the tester presses `Shift+Enter`, which is **Review Changes**
> **Then** the pending-commits review opens instead
> **When** the tester presses `Escape`
> **Then** nothing changes

### UC-PP-20 · See what a node holds

**SR-PP-75 · Details** — BR-PP-74, BR-PP-75
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

Six items on the tree's menu belong to another part of Testin: reports, export,
import and sync. Their scenarios live there. They are **Export**, **Import**,
**Generate Report** (`Ctrl+P`), **Sync With Remote**, **View Pending Commits**
and **Sync With SFTP**.

---

[Documentation](../README.md) › [System requirements](system-requirements.md) › **Project panel** — read with the [business requirements](../business-requirements/project-panel.md) and the [design](../design/project-panel.md)
