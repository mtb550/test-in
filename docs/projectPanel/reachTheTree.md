[Documentation](../README.md) › [The project panel](main.md) › UC-001

# UC-001: Open the panel and reach the tree

> **No key.** The panel opens from the **Testin** button on the IDE's tool
> window bar, on the left.

**As a** tester, **I want** the panel to show my tree, or tell me what to do
first, **so that** I am never stuck on an empty window.

## Rules

- **Rule 14** — The panel never opens on its own when the IDE starts.
- **Rule 15** — A code project does not have to name a test project. If it names
  none, and exactly one test project exists in the Testin folder, Testin binds it
  to that one without asking.
- **Rule 53** — An **Archived** test project is not opened at all on the next
  load. The panel says so, and offers the other test projects.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

1. The one link. Nothing else is offered, because nothing else can work yet.
   **Select Test Project** and **New Test Project** in the header are grayed.

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

The other two screens follow the same shape: a gray line saying what is true,
then the one link. They are:

- *\<name\> is not on this machine yet*, with **Clone \<name\>**
- **Create your first test project**

## Main flow

The panel shows one of five screens, and checks for them in this order. Each
screen holds exactly one link, and that link is the only way out of the
situation.

| If | The panel shows |
|---|---|
| No Testin folder is set | *Welcome to Testin* and the link **Configure Testin settings** |
| The bound test project is found | The tree |
| This code project names a test project that is not on this machine, and gives its Git address | *\<name\> is not on this machine yet* and the link **Clone \<name\>** |
| No test project exists in the Testin folder | The link **Create your first test project** |
| Otherwise | One link per test project, showing its name and then **Active**, **Inactive** or **Archived**. With more than six test projects, one link instead: **Select the test project for this repository** |

## What Testin refuses

**If this code project names no test project, and exactly one exists** — Testin
binds it to that one without asking, and shows its tree.

**If the bound test project is Archived** — the panel shows, in red, *\<name\> is
archived, so it is not opened*. Under that line sits the list of test projects to
choose from.

---

[Documentation](../README.md) › [The project panel](main.md)
