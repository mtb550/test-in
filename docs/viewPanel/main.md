[Documentation](../README.md) › The view panel

# The view panel

The view panel is the panel on the right of the IDE. It shows one test case at a
time: everything the test case says, and everything one test run recorded about
it.

| | |
|---|---|
| **Part of Testin** | The view panel |
| **Answers** | What the panel shows, how a test case gets into it, and what a tester can do from it |
| **Numbering** | Use cases are `UC-VIEW-PANEL-001` to `UC-VIEW-PANEL-015`. Rules are `Rule-VIEW-PANEL-001` to `Rule-VIEW-PANEL-059` |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `779fe6b4`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

| | What the tester does | |
|---|---|---|
| | **Getting a test case into the panel** | |
| **UC-VIEW-PANEL-001** | [Open a test case's details](openDetails.md) | |
| **UC-VIEW-PANEL-002** | [Let the panel follow the selection](followSelection.md) | |
| **UC-VIEW-PANEL-003** | [Page through several test cases](pageThroughCases.md) | |
| | **Reading** | |
| **UC-VIEW-PANEL-004** | [Read what a test case says](readTestCase.md) | |
| **UC-VIEW-PANEL-005** | [Read what a test run recorded](readRunResult.md) | |
| **UC-VIEW-PANEL-006** | [Read the whole stacktrace of a failure](readStacktrace.md) | |
| **UC-VIEW-PANEL-007** | [Read a test case's history](readHistory.md) | |
| **UC-VIEW-PANEL-008** | [See the bugs still open on a test case](seeOpenBugs.md) | |
| | **Working from the panel** | |
| **UC-VIEW-PANEL-009** | [Copy a test case's identity](copyIdentity.md) | |
| **UC-VIEW-PANEL-010** | [Go to the test set the test case lives in](goToTestSet.md) | |
| **UC-VIEW-PANEL-011** | [Change one field without leaving the panel](changeOneField.md) | |
| **UC-VIEW-PANEL-012** | [Run a test case from the panel](runFromPanel.md) | |
| **UC-VIEW-PANEL-013** | [Stop a test case from the panel](stopFromPanel.md) | |
| **UC-VIEW-PANEL-014** | [Go to the automation code](goToCode.md) | |
| **UC-VIEW-PANEL-015** | [Close the panel](closePanel.md) | |

---

## What the panel is for

A test case is too big for a card and too big for a grid row. A card shows a
title. A grid row shows the fields that fit. The view panel shows all of it,
beside whatever the tester is working in.

It is built for one moment above all. A tester is executing a test run, has just
recorded a failure, and needs the expected result, the steps and the test data
in front of them while they write down what actually happened.

**Three words, before the rules use them.**

- A **field** is one thing a test case carries, such as its expected result, its
  steps or its module.
- A **verdict** is what one test run recorded against this test case:
  **Passed**, **Failed** or **Blocked**.
- To **follow the selection** is what the panel does once it is open. The tester
  moves to another test case, and the panel moves with them.

---

## Rules that hold everywhere in the panel

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE. Its
  stripe reads **Testin**, exactly as the tree panel's does. The side they are
  on is the only thing that tells them apart.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has three tabs, and all three are drawn
  every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing any Testin editor empties the panel.

---

## Every key the panel answers to

| Key | What it does | The page that owns it |
|---|---|---|
| `Enter` | Opens the panel on the selected test cases | [UC-VIEW-PANEL-001](openDetails.md) |
| `F2` | Opens the menu that changes one field | [UC-VIEW-PANEL-011](changeOneField.md) |
| `Ctrl+Right` | Moves to the next test case | [UC-VIEW-PANEL-003](pageThroughCases.md) |
| `Ctrl+Left` | Moves to the previous test case | [UC-VIEW-PANEL-003](pageThroughCases.md) |
| `Escape` | Closes the panel, pressed in the editor | [UC-VIEW-PANEL-015](closePanel.md) |
| `Ctrl` and the mouse wheel | Makes every Testin text bigger or smaller | [UC-SETTING-011](../setting/changeTextSize.md) |

Two more keys are written in the panel's own tooltips and do nothing there. They
are difference 1 below.

---

## The panel

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin                                                     ( < )  ( > )   │
├────────────────────────────────────────────────────────────────────────────┤
│   Details      History      Open Bugs                                      │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   Demo  >  Test Cases  >  Accounts  >  Login                               │
│                                                                            │
│   ( 3f2a05c1-...-9c1b )  [copy]                                            │
│                                                                            │
│   Log in with a valid user                                                 │
│                                                                            │
│   [ go to code ]  [ run ]                                                  │
│                                                                            │
│   ( P1 )  ( Smoke )  ( Regression )  ( Failed )                            │
│                                                                            │
│   Run Status          Failed                                               │
│   Duration            02:14                                                │
│   Actual Result       The session was dropped.                             │
│   Stacktrace          java.lang.AssertionError: expected [true]            │
│                         at org.testin.demo.LoginTest.valid                 │
│                         at org.testng.internal.Invoker.invoke              │
│                       Show all 42 lines                                    │
│   Bug Severity        Blocker                                              │
│   Bug Priority        High                                                 │
│                                                                            │
│   Expected Result:    The dashboard opens.                                 │
│   Steps:              1- Open the login page.                              │
│                       2- Type the credentials.                             │
│                       3- Press Sign in.                                    │
│   Pre Conditions:     An account exists.                                   │
│   Test Data:          user=admin                                           │
│   Module:             Accounts                                             │
│   Created By:         muteb                                                │
│   Created At:         2 September 2026                                     │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The two arrows** — move to the previous and the next test case. They are
   gray when the panel was handed only one.
2. **The three tabs** — **Details**, **History** and **Open Bugs**. Only
   **Details** has anything in it today.
3. **The path** — one step for each folder above the test case. Only the last
   step opens anything.
4. **The identity** — the test case's own identity, with a button that copies
   it.
5. **The title** — the test case's description.
6. **The two buttons** — go to the automation code, and run the test case. Each
   is drawn only where the IDE has the plugin it needs.
7. **The badges** — the priority, then one for each group, then the verdict.
8. **The run rows** — what one test run recorded. They are drawn only when the
   panel was opened from a test run.
9. **The test case rows** — what the test case says. Every empty one is left
   out.

The captions on the run rows have no colon. The captions on the test case rows
do. That is difference 8 below.

---

## Why the panel is built this way

**It refuses to reappear.** A tester who closes the panel has said they want the
screen. Following the selection would open it again on the next click, so
following happens only while it is already open.

**It reads again instead of remembering.** The panel is handed a test case, and
then never draws that copy. It asks Testin's memory again every time. A tester
can change a field in the grid and watch the panel change with it.

**One panel for each code project.** Two code projects open at once used to
share one panel, so a test case from the first appeared in the second.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | Rule-VIEW-PANEL-003 — a key that is written is a key that works | The run button's tooltip says `F5`. The go to code button's says `Shift+F5`. Neither key does anything while the focus is in the panel. Both work on the cards in the editor. |
| **Difference 2** | Rule-VIEW-PANEL-004 — the tester closes the panel when they want the screen | `Escape` closes the panel from the editor, and does nothing from inside the panel. A tester who has just used `F2`, which needs the focus in the panel, cannot close it with `Escape`. |
| **Difference 3** | Rule-VIEW-PANEL-005 — reading a panel never changes anything | Clicking the last step of the path fails when the panel was opened from a test run. It looks for a test set where a test run is, finds none, and stops with an internal error the tester cannot read. |
| **Difference 4** | Rule-VIEW-PANEL-003 — what looks clickable is clickable | Every step of the path takes a hand pointer and underlines itself. Only the last one does anything. Clicking **Test Cases** to go up a level does nothing, and says nothing. |
| **Difference 5** | Rule-VIEW-PANEL-008 — the three tabs describe the same test case | A failed test case shows **Blocker** and **High** on the Details tab, while the tab beside it reads *No bugs found for this test case.* The Open Bugs tab is not built, and never looks at the test case. |
| **Difference 6** | Rule-VIEW-PANEL-006 — nothing is drawn about a test case that is not there | With no test case selected, Details reads *Select a test case to view details* and Open Bugs reads *No bugs found for this test case.* There is no test case. |
| **Difference 7** | Rule-VIEW-PANEL-001 — a tester can tell the two panels apart | Both Testin tool windows are named **Testin**. This is the tree panel's difference too. |
| **Difference 8** | Rule-VIEW-PANEL-006 — one look for one kind of thing | In one column of one panel, the run captions have no colon and the test case captions do. |
| **Difference 9** | Rule-VIEW-PANEL-009 — closing an editor empties the panel | It empties the panel whichever editor closed. A tester reading a test case from the first editor, who closes the second, watches the panel go blank for no reason they can see. |
| **Difference 10** | Rule-VIEW-PANEL-007 — a change confirms itself once | An edit made with `F2` that Testin cannot find a place to write is dropped. No message, no balloon, nothing on screen. Only the log says so. |
| **Difference 11** | Rule-VIEW-PANEL-008 — the panel redraws when its test case changes | Every result a running test reports redraws the whole panel, whichever test case reported. A test run of 50 test cases rebuilds the panel 50 times, including the two tabs that never change. |
| **Difference 12** | Rule-VIEW-PANEL-003 — the panel opens when the tester asks for a test case | Opening the view panel first, before anything else in Testin, can raise *Testin Setup Required*. The tester asked to read a test case and was handed a settings notification. |

---

## Not decided

**Question 1** — The History tab is empty and says so. Nobody has decided what a
test case's history should hold, or where it would be read from.

**Question 2** — The Open Bugs tab is empty and says so. Nothing in Testin
tracks a bug beyond the severity and the priority written on a failed verdict.

**Question 3** — Should the panel show a test case at all when it was opened
from a test run that does not hold it? It shows the test case with no run rows
today, which reads as a test case nobody has run.

---

[Documentation](../README.md) › **The view panel**
