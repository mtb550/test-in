[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-018

# UC-TREE-PANEL-018: Retire a test project, a test set or a package

> **No key.** On the menu: **Actions**, then the status. A test project has
> **Deactivate** and **Archive**. A test set has **Mark Deprecated**. A package
> has **Archive**.

**As a** tester, **I want** to mark old work retired, **so that** it stays for
its history without getting in the way of what I am testing now.

## Rules

- **Rule-TREE-PANEL-060** — Retiring deletes nothing. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-061** — A test project that is not **Active** shows nothing
  under it.
- **Rule-TREE-PANEL-062** — An **Archived** test project is not opened at all on
  the next load. The panel says so, and offers the other test projects.
- **Rule-TREE-PANEL-063** — A status is set on one node at a time. The status a
  node already has is not offered.

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects exactly one node.
2. The tester opens **Actions**. The status entries for that node's kind are
   there, and the status it already has is gray.

   | Node | Retire it with | Statuses it can have |
   |---|---|---|
   | Test project | **Deactivate**, **Archive** | Active, Inactive, Archived |
   | Test set | **Mark Deprecated** | Active, Deprecated |
   | Package | **Archive** | Active, Archived |

3. The tester chooses one.
4. Testin writes the status, refreshes the tree, and shows the new status word:
   *Inactive*, *Archived* or *Deprecated*.

**What retiring does.** A **Deprecated** test set or an **Archived** package is
drawn gray and sorts last. **Expand All** leaves it closed. It is not offered
when a test run is created, and its test cases are skipped when a parent is run.
Nothing inside it is deleted.

**What an inactive test project does.** Its row shows no children. If it is
**Archived**, the next load skips it, and the panel shows *\<name\> is archived,
so it is not opened*. That screen is drawn under
[UC-TREE-PANEL-001](reachTheTree.md).

To bring one back, see [UC-TREE-PANEL-019](reactivateNode.md).

## What Testin refuses

**If the status could not be written** — Testin says *Unable to update status to
\<status\>* for a test project, *Unable to mark test set \<status\>* for a test
set, or *Unable to mark package \<status\>* for a package. Nothing changes.

**If more than one node is selected** — the status entries are not offered.

---

[Documentation](../README.md) › [The tree panel](main.md)
