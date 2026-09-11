[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-001

# UC-TREE-PANEL-001: Open the panel and reach the tree

> **No key.** The panel opens from the **Testin** button on the IDE's tool
> window bar, on the left.

**As a** tester, **I want** the panel to show my tree, or tell me what to do
first, **so that** I am never stuck on an empty window.

This page says what the panel shows when the tester opens it. There are five
screens, and the tree is one of them.

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
- **Rule-TREE-PANEL-014** — The panel never opens on its own when the IDE
  starts.
- **Rule-TREE-PANEL-015** — A code project does not have to name a test project.
  If it names none, and exactly one test project exists in the Testin folder,
  Testin binds it to that one without asking.
- **Rule-TREE-PANEL-097** — Opening the tree panel puts the keyboard in the
  tree. Without that the IDE has nothing to give focus to and it stays in
  whatever was there before, so keys pressed over the tree are answered by the
  editor beside it.
- **Rule-TREE-PANEL-100** — An archived test project is shown in the tree and
  holds nothing. It is indexed as a node so the tree can say what it is - drawn
  with Archived beside its name like any other status - and its test sets, cases
  and runs are not read, because an archived project is not worked on.

Rule-TREE-PANEL-064 also holds here. It says an **Archived** test project is not
opened on the next load. It is written on [UC-TREE-PANEL-018](retireNode.md).

## No Testin folder

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

1. **The header** — these five lines start every welcome screen. The panel has
   five of them, and each one begins the same way.
2. **The one link.** Nothing else is offered. Nothing else can work until a
   Testin folder is set. **Select Test Project** and **New Test Project** in the
   toolbar are gray.

## Choose a project

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
   the tree appears. With more than six test projects, Testin shows one link
   instead of a list. That link opens the **Select Test Project** dialog.

Two more screens follow. The fifth is the tree itself.

- *\<name\> is not on this machine yet*, a gray line, then the link
  **Clone \<name\>**.
- **Create your first test project**, a link with no gray line above it.

## Main flow

The panel shows one of five screens. It checks for them in the order of the
table below. Each screen starts with the same header, drawn above. After the
header come the screen's own line and its own links.

The choose screen holds one link per test project, up to six of them. The others
hold one link each.

| If | The panel shows |
|---|---|
| No Testin folder is set | *Welcome to Testin* and the link **Configure Testin settings** |
| The bound test project is found | The tree |
| This code project names a test project that is not on this machine, and gives its Git address | *\<name\> is not on this machine yet* and the link **Clone \<name\>** |
| No test project exists in the Testin folder | The link **Create your first test project** |
| Otherwise | One link per test project, showing its name and then **Active**, **Inactive** or **Archived**. With more than six test projects, one link instead: **Select the test project for this repository** |

## What Testin refuses

**If the bound test project is Archived** — the panel shows, in red, *\<name\>
is archived, so it is not opened*. Under that line sits the list of test
projects to choose from.

**If the project file cannot be read** — the panel shows, in red, *testin.yml
names \<name\>, which could not be read*.

**If the project file is malformed** — Testin reads it as naming nothing. There
is no red line at all, and the tester gets the plain list of test projects with
no explanation.

> **The one project it binds to may be Archived.** Rule-TREE-PANEL-015 does not
> look at the status. A tester with one archived test project is bound to it.
> The panel then says it is not opened, above a list holding only that project.

> **Showing a Git-shared test project writes to the code project.** The first
> time the panel draws one, Testin asks Git for the remote address. Testin then
> writes that address into the project file. There is no dialog and no message.
> That file is one the tester commits.

---

[Documentation](../README.md) › [The tree panel](main.md)
