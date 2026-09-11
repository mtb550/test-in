[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-009

# UC-TREE-PANEL-009: Create a test run

> **`Ctrl+M`**, with **Test Runs** or a test run package selected, then pick
> *Test Run*. On the menu: **Create**.

**As a** tester, **I want** to start a test run over the test cases I choose,
**so that** a pass through the product is recorded on its own.

A test run is one round of testing, with a verdict for each test case.

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
- **Rule-TREE-PANEL-099** — A node says its status beside its name when the status
  is not active. An inactive test project, a deprecated test set and an archived
  package each say which they are, in gray; a node in current work says nothing,
  because "Active" on every name is a word read a hundred times and needed
  never. A test run always says its status, because where a cycle stands is what
  the tree is read for.
- **Rule-TREE-PANEL-029** — A test run needs at least one test case. It cannot
  be created empty.
- **Rule-TREE-PANEL-030** — Three things are not offered when a test run is
  created: a retired test set, anything under an **Archived** package, and an
  empty test set. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-031** — A new test run starts as **Created**. Every test
  case in it starts **Pending**.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is - drawn with Inactive beside its name like any other status - and its test
  sets, cases and runs are not read, because a project nobody is working on is
  not worth the walk.

Rule-TREE-PANEL-032 holds here too. It says what can be created under **Test
Runs**, and it is on [UC-TREE-PANEL-010](createTestRunPackage.md).

## The Create Test Run dialog

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Create Test Run                                                           │
├────────────────────────────────────────────────────────────────────────────┤
│  v Configuration details                                 Collapse   (1)    │
│     Test Run name   [ cycle-2                              ]       (2)     │
│     Change Log      [ Story-002 (register new user), Sto...]               │
│     Commit ID       [ Commit hash, like 9f3c1ab...         ]               │
│     Test Type       [                                    v ]               │
│     Platform        [ Web                                 v ]              │
│     Component       [ Frontend                            v ]              │
│     Language        [                                     v ]              │
│     Browser         [                                     v ]       (3)    │
│                                                                            │
│  [x] v Test Cases                                                  (4)     │
│  [x]   v Accounts                                                          │
│  [x]       Login                                                           │
│  [x]       Registration                                                    │
│  [ ]     Checkout                                                          │
│                                                                            │
│                                                     [ Create ]      (5)    │
├────────────────────────────────────────────────────────────────────────────┤
│  Tab Navigate    Space Check    Escape Cancel                              │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Configuration details** — a section the tester can open and close. The gray
   hint on its right reads *Collapse*, and *Expand* once it is closed. Closing
   it takes every field below with it, the name included.
2. **Test Run name** — filled in with what the tester typed in the first
   dialog. When it is emptied, its gray hint text reads *Cycle-1*. The keyboard
   does not start here. It starts in **Change Log**.
3. **The fields** — one to a row. Every dropdown starts blank. Blank means the
   tester has not answered it. Every dropdown can also be typed into, so a value
   the list does not offer is still saved. **Browser** is on the form only while
   *Platform* is **Web** and *Component* is **Frontend**. **Device Type** takes
   its place only while *Platform* is **Mobile** and *Component* is
   **Frontend**. A field that is not on the form is saved empty. So switching
   *Platform* from **Web** to **Mobile** drops the browser that was picked. The
   lists are:

   | Field | Offers |
   |---|---|
   | Test Type | *Functional Test*, *Performance Test* |
   | Platform | *Web*, *Mobile* |
   | Component | *Frontend*, *Backend* |
   | Language | *English*, *Arabic*, *French* |
   | Browser | *Chrome*, *Firefox*, *Safari*, *Edge* |
   | Device Type | *iPhone*, *Samsung*, *Huawei* |

4. **The test cases** — the whole folder tree, with a tick box on every row.
   The **Test Cases** row is at the top. Under it come each package, each test
   set, and each test case. The tree opens fully expanded. Ticking a folder
   ticks everything under it. An empty test set is left out, and so is a package
   that holds only empty test sets.
5. **Create** — enabled only while at least one test case is ticked. `Enter`
   does nothing here. The button is the only way to confirm. A click outside the
   dialog does not close it either. Only `Escape` closes it.

**Edit Test Run** is this same dialog with the button **Save**. It opens filled
with the test run's own name, test cases and settings.

## Main flow

1. The tester selects **Test Runs** or a test run package.
2. The tester presses `Ctrl+M`, or chooses **Create**.
3. The **Create Run Node** dialog opens. Its first row is selected, and reads
   *Records execution results*. Its gray hint text reads *set name, like Sprint
   3 Cycle 1...*.
4. The tester types a name and presses `Enter`.
5. The **Create Test Run** dialog opens. It holds the typed name in *Test Run
   name*. It also holds the *Configuration details* form, and a tree of test
   sets with every test case ticked. Three things are left out of that tree:
   retired test sets, anything under an **Archived** package, and empty test
   sets.
6. The tester ticks and unticks with `Space`, moves with `Tab`, and presses
   **Create**.
7. Testin writes the test run. A progress bar reads *Creating test run
   \<name\>*, and it cannot be canceled. Every ticked test case is **Pending**.
   The test run's status is **Created**.
8. Its editor opens, and Testin shows *Run created*.

## What Testin refuses

**If no test case is ticked** — the **Create** button is disabled.

**If the name has been emptied** — the dialog stays open, and *A test run needs
a name* is shown in red.

**If a test run with that name already exists** — *\<name\> Already Exists* is
shown in red. Typing it in the first dialog closes that dialog and the second
one never opens. Typing it in **Create Test Run** leaves the dialog open with
everything still in it.

**If the parent folder was removed while the dialog was open** — the dialog
stays open, and *'\<parent\>' no longer exists - test run not created* is shown
in red.

**If writing the test run fails** — an IDE notification titled *Test Run Not
Created* stays in the notification log, with the reason under it.

**If several nodes are selected** — **Create** works on the first of them.

---

[Documentation](../README.md) › [The tree panel](main.md)
