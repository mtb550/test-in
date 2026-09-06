[Documentation](../README.md) › [The project panel](main.md) › UC-017

# UC-017: Redo a change to the tree

> **`Ctrl+Y`**. On the menu: **Actions → Redo**, which names what it will put
> back.

**As a** tester, **I want** to put back a change I undid, **so that** changing my
mind twice costs no more than changing it once.

## Rules

- **Rule 50** — Making a new change forgets everything that was undone.

Rules 48 and 49 hold here too. They say what the tree remembers and what can be
taken back, and they are on [UC-016](undoChange.md).

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. Something was just undone.
2. The tester presses `Ctrl+Y`, or chooses **Actions → Redo \<what\>**.
3. Testin applies the change again, and shows *Redone*.

## What Testin refuses

**If a new change was made after the undo** — **Redo** is gray. The undone change
is forgotten. (rule 50)

**If nothing has been undone** — **Redo** is gray.

---

[Documentation](../README.md) › [The project panel](main.md)
