[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-015

# UC-TREE-PANEL-015: Order nodes among their siblings

> **No key.** On the menu: **Actions → Order**.

**As a** tester, **I want** to give a node a number that fixes its place among
its siblings, **so that** the tree reads in the order the work is done.

## Rules

- **Rule 44** — A number is 1 or higher. Leave it empty and Testin sorts by date
  instead.
- **Rule 45** — Two siblings may carry the same number. The older one comes
  first.
- **Rule 46** — A retired node sorts after every live sibling. Its number does
  not change that. (rule 8, rule 10)
- **Rule 47** — The test project and the two containers cannot be ordered.

Rules 1 to 13 hold everywhere in the panel. They are on
[the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Order dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Order                                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [edit]  1, 2, 3... or empty for date order             (1)  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    Escape Cancel                              │
└──────────────────────────────────────────────────────────────┘
```

1. **One field** — shows the node's current number, or nothing. It accepts
   digits only, with no leading zero. Anything else is refused as it is typed.
   Empty means date order. The cursor sits after the number, so typing **adds
   to it**: a node numbered 3 whose tester types 5 ends up at 35. `Ctrl+A`
   selects it all.

## Main flow

1. The tester selects a test set, a package or a test run.
2. The tester chooses **Actions → Order**.
3. The **Order** dialog opens with one field. It shows the node's current
   number, or is empty, with the gray hint text *1, 2, 3... or empty for date
   order*.
4. The tester types a number from 1 up and presses `Enter`.
5. The node moves among its siblings, and Testin shows *Ordered*.
6. Emptying the field and pressing `Enter` removes the number. The node returns
   to date order, and Testin shows *Ordered*.

## What Testin refuses

**If the tester types a leading zero, a letter or a space** — the character does
not appear.

**If the test project or a container is selected** — **Order** is gray. (rule
47)

**If several rows are selected** — **Order** stays black and orders the first of
them, saying nothing about the rest.

> **A number too large to hold silently clears the order.** Typing
> 99999999999 puts the node back into date order, and Testin still says
> *Ordered*.

---

[Documentation](../README.md) › [The tree panel](main.md)
