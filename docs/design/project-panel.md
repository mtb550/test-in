[Documentation](../README.md) › [Design](design.md) › Project panel

# Project panel — design

This document shows what the tester sees. It covers:

- the panel
- the tree
- the menu
- every dialog the panel opens

Each screen is drawn. Its parts are numbered.

| | |
|---|---|
| **Area** | [Design](design.md) |
| **Module** | `PP` — Project panel |
| **Answers** | Where every part of the panel sits, what it looks like, and why it is shaped that way |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every label and layout read from the code that draws it |
| **Read with** | [Business requirements](../business-requirements/project-panel.md) — the rules · [System requirements](../system-requirements/project-panel.md) — what happens, step by step |
| **Written to** | [How a document is written](../standard.md). A key appears only where a screen draws it |

---

## The panel

The panel is the Testin tool window. It sits on the left. The header buttons
belong to the IDE's own tool-window header. The branch box and the tree belong
to Testin.

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
3. **Expand All** and **Collapse All** — on expand, retired branches stay
   collapsed. On collapse, the project and its two containers stay visible.
   Part 10 says what retired means.
4. **Refresh** — re-reads which test project the repository is bound to. Then
   it re-indexes from disk.
5. **Select Test Project** and **New Test Project** — grayed until a Testin
   folder is set.
6. **The branch box** — shown only for a test project shared through Git. Its
   placeholder reads *Loading branches...* until Git answers.
7. **The project row** — drawn bold, with the IDE's project icon. The 2
   containers under it are bold too. There are always exactly two containers.
8. **A package** — folder icon. **A test set** — changelist icon. Neither is
   bold.
9. **A test run** — draws its **status** as its icon. The status word follows
   the name in gray. So the tester reads the status of every test run without
   opening one. A test run never draws a kind icon.
10. **Gray text** — means retired, or cut and not yet pasted. Retired means a
    **Deprecated** test set or an **Archived** package. Both look the same on
    purpose. Both mean "not part of current work right now".

Retired nodes sit at the bottom of their folder. Their number does not change
that.

Used by every use case. The rendering rules are SR-PP-01 to SR-PP-04.

---

## Before there is a tree

Before there is a tree, the panel shows one of five screens. The screens come
in a fixed order. Each screen holds exactly one link. The link is the only step out of
that situation.

### No Testin folder

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

### Choose a project

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

1. **The reason** — in red. It is shown when the repository named a test
   project it could not use. The reason is one of:
   - *not under the Testin root*
   - *archived*
   - *could not be read*
2. **One link per project** — name, then status. The tester clicks a link.
   That binds this repository to the test project and shows its tree. With
   more than six test projects, the list becomes one link. That link opens the
   **Select Test Project** dialog.

The other two screens follow the same shape: a gray line saying what is true,
then the one link. They are:

- *\<name\> is not on this machine yet*, with **Clone \<name\>**
- **Create your first test project**

Used by UC-PP-01, UC-PP-02, UC-PP-03.

---

## The menu

The tester right-clicks any row, or presses the keyboard's Context Menu key.
The menu opens. What is grayed depends on the row. What is absent depends on
which optional plugins are installed.

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
4. **Export**, **Import** — module `EX`.
5. **Sync With Remote**, **View Pending Commits** — only when the Git plugin is
   present. Otherwise the whole section is absent, separator included.
6. **Sync With SFTP** — always present. Grayed unless the test project is
   shared over SFTP.
7. **Edit Run** and **Set Status** — a test run's own two entries. Grayed once
   the test run is signed off.
8. **Generate Report** and **Details** — last.

Every entry that changes something confirms itself once. The confirmation is
one past-tense word. Every entry that only shows something confirms nothing.

---

## The dialogs

Every dialog below is built on the same frame. The frame has:

- a title
- its fields
- a status bar naming each key the dialog answers to

`Enter` confirms. `Escape` cancels. A dialog that differs says so in its status
bar.

### Create Test Node

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

1. **The name** — placeholder *set name...*. The tester presses `Enter` with
   the field empty. The placeholder turns red. Focus stays here.
2. **The two kinds** — each with a hint. `↑` `↓` move between them. The field's
   leading icon follows. The tester clicks a row. The dialog submits with that
   kind.
