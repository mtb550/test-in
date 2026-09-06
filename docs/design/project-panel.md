# Project panel — design

What the tester sees: the panel, the tree, the menu, and every dialog the panel
opens. Each screen is drawn, and its parts are numbered.

| | |
|---|---|
| **Area** | [Design](design.md) |
| **Module** | `PP` — Project panel |
| **Answers** | Where every part of the panel sits, what it looks like, and why it is shaped that way |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `cddad453`, 6 September 2026 — every label and layout read from the code that draws it |
| **Written to** | [How a document is written](../standard.md). Behaviour is in the [system requirements](../system-requirements/project-panel.md); the rules in the [business requirements](../business-requirements/project-panel.md). Keys appear here only where a screen draws them |

---

## The panel

The Testin tool window, anchored on the left. The header buttons are the IDE's
own tool-window header; the branch box and the tree are Testin's.

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

1. **Search Test Project** — opens search. The one header button with a key,
   and the key works anywhere in the IDE.
2. **Settings** — the Testin page of the IDE settings.
3. **Expand All** and **Collapse All** — retired branches stay collapsed on
   expand; the project and its two containers stay visible on collapse.
4. **Refresh** — re-reads the repository's binding and re-indexes from disk.
5. **Select Test Project** and **New Test Project** — greyed until a Testin
   folder is set.
6. **The branch box** — only for a project shared through Git. Its placeholder
   reads *Loading branches...* until Git answers.
7. **The project row**, bold, with the IDE's project icon. The two containers
   under it are bold too, and always exactly two.
8. **A package** — folder icon. **A test set** — changelist icon. Neither is
   bold.
9. **A test run** draws its **status** as its icon, and the status word in gray
   after its name — so a tester reads where every run stands without opening
   one. A run never draws a kind icon.
10. **Gray text** means retired — a **Deprecated** set or an **Archived**
    package — or cut and not yet pasted. The two look the same, deliberately:
    both mean "not part of current work right now".

Retired nodes sit at the bottom of their folder, whatever number they carry.

Used by every use case. The rendering rules are SR-PP-01 to SR-PP-04.

---

## Before there is a tree

The panel shows one of five screens, in a fixed order, and each holds exactly
one link — the one step out of that situation.

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
   **Select Test Project** and **New Test Project** in the header are greyed.

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

1. **The reason**, in red, when the repository named a project it could not
   use — *not under the Testin root*, *archived*, *could not be read*.
2. **One link per project**, name then status. Clicking binds this repository
   to it and shows its tree. With more than six projects, this becomes a single
   link opening the **Select Test Project** dialog.

The other two screens — *\<name\> is not on this machine yet* with **Clone
\<name\>**, and **Create your first test project** — follow the same shape:
a gray line saying what is true, then the one link.

Used by UC-PP-01, UC-PP-02, UC-PP-03.

---

## The menu

Right-click any row, or press the keyboard's Context Menu key. What is greyed
depends on the row; what is absent depends on which optional plugins are
installed.

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

1. **Open** and **Create** first — the two things done most.
2. **Actions** is a submenu holding everything that changes a node in place:
   its status, undo and redo, re-create, remove, rename, order, and the
   clipboard. Status entries appear only for the kind selected — the three
   rows shown are a test project's.
3. **Run Tests** — only when the automation plugin is present.
4. **Export**, **Import** — module `EX`.
5. **Sync With Remote**, **View Pending Commits** — only when the Git plugin is
   present; the whole section is absent otherwise, separator included.
6. **Sync With SFTP** — always present, greyed unless the project is shared
   over SFTP.
7. **Edit Run** and **Set Status** — a run's own two; greyed once it is signed
   off.
8. **Generate Report** and **Details** last.

Every entry that changes something confirms itself once, in one past-tense
word. Every entry that only shows something confirms nothing.

---

## The dialogs

Every dialog below is built on the same frame: a title, its fields, and a
status bar naming each key it answers to. `Enter` confirms and `Escape` cancels
unless the bar says otherwise.

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

1. **The name.** Placeholder *set name...*. Empty on `Enter` turns the
   placeholder red and keeps focus here.
2. **The two kinds**, with a hint each. `↑` `↓` move between them; the field's
   leading icon follows. Clicking a row submits with that kind.
3. **The status bar** — every key this dialog answers to.

The run-side twin, **Create Run Node**, is identical with the kinds *test run*
(*Records execution results*) and *test run package* (*Groups test runs*), and
the placeholder *set name, like Sprint 3 Cycle 1...*.

