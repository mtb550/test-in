[Documentation](../README.md) › [The project panel](main.md) › UC-021

# UC-021: Re-create a test run

> **No key.** On the menu: **Actions → Re-create**.

**As a** tester, **I want** to make the next cycle from a finished test run, with
the same test cases and settings and no verdicts, **so that** starting the next
round of testing takes one step, instead of building the whole test run again by
hand.

## Rules

- **Rule 57** — Re-create works on a test run in any status, including a
  signed-off one. That is what it is for.
- **Rule 58** — Only the test cases and the configuration are carried over.
  Verdicts, durations and failure details start fresh.
- **Rule 59** — The next name is suggested by counting up. *cycle-1* becomes
  *cycle-2*. A name already taken is skipped.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The dialog is drawn under [UC-009](createTestRun.md).

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

The same three refusals apply as when a test run is created.

**If no test case is ticked** — the **Create** button is disabled.

**If the name is empty** — *A test run needs a name* is shown, and the dialog
stays open with everything typed still in it.

**If the name is already used** — *\<name\> Already Exists* is shown, and the
dialog stays open with everything typed still in it.

---

[Documentation](../README.md) › [The project panel](main.md)
