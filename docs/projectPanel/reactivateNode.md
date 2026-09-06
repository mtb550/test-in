[Documentation](../README.md) › [The project panel](main.md) › UC-019

# UC-019: Bring a retired node back

> **No key.** On the menu: **Actions → Activate** for a test project, or
> **Mark Active** for a test set or a package.

**As a** tester, **I want** to make a retired test set, package or test project
current again, **so that** work I put aside can be picked up without building it
again.

## Rules

- **Rule 85** — Bringing a node back undoes nothing but the status. Everything
  inside it is exactly as it was left.

Rule 54 holds here too. It says a status is set on one node at a time, and it is
on [UC-018](retireNode.md).

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects exactly one retired node.
2. The tester opens **Actions**.
3. The tester chooses **Activate** for a test project, or **Mark Active** for a
   test set or a package.
4. Testin writes the status, refreshes the tree, and shows *Active*.
5. The node is no longer gray. It sorts among the live nodes again, by its own
   number. (rule 10)
6. A test set that is **Active** again is offered when a test run is created.

An **Archived** test project cannot be brought back from the tree, because the
tree does not open it. The panel offers it in the list of test projects, and
choosing it is [UC-004](chooseTestProject.md).

## What Testin refuses

**If the node is already Active** — the entry is gray. (rule 54)

**If the status could not be written** — Testin says it could not, and nothing
changes. The messages are on [UC-018](retireNode.md).

---

[Documentation](../README.md) › [The project panel](main.md)
