[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-004

# UC-TREE-PANEL-004: Choose which test project this code project uses

> **No key.** Press **Select Test Project** at the top of the panel.

**As a** tester, **I want** to point this code project at a different test
project in the same Testin folder, **so that** one machine can serve several
products.

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
- **Rule-TREE-PANEL-020** — The choice is written into the code project. A
  colleague who copies that project down gets the same test project, with no
  setup.
- **Rule-TREE-PANEL-021** — If the choice cannot be written, Testin says so. It
  never reports the choice as saved.

## The Select Test Project dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Select Test Project                                         │
├──────────────────────────────────────────────────────────────┤
│                                       Status            (1)  │
│  Demo                                 Active            (2)  │
│  Mobile                               Active                 │
│  Legacy                               Archived               │
├──────────────────────────────────────────────────────────────┤
│  Enter Select    Escape Cancel                          (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **Two columns.** The second is headed **Status**. The first has no heading at
   all, which is difference 7 below.
2. **One row per test project** in the Testin folder, whatever its status. The
   one this code project already uses is selected.
3. **The status bar** — every key this dialog answers to. A click selects a row
   but never confirms; only `Enter` does.

## Main flow

1. The tester presses the **Select Test Project** button in the panel header.
2. The **Select Test Project** dialog lists every test project in the Testin
   folder, with its status. The current one is selected.
3. The tester selects one and presses `Enter`.
4. Testin writes the choice into this code project.
5. The tree reloads on that test project.
6. Testin shows *Bound*, with the test project's name.

## What Testin refuses

**If no test project folder exists in the Testin folder** — no dialog opens, and
the message *No Test Projects*, with the line *Create one under the Testin root
first*, is shown in red.

**If the code project's configuration file cannot be written** — an error titled
*Not Bound* says the choice will not be remembered, and the dialog stays open.

**If no row is selected** — `Enter` does nothing, and says nothing. That happens
when nothing is bound yet, or when the bound name matches no row.

**If the chosen test project is Archived** — Testin binds to it and says *Bound*
all the same. The panel then shows *\<name\> is archived, so it is not opened*
instead of a tree.

**If the project file is edited by hand** — the tree does not notice. Testin
reads that file when the project opens, and again only when the tester presses
**Refresh**.

---

[Documentation](../README.md) › [The tree panel](main.md)
