[Documentation](../README.md) › [The project panel](main.md) › UC-020

# UC-020: See what a node holds

> **No key.** On the menu: **Details**.

**As a** tester, **I want** to see a node's counts, dates, status and verdict
breakdown without opening anything, **so that** I can see how big a part of the
tree is at a glance.

## Rules

- **Rule 74** — Opening **Details** changes nothing, so Testin says nothing.
  (rule 7)
- **Rule 75** — Testin counts what a node holds when the tester asks. It never
  saves the number.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester selects any node.
2. The tester chooses **Details**.
3. The **Details** dialog shows the node's name and its path.
4. It shows who created it, who last changed it, and when.
5. It shows its status, and what it holds, counted now and never saved.
6. It shows a verdict chart.
7. The tester presses `Escape`. It closes. Nothing was changed, and nothing is
   announced.

---

[Documentation](../README.md) › [The project panel](main.md)
