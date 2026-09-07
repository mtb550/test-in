[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-020

# UC-TREE-PANEL-020: Set a test run's status

> **No key opens it.** On the menu: **Set Status**. Inside the popup,
> **`1`** is **Assigned**, **`2`** is **Completed** and **`3`** is
> **Closed**.

**As a** tester, **I want** to mark a test run **Assigned**, **Completed** or
**Closed** from the tree, **so that** the test run's place in its life is
visible without opening it.

## Rules

- **Rule-TREE-PANEL-065** — **Completed** and **Closed** are final. The test run
  accepts no more verdicts. Every test case still **Pending** becomes
  **Untested**. (Rule-TREE-PANEL-009)
- **Rule-TREE-PANEL-066** — A tester sets **Assigned**, **Completed** and
  **Closed**. **Created** and **In Progress** are the test run's own record of
  itself.

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Set Test Run Status popup

```
┌──────────────────────────────────────────────────────────────┐
│  Set Test Run Status                                         │
├──────────────────────────────────────────────────────────────┤
│  > +  Created                                           (1)  │
│    @  In Progress                                            │
│    *  Completed                                     2   (2)  │
│    !  Assigned                                      1        │
│    x  Closed                                        3        │
└──────────────────────────────────────────────────────────────┘
```

1. **Five rows, one per status** — each with its icon. It is the same icon the
   tree draws for a test run in that status.
2. **A key beside three of them** — *Created* and *In Progress* have none,
   because they are the test run's own record. The keys are not in order down
   the list, because the rows follow the status order instead: **Assigned** is
   `1`, **Completed** is `2`, **Closed** is `3`.
3. The popup opens in the middle of the IDE window, not at the pointer, with
   the first row selected. The tester chooses with `↑` `↓` and `Enter`, with a
   key, or with a click. **Nothing on screen says so**: the popup has no status
   bar.

## Main flow

1. The tester selects a test run that is **Created**, **In Progress** or
   **Assigned**.
2. The tester chooses **Set Status**.
3. The **Set Test Run Status** popup lists the five statuses. Three of them
   carry a key: `1` **Assigned**, `2` **Completed**, `3` **Closed**.
4. The tester presses that key, or moves with `↑` `↓` and presses `Enter`, or
   clicks a row.
5. Testin writes the status. The test run's icon and its gray status word in the
   tree both change, and Testin shows the new status word.
6. Setting **Completed** or **Closed** signs the test run off. Every test case
   still **Pending** becomes **Untested**. Testin records the time the test run
   finished, but only if it had been started. From then on **Set Status**,
   **Edit Run** and **Run Tests** are gray on it.

**Signing a test run off cannot be undone.** A status change is not on the
tree's history (Rule-TREE-PANEL-058), and there is no confirmation before it.
Every **Pending** verdict becomes **Untested** at that moment.

## What Testin refuses

**If the tester clicks outside the popup** — it closes, and nothing changes.

**If several rows are selected** — **Set Status** stays black and acts on the
last row the tester clicked.

> **The popup offers the status the test run already has.** Choosing it rewrites
> the record, stamps who changed it and when, and confirms with that word. Rule
> 54, which keeps the current status off the menu, holds for retiring and not
> here.

> **An open editor of that test run is not told.** It keeps showing the old
> status, and the rows that just became **Untested**, until it is reopened.

**Not decided** — see question 1 and question 2 on
[the tree panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The tree panel](main.md)
