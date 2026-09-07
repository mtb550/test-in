[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-009

# UC-TREE-PANEL-009: Create a test run

> **`Ctrl+M`**, with **Test Runs** or a test run package selected, then pick
> *test run*. On the menu: **Create**.

**As a** tester, **I want** to start a test run over the test cases I choose,
**so that** a pass through the product is recorded on its own.

## Rules

- **Rule-TREE-PANEL-027** — A test run needs at least one test case. It cannot
  be created empty.
- **Rule-TREE-PANEL-028** — Three things are not offered when a test run is
  created: a retired test set, anything under an **Archived** package, and an
  empty test set. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-029** — A new test run starts as **Created**. Every test
  case in it starts **Pending**.

Rule-TREE-PANEL-026 holds here too. It says what can be created under **Test
Runs**, and it is on [UC-TREE-PANEL-010](createTestRunPackage.md).

Rule-TREE-PANEL-001 to Rule-TREE-PANEL-013 hold everywhere in the panel. They
are on [the tree panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Create Test Run dialog

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Create Test Run                                                           │
├────────────────────────────────────────────────────────────────────────────┤
│  v Configuration details                                 Collapse   (1)    │
│     Test Run name   [ cycle-2                              ]       (2)     │
│     Change Log      [ Story-002 (register new user), Sto...]               │
│     Commit ID       [ Commit hash, like 9f3c1ab...         ]               │
│     Test Type       [                                    v ]               │
│     Platform        [ Web                                 v ]              │
│     Component       [ Frontend                            v ]              │
│     Language        [                                     v ]              │
│     Browser         [                                     v ]       (3)    │
│                                                                            │
│  [x] v Test Cases                                                  (4)     │
│  [x]   v Accounts                                                          │
│  [x]       Login                                                           │
│  [x]       Registration                                                    │
│  [ ]     Checkout                                                          │
│                                                                            │
│                                                     [ Create ]      (5)    │
├────────────────────────────────────────────────────────────────────────────┤
│  Tab Navigate    Space Check    Escape Cancel                              │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Configuration details** — a section the tester can open and close. The gray
   hint on its right reads *Collapse*, and *Expand* once it is closed. Closing
   it takes every field below with it, the name included.
2. **Test Run name** — filled in with what the tester typed in the create
   dialog. Emptied, its gray hint text reads *Cycle-1*. The keyboard does not
   start here: it starts in **Change Log**.
3. **The fields** — one to a row. Every dropdown starts blank, which means
   unanswered, and every one can be typed into, so a value the list does not
   offer is still saved. **Browser** is on the form only while *Platform* is
   **Web** and *Component* is **Frontend**. **Device Type** replaces it only
   while *Platform* is **Mobile** and *Component* is **Frontend**. A field that
   is not on the form is saved empty, so switching *Platform* from **Web** to
   **Mobile** drops the browser that was picked. The lists are:

   | Field | Offers |
   |---|---|
   | Test Type | *Functional Test*, *Performance Test* |
   | Platform | *Web*, *Mobile* |
   | Component | *Frontend*, *Backend* |
   | Language | *English*, *Arabic*, *French* |
   | Browser | *Chrome*, *Firefox*, *Safari*, *Edge* |
   | Device Type | *iPhone*, *Samsung*, *Huawei* |

4. **The test cases** — the whole folder tree, each row with a tick box: a
   **Test Cases** row at the top, then each package, each test set, and each
   test case under it. It opens fully expanded. Ticking a folder ticks
   everything under it. A test set with no test cases, and a package holding
   only such test sets, is left out.
5. **Create** — enabled only while at least one test case is ticked. `Enter`
   does nothing here. The button is the only way to confirm. A click outside the
   dialog does not close it either; only `Escape` does.

**Edit Test Run** is this same dialog with the button **Save**. It opens filled
with the test run's own name, test cases and settings.

## Main flow

1. The tester selects **Test Runs** or a test run package.
2. The tester presses `Ctrl+M`, or chooses **Create**.
3. The **Create Run Node** dialog opens. Its first row is selected, and reads
   *Records execution results*. Its gray hint text reads *set name, like Sprint
   3 Cycle 1...*.
4. The tester types a name and presses `Enter`.
5. The **Create Test Run** dialog opens. It holds the typed name in *Test Run
   name*, a *Configuration details* form, and a tree of every live, non-empty
   test set with all test cases ticked. Retired test sets, anything under an
   **Archived** package, and empty test sets are not in that tree.
6. The tester ticks and unticks with `Space`, moves with `Tab`, and presses
   **Create**.
7. Testin writes the test run under a progress bar reading *Creating test run
   \<name\>*, which cannot be canceled. Every ticked test case is **Pending**,
   and the status is **Created**.
8. Its editor opens, and Testin shows *Run created*.

## What Testin refuses

**If no test case is ticked** — the **Create** button is disabled.

**If the name has been emptied** — the dialog stays open, and *A test run needs
a name* is shown in red.

**If a test run with that name already exists** — *\<name\> Already Exists* is
shown in red. Typing it in the first dialog closes that dialog and the second
one never opens. Typing it in **Create Test Run** leaves the dialog open with
everything still in it.

**If the parent folder was removed while the dialog was open** — the dialog
stays open, and *'\<parent\>' no longer exists - test run not created* is shown
in red.

**If writing the test run fails** — an IDE notification titled *Test Run Not
Created* stays in the notification log, with the reason under it.

**If several nodes are selected** — **Create** works on the first of them.

---

[Documentation](../README.md) › [The tree panel](main.md)
