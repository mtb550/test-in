[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-021

# UC-TREE-PANEL-021: Re-create a test run

> **No key.** On the menu: **Actions → Re-create**.

**As a** tester, **I want** to make the next cycle from a finished test run,
with the same test cases and settings and no verdicts, **so that** starting the
next round of testing takes one step, instead of building the whole test run
again by hand.

## Rules

- **Rule-TREE-PANEL-067** — Re-create works on a test run in any status,
  including a signed-off one. That is what it is for.
- **Rule-TREE-PANEL-068** — Only the test cases and the configuration are
  carried over. Verdicts, durations and failure details start fresh.
- **Rule-TREE-PANEL-069** — The next name is suggested by counting up. *cycle-1*
  becomes *cycle-2*, and a name with no number on it gets one: *smoke* becomes
  *smoke-2*. A name already taken is skipped.
- **Rule-TREE-PANEL-070** — The new test run is created in the same folder as
  the one it was made from.

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The dialog is drawn under [UC-TREE-PANEL-009](createTestRun.md).

## Main flow

1. The tester selects a test run in any status.
2. The tester chooses **Actions → Re-create**.
3. The **Create Test Run** dialog opens, with the next name suggested: *cycle-1*
   becomes *cycle-2*. The same test cases are ticked, and the same configuration
   is filled in.
4. A test case removed from its test set since the last test run is simply not
   there.
5. The tester presses **Create**.
6. Testin writes a new test run, with every ticked test case **Pending**.
7. Its editor opens, and Testin shows *Run created*.

## What Testin refuses

The same four refusals apply as when a test run is created.

**If no test case is ticked** — the **Create** button is disabled.

**If the name is empty** — *A test run needs a name* is shown, and the dialog
stays open with everything typed still in it.

**If the name is already used** — *\<name\> Already Exists* is shown, and the
dialog stays open with everything typed still in it.

**If the folder was removed while the dialog was open** — *'\<parent\>' no
longer exists - test run not created* is shown in red.

**If several rows are selected** — **Re-create** is gray. It needs exactly one.

---

[Documentation](../README.md) › [The tree panel](main.md)
