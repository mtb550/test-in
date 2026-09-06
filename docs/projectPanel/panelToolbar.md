[Documentation](../README.md) › [The project panel](main.md) › UC-028

# UC-028: Use the buttons at the top of the panel

> **Only one of the seven buttons has a key.** Search is **`Ctrl+Alt+F`**,
> and **`Cmd+Alt+F`** on a Mac.

**As a** tester, **I want** the panel's own buttons within reach of the tree,
**so that** the things I do to the whole panel are not hidden in a menu
somewhere else.

## Rules

- **Rule 77** — **Select Test Project** and **New Test Project** are gray until
  a Testin folder is set. Nothing else on the toolbar is ever gray.
- **Rule 78** — Every button says what it does when the tester hovers over it.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The toolbar

It is the row along the top of the panel, to the right of the word **Testin**.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin              [1]  [2]  [3]  [4]  [5]  [6]  [7]                     │
├────────────────────────────────────────────────────────────────────────────┤
│  [ main            v ]                                                     │
│                                                                            │
│  v Demo                                                                    │
│    v Test Cases                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

Seven buttons, always in this order. The last column is the page that says what
happens when the tester presses one.

| | Button | Hovering says | Where it is written |
|---|---|---|---|
| 1 | **Search Test Project** | *Find a test case, test set, package or run and go to it* | [UC-024](searchProject.md) |
| 2 | **Settings** | *Configure Testin settings* | Below |
| 3 | **Expand All** | *Expand all nodes* | Below |
| 4 | **Collapse All** | *Collapse all nodes* | Below |
| 5 | **Refresh** | *Re-index and reload tree* | [UC-025](refreshTree.md) |
| 6 | **Select Test Project** | *Choose the test project this repository exercises* | [UC-004](chooseTestProject.md) |
| 7 | **New Test Project** | *Create or Clone test project* | [UC-002](createTestProject.md) |

**Only the search button has a key.** It is `Ctrl+Alt+F`, and `Cmd+Alt+F` on a
Mac, and it works anywhere in the IDE. The button is how a tester finds out the
search exists at all, because nothing else on screen mentions it.

## Main flow

**Settings**

1. The tester presses **Settings**.
2. The IDE's settings open on the Testin page, where the Testin folder, the
   tester's name and the rest are set.

**Expand All**

1. The tester presses **Expand All**.
2. Every node opens, except retired ones, which stay closed. Retired means a
   **Deprecated** test set or an **Archived** package.

**Collapse All**

1. The tester presses **Collapse All**.
2. Every row under the test project closes.
3. The test project, **Test Cases** and **Test Runs** stay visible.

## What Testin refuses

**If no Testin folder is set** — **Select Test Project** and **New Test
Project** are gray. The other five still work.

---

[Documentation](../README.md) › [The project panel](main.md)
