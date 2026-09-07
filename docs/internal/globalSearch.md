[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-001

# UC-INTERNAL-001: Find anything in the test project

> **`Ctrl+Alt+F`**, and **`Cmd+Alt+F`** on a Mac. It works anywhere in the
> IDE, not only in the tree. There is also a **Search Test Project** button
> at the top of the tree panel.

**As a** tester, **I want** to type part of a name, a step or a test case number
and jump to it, **so that** a large tree is never a place to scroll.

## Rules

- **Rule-INTERNAL-001** — Nodes are found by name, from the first character
  typed. Test cases are found from the second character, and every field of the
  test case is searched: its description, its id, its steps, its expected
  result, its priority, its status, its groups, its module, its preconditions,
  its reference, its test data, its place in the tree, its generated code, and
  who created or last changed it and when. Case does not matter, and spaces at
  either end are dropped.
- **Rule-INTERNAL-002** — Choosing a result always takes the tree to it. Only a
  test set and a test run open an editor.

The rules that hold everywhere in the tree panel do not govern this dialog.
It opens from anywhere in the IDE, and the tree is only where it lands.

## The Search Test Project dialog

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
   for anything...*. With nothing typed the list holds test sets and test runs
   only, the places there is something to open. Once the tester types, packages,
   the two containers and the test project row match by name too.
2. **A result row** — one line: the icon, the name, then the path after it in
   gray. Twelve rows are visible and the rest scroll. The icon at the front of
   the field changes to the icon of whichever row is selected.
3. **At most 50 rows in all.** Nodes are taken first, so 50 matching nodes leave
   no room for test cases. The top row is selected as soon as the list refills,
   so `Enter` goes there without pressing `↓`. `↑` and `↓` stop at the ends.

## Main flow

1. The tester presses `Ctrl+Alt+F` anywhere in the IDE, or `Cmd+Alt+F` on a
   Mac, or presses the **Search Test Project** button in the panel header.
2. The **Search Test Project** dialog opens. Before anything is typed, it lists
   every test set and test run.
3. The tester types.
4. Testin lists every node whose name contains what was typed, from the first
   character. It lists test cases from the second character. The list is ordered
   nodes first, shortest name first, then test cases.
5. The tester moves with `↑` `↓` and presses `Enter`, or clicks a row.
6. The panel comes forward, and the tree expands to that node and selects it.
7. A test set or a test run opens in its editor. Any other node is only revealed
   in the tree.
8. A test case opens its test set's editor with that test case selected, and
   fills the view panel on the right with it.

## What Testin refuses

**If nothing matches** — the list is empty. `Enter` does nothing, and the dialog
stays open.

**If no Testin folder is set, or nothing is indexed yet** — the dialog still
opens. It lists nothing. The key and the button are never gray.

**If a package, a container or the test project row is chosen** — the tree goes
to it and nothing opens. Testin says nothing.

---

[Documentation](../README.md) › [Inside Testin](main.md)
