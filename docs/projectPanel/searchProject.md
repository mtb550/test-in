[Documentation](../README.md) › [The project panel](main.md) › UC-017

# UC-017: Find anything in the project

**As a** tester, **I want** to type part of a name, a step or a test case number
and jump to it, **so that** a large tree is never a place to scroll.

## Rules

- **Rule 66** — Nodes are found by name, from the first character typed. Test
  cases are searched from the second character, in every part of the test case.
- **Rule 67** — Choosing a result opens the tree at it, and opens its editor
  too.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
   for anything...*. Before the tester types, the list holds every test set and
   test run.
2. **A result row** — shows:
   - the icon
   - the name
   - under them, the path through the tree
3. At most 50 rows.

## Main flow

1. The tester presses `Ctrl+Alt+F` anywhere in the IDE, or `Cmd+Alt+F` on a
   Mac, or presses the **Search Test Project** button in the panel header.
2. The **Search Test Project** dialog opens. Before anything is typed, it lists
   every test set and test run.
3. The tester types.
4. Testin lists every node whose name contains what was typed, starting from the
   first character typed. It lists test cases too, starting from the second
   character, matching any part of the test case.
5. At most 50 rows are shown. Each row shows the icon, the name, and under it the
   path *\<project\> > Test Cases > …*.
6. The tester moves with `↑` `↓` and presses `Enter`.
7. The panel comes forward, the tree expands to that node and selects it, and its
   editor opens.
8. If the row was a test case, its test set's editor opens with that test case
   selected.

---

[Documentation](../README.md) › [The project panel](main.md)