3. **The status bar** — every key this dialog answers to.

**Create Run Node** is the twin dialog on the test run side. It is identical
except for two things. Its kinds are *test run* (*Records execution results*) and
*test run package* (*Groups test runs*). Its placeholder is *set name, like
Sprint 3 Cycle 1...*.

> **Today the two kind names are blank.** The rows show only their hints.
> This is D4 in the business requirements.

Used by UC-PP-05, UC-PP-06.

### Rename

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

Used by UC-PP-07.

### Confirm Removing

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
3. **Where it is** — muted.
4. The confirm key is named for what it does. It reads **Remove**, not **OK**.

For several nodes, the question is *Remove these N items?*. It shows no counts
and no path.

Used by UC-PP-08.

### Paste, Move and Copy

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
2. **From** and **To** — muted. They let the tester catch a wrong drop before
   pressing `Enter`.
3. The confirm key is named for the verb.

From a paste, the title is **Paste**. From a drop, the title is the verb:
**Move** or **Copy**. The dialog is the same otherwise.

Used by UC-PP-09.

### Order

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

Used by UC-PP-10.

### Set Test Run Status

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
2. **A key beside three of them** — *Created* and *In Progress* have none. They
   are the test run's own record. The keys read `2` `1` `3` down the list. The
   rows are in the product's status order, not key order.
3. The first row is selected when the popup opens. The tester chooses in one
   of three ways:
   - `↑` `↓` and `Enter`
   - a key
   - a click

Used by UC-PP-13.

### Create Test Run

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
2. **Configuration details** — a collapsible form. Its fields are:
   - change log
   - commit id
   - test type
   - platform
   - component
   - language
   - browser or device, only when platform and component call for it
3. **The cases** — a checkbox tree of every live, non-empty test set. For a
   new test run, all are checked. For a re-creation, only the previous cycle's
   test cases are checked.
4. **Create** — enabled only while at least one test case is checked. There is
   no `Enter` to submit. The button is the only way to submit.

**Edit Test Run** is this same dialog with the button **Save**. It opens filled
with the test run's own:

- name
- test cases
- configuration

Used by UC-PP-06, UC-PP-14, UC-PP-15.

### Search Test Project

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

1. **The field** — placeholder *Go to a test set or run, or search for
   anything...*. Before the tester types, the list holds every test set and
   test run.
2. **A result row** — shows:
   - the icon
   - the name
   - under them, the path through the tree
3. At most 50 rows.

Used by UC-PP-17.

### Uncommitted Changes

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

Used by UC-PP-19.

---

## Decisions this design takes

### One project in the tree, not a project list

The panel shows the test project this repository is bound to, and no other.
With a list of test projects in the tree, three things could disagree about which
test project is open:

- the tree
- the reports
- the test runs

The binding lives in the repository. So a colleague who clones the repository
lands on the same test project, with no setup.

### A run is drawn as its status

Every other node draws what it *is*. A test run draws where it *stands*: its
status icon and the status word. That is the one thing a tester scanning the
tree wants to know about a test run. Without it, the tester opens each test run
to find out. This design saves that cost.

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
belongs to other modules. Everything that changes a node in place is one level
down, under **Actions**. So the first thing the tester sees is short. The
dangerous things take a deliberate second step.

### Order is a dialog, not a drag

Dropping between rows to reorder is not offered. A number:

- is explicit
- survives a re-index
- is the same number the generated automation carries as its execution order

A drag would be faster. It would say nothing.

---

## Where the build differs from this design

| Drawn | Built |
|---|---|
| The create dialog's rows show a kind name and a hint | The names are blank. Only the hints show. D4 in the business requirements |
| **Paste** grayed on a test run, as on a test set | Enabled. It always refuses. D2 |
| The status popup offers the three statuses a tester sets | It offers all five. It lets a test run go backwards. D5 |
| **New Test Project** re-enables once a Testin folder is set | It stays grayed until the IDE project is reopened |
| **Select Test Project** has a column heading over the names | The heading is blank. It is the same caption sweep as D4 |

---

[Documentation](../README.md) › [Design](design.md) › **Project panel** — read with the [business requirements](../business-requirements/project-panel.md) and the [system requirements](../system-requirements/project-panel.md)
