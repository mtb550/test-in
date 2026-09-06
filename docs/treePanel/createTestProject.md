[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-002

# UC-TREE-PANEL-002: Create a test project

> **No key.** Press **New Test Project** at the top of the panel, or the
> welcome link **Create your first test project**.

**As a** tester, **I want** to create a test project by name, **so that** a new
product under test has a place before any test is written.

## Rules

- **Rule 16** — A test project is a folder directly under the Testin folder.
  Any other folder there is ignored.
- **Rule 17** — Creating a test project binds this code project to it.

Rules 1 to 13 hold everywhere in the panel. They are on
[the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Create Project dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Create Project                                              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [tp]  set name or paste url...                         (1)  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    Escape Cancel                         (2)  │
└──────────────────────────────────────────────────────────────┘
```

1. **One field** — its gray hint text reads *set name or paste url...*. What the
   tester types decides what happens. A Git address is cloned, which is
   [UC-TREE-PANEL-003](importTestProject.md). Anything else is the name of a new test
   project. There is no list to pick from, because the tester would only be
   saying twice what they already typed.
2. **The status bar** — every key this dialog answers to.

The **New Test Project** button opens it. So does the welcome link **Create your
first test project**, which is drawn under [UC-TREE-PANEL-001](reachTheTree.md).

## Main flow

1. The tester presses the **New Test Project** button in the panel header, or
   the welcome link **Create your first test project**.
2. The tester types a name and presses `Enter`.
3. Testin creates the test project folder in the Testin folder.
4. Testin binds this code project to it, and the tree appears.
5. Testin shows *Project created*.

To bring down a test project that already exists somewhere else, paste its
address instead of a name. That is [UC-TREE-PANEL-003](importTestProject.md).

## What Testin refuses

**If the name is empty** — the gray hint text turns red, and the dialog stays
open.

**If a folder with that name already exists in the Testin folder** — nothing is
created, and *\<name\> Already Exists* is shown in red.

**If no Testin folder is set** — the **New Test Project** button is gray.
(rule 77)

---

[Documentation](../README.md) › [The tree panel](main.md)
