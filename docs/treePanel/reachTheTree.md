[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-001

# UC-TREE-PANEL-001: Open the panel and reach the tree

> **No key.** The panel opens from the **Testin** button on the IDE's tool
> window bar, on the left.

**As a** tester, **I want** the panel to show my tree, or tell me what to do
first, **so that** I am never stuck on an empty window.

## Rules

- **Rule-TREE-PANEL-014** — The panel never opens on its own when the IDE
  starts.
- **Rule-TREE-PANEL-015** — A code project does not have to name a test project.
  If it names none, and exactly one test project exists in the Testin folder,
  Testin binds it to that one without asking.

Rule-TREE-PANEL-053 also holds here. It says an **Archived** test project is not
opened on the next load, and it is on [UC-TREE-PANEL-018](retireNode.md).

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

1. **The header** — every welcome screen starts with these five lines, whichever
   of the five it is.
2. **The one link.** Nothing else is offered, because nothing else can work yet.
   **Select Test Project** and **New Test Project** in the toolbar are gray.

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
   the tree appears. If there are more than six test projects, Testin shows one
   link instead of a list. That link opens the **Select Test Project** dialog.

Two more screens follow. The fifth is the tree itself.

- *\<name\> is not on this machine yet*, a gray line, then the link
  **Clone \<name\>**.
- **Create your first test project**, a link with no gray line above it.

## Main flow

The panel shows one of five screens, and checks for them in this order. Every
one of them opens with the same header, drawn below, and then its own line and
its own links.

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

> **The one project it binds to may be Archived.** Rule-TREE-PANEL-015 does not look at the
> status, so a tester with a single archived test project is bound to it and
> then told it is not opened, above a list holding only that one project.

> **Showing a Git-shared test project writes to the code project.** The first
> time the panel draws one, Testin asks Git for the remote address and writes it
> into the project file. There is no dialog and no message, and that file is one
> the tester commits.

---

[Documentation](../README.md) › [The tree panel](main.md)
