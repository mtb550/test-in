[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-003

# UC-TREE-PANEL-003: Import a test project that already exists

> **No key.** Press **New Test Project** at the top of the panel, and paste
> the address instead of a name.

**As a** tester, **I want** to bring a test project that already exists
somewhere else onto this machine, **so that** I can work on it without building
it again by hand.

This copies a test project from Git onto this machine.

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
- **Rule-TREE-PANEL-018** — Cloning needs two things. It needs the Git plugin.
  It also needs this code project to already name the test project it is
  cloning.
- **Rule-TREE-PANEL-019** — The folder is named by the code project, never by
  the address. A repository called `nafath-test-case` is a place to clone from.
  The test project's name is written once, in `testin.yml`, which travels with
  the repository. The tree, the reports and the server path all read it from
  there.
- **Rule-TREE-PANEL-100** — An archived test project is shown in the tree and
  holds nothing. It is indexed as a node so the tree can say what it is - drawn
  with Archived beside its name like any other status - and its test sets, cases
  and runs are not read, because an archived project is not worked on.

## What the tester sees

The **Create Project** dialog opens. It is the same one-field dialog a new test
project uses, and it is drawn under
[UC-TREE-PANEL-002](createTestProject.md). While Git copies the repository, a
progress bar reads *Cloning repository*, with the line *Cloning into
\<name\>...* under it. The tree then appears, and *Project cloned* shows above
the status bar at the bottom right of the IDE.

## Main flow

There is a second way in. The code project may name a test project that is not
on this machine. The panel then offers the link **Clone \<name\>**. That link is
drawn under [UC-TREE-PANEL-001](reachTheTree.md), and it does the same thing.

**From a Git address**

1. The Git plugin is installed, and this code project already names the test
   project.
2. The tester presses the **New Test Project** button in the panel header.
3. The tester pastes the repository address instead of a name, and presses
   `Enter`.
4. Testin copies the repository into the Testin folder. The new folder takes
   the name the code project gives. A progress bar reads *Cloning repository*,
   with the line *Cloning into \<name\>...*. It cannot be canceled.
5. Testin binds this code project to it, and the tree appears.
6. Testin shows *Project cloned*.

## What Testin refuses

**If this code project names no test project** — nothing is cloned. A warning
titled *No Test Project Named* opens. It says the project file must name the
test project first. It also says the tester can set the name there, or pick a
project with **Select Test Project**.

**If the Git plugin is not installed** — nothing is cloned. Testin shows *Git
Plugin Not Available*, reading *Git synchronization and cloning require the Git
plugin, which is not available in this IDE.* Testin checks this before anything
else. So in an IDE without Git the tester never sees *No Test Project Named*.

**If no Testin folder is set** — the **New Test Project** button is gray.
(Rule-TREE-PANEL-089)

---

## From an SFTP server: not built

> **A tester cannot do this today.** Testin can sync a test project it
> already has with an SFTP server, which is part of reports, export, import and
> sync. It cannot bring a test project down from one that it does not have yet.
> There is no button, no menu item and no key for it.
>
> What it would need: a place to type the server, the folder and the account.
> It would also need the same Rule-TREE-PANEL-019 decision about what the test
> project is called. Until that is built, a tester who keeps test projects on an
> SFTP server works in three steps. They create the test project, they set up
> the SFTP account, and then they sync.

---

[Documentation](../README.md) › [The tree panel](main.md)
