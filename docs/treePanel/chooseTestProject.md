[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-004

# UC-TREE-PANEL-004: Choose which test project this code project uses

> **No key.** Press **Select Test Project** at the top of the panel.

**As a** tester, **I want** to point this code project at a different test
project in the same Testin folder, **so that** one machine can serve several
products.

## Rules

- **Rule 19** — The choice is written into the code project. A colleague who
  copies that project down gets the same test project, with no setup.
- **Rule 20** — If the choice cannot be written, Testin says so. It never
  reports the choice as saved.

Rules 1 to 13 hold everywhere in the panel. They are on
[the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
