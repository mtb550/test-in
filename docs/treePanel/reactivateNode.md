[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-019

# UC-TREE-PANEL-019: Bring a retired node back

> **No key.** On the menu: **Actions → Activate** for a test project, or
> **Mark Active** for a test set or a package.

**As a** tester, **I want** to make a retired test set, package or test project
current again, **so that** work I put aside can be picked up without building it
again.

## Rules

- **Rule-TREE-PANEL-064** — Bringing a node back undoes nothing but the status.
  Everything inside it is exactly as it was left.

Rule-TREE-PANEL-063 holds here too. It says a status is set on one node at a
time, and it is on [UC-TREE-PANEL-018](retireNode.md).

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects exactly one retired node.
2. The tester opens **Actions**.
3. The tester chooses **Activate** for a test project, or **Mark Active** for a
   test set or a package.
4. Testin writes the status, refreshes the tree, and shows *Active*.
5. The node is no longer gray. It sorts among the live nodes again, by its own
   number. (Rule-TREE-PANEL-010)
6. A test set that is **Active** again is offered when a test run is created.

An **Archived** test project cannot be brought back from the tree, because the
tree does not open it. The panel offers it in the list of test projects, and
choosing it is [UC-TREE-PANEL-004](chooseTestProject.md).

## What Testin refuses

**If the node is already Active** — the entry is gray. (Rule-TREE-PANEL-063)

**If the status could not be written** — Testin says it could not, and nothing
changes. The messages are on [UC-TREE-PANEL-018](retireNode.md).

---

[Documentation](../README.md) › [The tree panel](main.md)