> **Today the two kind names are blank** — the rows show only their hints.
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

1. The current name, filled in and selected, so typing replaces it.

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

1. **The question**, naming the node.
2. **What it holds** — the line a tester reads before pressing `Enter`. Omitted
   when every count is zero.
3. **Where it is**, muted.
4. The confirm key is named for what it does: **Remove**, not **OK**.

For several nodes the question is *Remove these N items?* with no counts and no
path.

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

1. **The question** says the verb — *Move* after a cut, *Copy* after a copy —
   and the names.
2. **From** and **To**, muted, so a wrong drop is caught before `Enter`.
3. The confirm key is named for the verb.

From a paste the title is **Paste**; from a drop it is the verb, **Move** or
**Copy**. Same dialog otherwise.

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

1. **One field.** Shows the node's current number, or nothing. Digits only, no
   leading zero — refused as typed. Empty means date order.

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

1. **Five rows, one per status**, each with its icon — the same icon the tree
   draws for a run in that status.
2. **A key beside three of them.** *Created* and *In Progress* are the run's
   own record and have none. The keys read `2` `1` `3` down the list because
   the rows are in the product's status order, not key order.
3. The first row is selected when the popup opens; `↑` `↓` and `Enter`, a key,
   or a click all choose.

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

1. **The name**, prefilled from the create dialog, placeholder *Cycle-1*.
2. **Configuration details** — a collapsible form: change log, commit id, test
   type, platform, component, language, and, only when platform and component
   call for it, browser or device.
3. **The cases**, as a checkbox tree of every live, non-empty test set. All
   checked for a new run; only the previous cycle's for a re-creation.
4. **Create** is enabled only while at least one case is checked. There is no
   `Enter` to submit; the button is the gesture.

**Edit Test Run** is this dialog with the button **Save**, opened on the run's
own name, cases and configuration.

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

1. **The field**, placeholder *Go to a test set or run, or search for
   anything...*. Before anything is typed the list holds every test set and
   run.
2. **A result row**: icon, name, and under it the path through the tree.
3. At most fifty rows.

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

1. **How many changes**, and the one sentence that matters: they come with
   you, and can be committed onto the wrong branch by mistake.
2. **From** and **To** — the branch the tester is on, and the one they picked.
3. **Two confirms**: `Enter` switches anyway; `Shift+Enter` opens the review
   instead.

Used by UC-PP-19.

---

## Decisions this design takes

### One project in the tree, not a project list

The panel shows the test project this repository is bound to and no other. A
list of projects in the tree would mean the tree, the reports and the runs could
disagree about which project is open. The binding lives in the repository, so a
colleague who clones it lands on the same project with no setup.

### A run is drawn as its status

Every other node draws what it *is*. A run draws where it *stands* — its status
icon and the status word — because that is the one thing a tester scanning the
tree wants to know about a run, and opening each one to find out is the cost
this saves.

### Retired and cut look the same

Both are gray. A **Deprecated** set, an **Archived** package and a node cut but
not yet pasted are all "not part of current work right now", and one color for
one meaning is easier to learn than three.

### Every removal, move and copy asks first

The confirmation names what will happen — *Remove 'Login'?*, *Move 'Login' into
'Smoke'?* — and shows where from and where to. The confirm key is named for the
verb. Nothing in the tree changes until `Enter`.

### The menu is short at the top and deep in Actions

The top level holds what is done often — open, create — and what belongs to
other modules. Everything that changes a node in place is one level down under
**Actions**, so the first thing a tester sees is short, and the dangerous
things are a deliberate second step.

### Order is a dialog, not a drag

Dropping between rows to reorder is not offered. A number is explicit, survives
a re-index, and is the same number the generated automation carries as its
execution order. A drag would be faster and would say nothing.

---

## Where the build differs from this design

| Drawn | Built |
|---|---|
| The create dialog's rows show a kind name and a hint | The names are blank; only the hints show. D4 in the business requirements |
| **Paste** greyed on a test run, as on a test set | Enabled, and always refuses. D2 |
| The status popup offers the three statuses a tester sets | It offers all five, and lets a run go backwards. D5 |
| **New Test Project** re-enables once a Testin folder is set | It stays greyed until the IDE project is reopened |
| **Select Test Project** has a column heading over the names | The heading is blank — the same caption sweep as D4 |
