[Documentation](../README.md) › [The project panel](main.md) › UC-012

# UC-012: Retire and reactivate

**As a** tester, **I want** to mark a test set **Deprecated**, a package
**Archived**, or a whole test project **Inactive** or **Archived**, **so that**
old work stays for its history without getting in the way of current work.

## Rules

- **Rule 51** — Retiring deletes nothing. (rule 8)
- **Rule 52** — A test project that is not **Active** shows nothing under it.
- **Rule 54** — A status is set on one node at a time. The status a node already
  has is not offered.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects exactly one node.
2. The tester opens **Actions**. The status entries for that node's kind are
   there, and the status it already has is gray.

   | Node | Entries | Statuses |
   |---|---|---|
   | Test project | **Activate**, **Deactivate**, **Archive** | Active, Inactive, Archived |
   | Test set | **Mark Active**, **Mark Deprecated** | Active, Deprecated |
   | Package | **Mark Active**, **Archive** | Active, Archived |

3. The tester chooses one.
4. Testin writes the status, refreshes the tree, and shows the new status word:
   *Active*, *Inactive*, *Archived* or *Deprecated*.

**What retiring does.** A **Deprecated** test set or an **Archived** package is
drawn gray and sorts last. **Expand All** leaves it closed. It is not offered
when a test run is created, and its test cases are skipped when a parent is run.
Nothing inside it is deleted.

**What an inactive test project does.** Its row shows no children. If it is
**Archived**, the next load skips it, and the panel shows *\<name\> is archived,
so it is not opened*. That screen is drawn under
[UC-001](reachTheTree.md).

---

[Documentation](../README.md) › [The project panel](main.md)
