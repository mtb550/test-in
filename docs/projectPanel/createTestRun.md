[Documentation](../README.md) › [The project panel](main.md) › UC-006

# UC-006: Create a test run or a test run package

> **`Ctrl+M`**, with **Test Runs** or a test run package selected. On the
> menu: **Create**.

**As a** tester, **I want** to start a test run over the test cases I choose,
**so that** a pass through the product is recorded on its own.

## Rules

- **Rule 26** — Under **Test Runs** or a test run package, only a test run or a
  test run package can be created.
- **Rule 27** — A test run needs at least one test case. It cannot be created
  empty.
- **Rule 28** — Three things are not offered when a test run is created: a
  retired test set, anything under an **Archived** package, and an empty test
  set. (rule 8)
- **Rule 29** — A new test run starts as **Created**. Every test case in it
  starts **Pending**.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Create Test Run dialog

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Create Test Run                                                           │
├────────────────────────────────────────────────────────────────────────────┤
│  Test Run name:  [ cycle-2                            ]         (1)        │
│                                                                            │
│  v Configuration details                                        (2)        │
│    Change Log  [ Story-002 (register new user)...   ]                      │
│    Commit ID   [ 9f3c1ab                            ]                      │
│    Test Type   [ Functional Test  v ]   Platform  [ Web  v ]               │
│    Component   [ Frontend         v ]   Browser   [ Chrome v ]             │
│                                                                            │
│  [x] v Accounts                                                 (3)        │
│  [x]     Login                                                             │
│  [x]     Registration                                                      │
│  [ ]   Checkout                                                            │
│                                                                            │
│                                                    [ Create ]   (4)        │
├────────────────────────────────────────────────────────────────────────────┤
│  Tab Navigate    Space Check    Escape Cancel                              │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The name** — prefilled from the create dialog. Placeholder *Cycle-1*.
2. **Configuration details** — a section the tester can open and close. Its
   fields are:
   - change log
   - commit id
   - test type
   - platform
   - component
   - language
   - browser or device, only when the platform and component make it relevant
3. **The test cases** — a tree with a tick box for every test set still in use
   that has test cases in it. For a new test run, all are ticked. For a
   re-creation, only the ones the last cycle used.
4. **Create** — enabled only while at least one test case is ticked. `Enter`
   does nothing here. The button is the only way to confirm.

**Edit Test Run** is this same dialog with the button **Save**. It opens filled
with the test run's own:

- name
- test cases
- configuration

## Main flow

1. The tester selects **Test Runs** or a test run package.
2. The tester presses `Ctrl+M`, or chooses **Create**.
3. The **Create Run Node** dialog opens with two kinds. Beside *test run* it says
   *Records execution results*. Beside *test run package* it says *Groups test
   runs*.
4. The tester types a name, picks a kind, and presses `Enter`.
5. For a package, Testin creates it and shows *Created*. That is the end.
6. For a test run, the **Create Test Run** dialog opens instead. It holds the
   typed name in *Test Run name*, a *Configuration details* form, and a tree of
   every live, non-empty test set with all test cases ticked. Retired test sets,
   anything under an **Archived** package, and empty test sets are not in that
   tree.
7. The tester ticks and unticks with `Space`, moves with `Tab`, and presses
   **Create**.
8. Testin writes the test run, with every ticked test case **Pending** and the
   status **Created**.
9. Its editor opens, and Testin shows *Run created*.

## What Testin refuses

**If no test case is ticked** — the **Create** button is disabled.

**If the name has been emptied** — the dialog stays open, and *A test run needs a
name* is shown in red.

**If the parent folder was removed while the dialog was open** — the dialog stays
open, and *'\<parent\>' no longer exists - test run not created* is shown in
red.

---

[Documentation](../README.md) › [The project panel](main.md)
