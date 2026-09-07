[Documentation](../README.md) › The editor panel

# The editor panel

The editor panel is the middle of the IDE, where a test set or a test run opens
in a tab. It is where test cases are written, and where a test run is executed.

| | |
|---|---|
| **Part of Testin** | The editor panel |
| **Answers** | What a tester can do to test cases and to a test run, exactly what happens step by step, and what every screen looks like |
| **Numbering** | Use cases are `UC-EDITOR-PANEL-001` to `UC-EDITOR-PANEL-046`. Rules are `Rule-EDITOR-PANEL-001` and up, and belong to the editor panel |
| **Last rule** | `Rule-EDITOR-PANEL-190`. The next rule written here is `Rule-EDITOR-PANEL-191` |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `a53922a1`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

Two editors share this panel. A test set opens in one, a test run in the other.
They share the toolbar, the status bar, the two views and every key that is not
about a verdict.

| | What the tester does | |
|---|---|---|
| | **Opening and reading** | |
| **UC-EDITOR-PANEL-001** | [Open a test set and see its test cases](openTestSet.md) | |
| **UC-EDITOR-PANEL-002** | [Switch between cards and a grid](switchView.md) | |
| **UC-EDITOR-PANEL-003** | [Choose which fields are shown](chooseFields.md) | |
| **UC-EDITOR-PANEL-004** | [Change a grid column's width](changeColumnWidth.md) | |
| | **Writing test cases** | |
| **UC-EDITOR-PANEL-005** | [Create a test case](createTestCase.md) | |
| **UC-EDITOR-PANEL-006** | [Change one field of one test case](changeOneField.md) | |
| **UC-EDITOR-PANEL-007** | [Change one field on many test cases at once](bulkEdit.md) | |
| **UC-EDITOR-PANEL-008** | [Type straight into a grid cell](editGridCell.md) | |
| **UC-EDITOR-PANEL-009** | [Move a test case by typing its number](setOrderByNumber.md) | |
| **UC-EDITOR-PANEL-010** | [Reorder test cases by dragging](dragToReorder.md) | |
| **UC-EDITOR-PANEL-011** | [Remove test cases](removeTestCases.md) | |
| **UC-EDITOR-PANEL-012** | [Undo a change](undoChange.md) | |
| **UC-EDITOR-PANEL-013** | [Redo a change](redoChange.md) | |
| | **The clipboard** | |
| **UC-EDITOR-PANEL-014** | [Copy a test case's details as text](copyAsText.md) | |
| **UC-EDITOR-PANEL-015** | [Copy test cases](copyTestCases.md) | |
| **UC-EDITOR-PANEL-016** | [Cut test cases](cutTestCases.md) | |
| **UC-EDITOR-PANEL-017** | [Paste test cases](pasteTestCases.md) | |
| **UC-EDITOR-PANEL-018** | [Copy, cut and paste grid cells](gridClipboard.md) | |
| | **Finding what I want** | |
| **UC-EDITOR-PANEL-019** | [Search the test cases](searchTestCases.md) | |
| **UC-EDITOR-PANEL-020** | [Filter the test cases](filterTestCases.md) | |
| **UC-EDITOR-PANEL-021** | [Clear the filters](clearFilters.md) | |
| **UC-EDITOR-PANEL-022** | [Page through the test cases](pageThrough.md) | |
| **UC-EDITOR-PANEL-023** | [Change how many a page holds](changePageSize.md) | |
| **UC-EDITOR-PANEL-024** | [Select test cases](selectTestCases.md) | |
| | **Working from the editor** | |
| **UC-EDITOR-PANEL-025** | [Open the details panel](openDetailsPanel.md) | |
| **UC-EDITOR-PANEL-026** | [Step back](stepBack.md) | |
| **UC-EDITOR-PANEL-027** | [Refresh the editor from disk](refreshEditor.md) | |
| **UC-EDITOR-PANEL-028** | [See the test set's own details](nodeDetails.md) | |
| **UC-EDITOR-PANEL-029** | [Open the menu from the keyboard](keyboardMenu.md) | |
| | **The test run editor** | |
| **UC-EDITOR-PANEL-030** | [Open a test run and see what it covers](openTestRun.md) | |
| **UC-EDITOR-PANEL-031** | [Start executing by hand](startExecution.md) | |
| **UC-EDITOR-PANEL-032** | [Record that a test case passed](recordPassed.md) | |
| **UC-EDITOR-PANEL-033** | [Record that a test case is blocked](recordBlocked.md) | |
| **UC-EDITOR-PANEL-034** | [Record that a test case failed, and say why](recordFailed.md) | |
| **UC-EDITOR-PANEL-035** | [Stop executing](stopExecution.md) | |
| **UC-EDITOR-PANEL-036** | [Resume a run I stopped](resumeExecution.md) | |
| **UC-EDITOR-PANEL-037** | [Record a verdict out of order](recordOutOfOrder.md) | |
| **UC-EDITOR-PANEL-038** | [Correct a verdict I got wrong](correctVerdict.md) | |
| **UC-EDITOR-PANEL-039** | [Record one verdict on many test cases](bulkVerdict.md) | |
| **UC-EDITOR-PANEL-040** | [Change the failure details on their own](editFailureDetail.md) | |
| **UC-EDITOR-PANEL-041** | [Type an actual result into the grid](typeActualResult.md) | |
| **UC-EDITOR-PANEL-042** | [Watch how the run is going](watchProgress.md) | |
| **UC-EDITOR-PANEL-043** | [Run one test case's automation](runOneCase.md) | |
| **UC-EDITOR-PANEL-044** | [Run everything not yet judged](runWholeRun.md) | |
| **UC-EDITOR-PANEL-045** | [Write the result analysis](writeResultAnalysis.md) | |
| **UC-EDITOR-PANEL-046** | [Work in light mode](lightMode.md) | |

---

## What the panel is for

The tree says what exists. The editor is where the work happens.

A test set opens as a list of its test cases, and the tester writes them there.
A test run opens as the same list with a verdict beside each row, and the tester
walks it, judging each test case in turn.

Everything else in the panel exists to make those two things fast: two views of
the same rows, a filter, a search, and a key for every gesture.

**Five words, before the rules use them.**

- A **card** is one test case drawn as a block, with its description on top and
  its fields under it.
- The **grid** is the same test cases drawn as a table, one row each.
- A **field** is one thing a test case carries, such as its expected result.
- A **verdict** is what a test run records against one test case: **Passed**,
  **Failed** or **Blocked**.
- To **execute** a test run is to walk it test case by test case, recording a
  verdict for each.

---

## Rules that hold everywhere in the panel

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-004** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-005** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-006** — Each editor keeps an undo history of its own, and
  the tree keeps another.
- **Rule-EDITOR-PANEL-007** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-008** — Moving the view says nothing. Paging, filtering,
  searching and opening the details panel are all silent.
- **Rule-EDITOR-PANEL-009** — While a grid cell is open for editing, every key
  that would act on the row is refused.

---

## Every key the panel answers to

**Reading and moving**

| Key | What it does | The page that owns it |
|---|---|---|
| `Ctrl+F` | Puts the cursor in the search box | [UC-EDITOR-PANEL-019](searchTestCases.md) |
| `Ctrl+Right` | The next page | [UC-EDITOR-PANEL-022](pageThrough.md) |
| `Ctrl+Left` | The previous page | [UC-EDITOR-PANEL-022](pageThrough.md) |
| `Enter` | Opens the details panel, or a grid cell | [UC-EDITOR-PANEL-025](openDetailsPanel.md) |
| `Escape` | Steps back one step | [UC-EDITOR-PANEL-026](stepBack.md) |
| `Context Menu` | Opens the menu on the selection | [UC-EDITOR-PANEL-029](keyboardMenu.md) |
| `Ctrl` and the wheel | Changes the text size | [UC-SETTING-011](../setting/changeTextSize.md) |

**Writing test cases**

| Key | What it does | The page that owns it |
|---|---|---|
| `Ctrl+M` | Creates a test case | [UC-EDITOR-PANEL-005](createTestCase.md) |
| `F2` | Opens the menu of fields to change | [UC-EDITOR-PANEL-006](changeOneField.md) |
| `D` `E` `M` `T` `B` `S` `P` `G` `O` | Opens that one field straight away | [UC-EDITOR-PANEL-006](changeOneField.md) |
| `Delete` | Removes the selected test cases | [UC-EDITOR-PANEL-011](removeTestCases.md) |
| `Ctrl+Z` | Takes back the last change | [UC-EDITOR-PANEL-012](undoChange.md) |
| `Ctrl+Y` | Puts it back | [UC-EDITOR-PANEL-013](redoChange.md) |
| `Ctrl+Enter` | A line break inside a grid cell or a long field | [UC-EDITOR-PANEL-008](editGridCell.md) |

**The clipboard**

| Key | What it does | The page that owns it |
|---|---|---|
| `Ctrl+C` | On cards, copies the details as text. In the grid, copies the cells | [UC-EDITOR-PANEL-014](copyAsText.md) |
| `Ctrl+X` | In the grid, copies the cells and empties the ones that can be typed into | [UC-EDITOR-PANEL-018](gridClipboard.md) |
| `Ctrl+V` | In the grid, pastes text into the cells | [UC-EDITOR-PANEL-018](gridClipboard.md) |
| `Ctrl+Shift+C` | Copies the test cases themselves | [UC-EDITOR-PANEL-015](copyTestCases.md) |
| `Ctrl+Shift+X` | Cuts the test cases | [UC-EDITOR-PANEL-016](cutTestCases.md) |
| `Ctrl+Shift+V` | Pastes test cases into this test set | [UC-EDITOR-PANEL-017](pasteTestCases.md) |

**Executing a test run**

| Key | What it does | The page that owns it |
|---|---|---|
| `P` | Records **Passed** | [UC-EDITOR-PANEL-032](recordPassed.md) |
| `F` | Records **Failed**, and asks why | [UC-EDITOR-PANEL-034](recordFailed.md) |
| `B` | Records **Blocked** | [UC-EDITOR-PANEL-033](recordBlocked.md) |
| `F2` | Changes the failure details without changing the verdict | [UC-EDITOR-PANEL-040](editFailureDetail.md) |
| `F5` | Runs the selected test cases, or stops them | [UC-EDITOR-PANEL-043](runOneCase.md) |
| `Shift+F5` | Goes to the automation code | [UC-CODEGEN-006](../codegen/goToCode.md) |
| `Ctrl+F12` | **Automate Test Case**, which is not built | [UC-CODEGEN-005](../codegen/automateTestCase.md) |
| `Ctrl+P` | Generates a report on this test run | [UC-REPORT-001](../report/generateReport.md) |

**Nothing has a key** for: the first page, the last page, **Refresh**,
**Grid View**, **List View**, the fields button, the filter button, the node
details button, **Start Manual Execution**, **Stop Execution**, **Result
Analysis**, light mode, and stopping one running test case.

---

## The editor

```
┌────────────────────────────────────────────────────────────────────────────┐
│ (+) (refresh) (fields) (filter) (grid)   [ search...          ]  (details) │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   1. Log in with a valid user                        ( P1 ) ( Regression ) │
│      Expected Result: The dashboard opens.                                 │
│                                                                            │
│   2. Log in with a locked account                    ( P2 ) ( Smoke )      │
│      Expected Result: The account is refused.                              │
│                                                                            │
│   3. Log in with the wrong password                  ( P1 )                │
│      Expected Result: The password is refused.                             │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  1 of 12 test cases (filtered from 120)   |< < 1 of 3 > >|          [ 50 ] │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The toolbar** — create, refresh, choose the fields shown, filter, switch
   view, search, and the test set's own details.
2. **A card** — its number in the test set, then its description, then a badge
   for the priority and one for each group.
3. **The detail lines** — one for each field the tester chose to show.
4. **The status bar, left** — how many test cases are selected, of how many, and
   how many the filter hid.
5. **The status bar, middle** — first page, previous, which page of how many,
   next, last.
6. **The status bar, right** — how many test cases a page holds.

## The test run editor

The same shape, with three things added.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ (start) (light) (report) (refresh) (fields) (filter) (grid) [ search ] (..)│
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   1. Log in with a valid user                        ( P1 ) ( Passed )     │
│      Run Status: Passed                                                    │
│      Duration: 00:42                                                       │
│                                                                            │
│   2. Log in with a locked account                    ( P2 ) ( Failed )     │
│      Run Status: Failed                                                    │
│      Actual Result: The session was dropped.                               │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  2 of 12 test cases    |< < 1 of 1 > >|   In Progress                      │
│                        Passed 1 - Failed 1 - Pending 10   00:01:14  [ 50 ] │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Start Manual Execution** — the first button. It becomes **Stop Execution**
   while a run is going.
2. **Light mode** — the always on top window, on [its own page](lightMode.md).
3. **The run status** — on the right of the status bar, with the tree's own
   icon.
4. **The figures** — one for each verdict any test case carries. A verdict
   nobody recorded is not drawn.
5. **The clock** — how long this test run has been executing. It ticks once a
   second.

---

## Why the panel is built this way

**Two views of one list.** Cards are for reading one test case properly. The
grid is for comparing many and correcting them quickly. Both draw the same
rows, and the selection follows from one to the other.

**The grid is built late.** A test set of 2,770 test cases would cost a table
nobody asked for. The grid is built the first time the tester presses the
button.

**A key for everything a tester does often.** Executing a test run is `P`, `F`
and `B` and nothing else. The gestures a tester does twice a project, such as
switching view or refreshing, are buttons.

**One message per gesture.** Twenty test cases removed is *Removed 20*, not
twenty messages. Two places break this and they are named below.

---

## Where the plugin breaks its own rules, writing test cases

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | Rule-EDITOR-PANEL-007 — a message says what happened | `Ctrl+C` says *Details copied* and copies one line, the description. Every other field is left out. |
| **Difference 2** | Rule-EDITOR-PANEL-004 — a field the tester can fill in has a way in | **Test Data** and **Pre Conditions** cannot be filled in when a test case is created. Both are drawn in the dialog and neither has a key that opens it. |
| **Difference 3** | Rule-EDITOR-PANEL-004 — one word for one thing | The group boxes in the create dialog read **REGRESSION** and **SMOKE**. The badges, the filter and the grid beside them read **Regression** and **Smoke**. |
| **Difference 4** | Rule-EDITOR-PANEL-004 — what can be filtered can be set | Four groups can be filtered on and never assigned. The filter offers Security, UI, Functional and Validation. The dialog offers only Regression, Smoke and Sanity. |
| **Difference 5** | Rule-EDITOR-PANEL-001 — one name for one thing | Two buttons on one toolbar are both tooltipped **Details**. One picks which fields are shown, the other opens the test set's own details. |
| **Difference 6** | Rule-EDITOR-PANEL-007 — one message with a count | Pasting a block of 20 cells into the grid raises 20 messages. `Ctrl+X` over a block does the same. |
| **Difference 7** | Rule-EDITOR-PANEL-004 — what the tester typed is stored | A description typed into a grid cell loses characters Testin will not keep. If nothing else changed, nothing is saved and nothing is said, and the tester watches their text change. |
| **Difference 8** | Rule-EDITOR-PANEL-004 — one answer to one situation | A priority typed into a grid cell that Testin cannot read becomes the lowest. A status it cannot read keeps the value the test case already had. Two columns, two answers to a typo. |
| **Difference 9** | Rule-EDITOR-PANEL-004 — a key works the same on every machine | `Ctrl+M` is not made into `Cmd+M` on a Mac, though `Ctrl+C` and `Ctrl+F` are. The empty editor's second line reads *Press Ctrl+M to add* whatever machine it is on. |
| **Difference 10** | Rule-EDITOR-PANEL-008 — moving the view says nothing, and changes nothing | Selecting a test case that a filter is hiding throws every filter away, silently. It happens after creating a test case, after a drag, and after choosing a search result. |
| **Difference 11** | Rule-EDITOR-PANEL-004 — what the tester typed is stored | The page size box takes anything and quietly answers something else. `5000` becomes 1000. `0`, a negative number, letters and an empty box all become 50. |
| **Difference 12** | Rule-EDITOR-PANEL-002 — the two views show the same rows | Unticking **Order** stops three gestures in the grid working: clicking a row to select it, `Enter` to open the details panel, and the double-click. Nothing says why. |
| **Difference 13** | Rule-EDITOR-PANEL-008 — nothing is lost without being said | **Refresh** throws away every filter and the search text. The message afterwards says only *Refreshed*. |
| **Difference 14** | Rule-EDITOR-PANEL-004 — the tester sees what they did | A card dragged between two visible cards lands after whatever the filter is hiding between them. The message says *Re-sorted* and the test case is not where it was dropped. |
| **Difference 15** | Rule-EDITOR-PANEL-007 — one word for one act | Dragging cards says *Re-sorted*. Moving a test case by typing its number says *Updated*. The same act, two words. |
| **Difference 16** | Rule-EDITOR-PANEL-009 — a key that works is written down | `Shift+Enter` saves a bulk edit. The strip along the bottom names only `Enter`. |
| **Difference 17** | Rule-EDITOR-PANEL-008 — a search finds what is there | The search reads the description, the identity, the expected result and the steps. It does not read the module, the group, the test data or the pre-conditions, each of which has its own column and its own filter. |
| **Difference 18** | Rule-EDITOR-PANEL-008 — the status bar says what is true | With a row highlighted the status bar can read *0 of 12 test cases*, where the zero is meant as a position and reads as a count. |

## Where the plugin breaks its own rules, executing a test run

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 19** | Rule-EDITOR-PANEL-007 — a signed off test run records nothing more | The status bar's own tooltip says a completed or closed test run records no more verdicts. `P`, `F` and `B` still record one, still save it, and still say *Passed*. Only the automation refuses. |
| **Difference 22** | Rule-EDITOR-PANEL-007 — a change confirms itself once | The walk lands on test cases that already have a verdict, times them again, and re-stamps them with a new time and a new tester name. |
| **Difference 23** | Rule-EDITOR-PANEL-007 — a test run that is finished says so | Judging the last pending test case from the menu leaves the test run **In Progress**. Only the automation marks a test run completed. |
| **Difference 24** | Rule-EDITOR-PANEL-008 — nothing is lost without being said | **Refresh** stops the execution. The clock stops, the walk ends, and the button turns back into **Start Manual Execution**. The message says *Refreshed*. |
| **Difference 25** | Rule-EDITOR-PANEL-007 — one message with a count | An automated test run raises one message for each test case. Fifty test cases is fifty messages. |
| **Difference 26** | Rule-EDITOR-PANEL-004 — what the tester typed is kept | An automated pass still clears the actual result, the error, the bug severity and the bug priority the tester wrote by hand. It now says so afterwards, in a message that stays in the notification list. The dialog that asks first is still on the manual path only. |
| **Difference 27** | Rule-EDITOR-PANEL-007 — a message says what happened | Pressing `F5` to **stop** a test case marks the test run **In Progress** first, and raises that message. Stopping starts something. |
| **Difference 29** | Rule-EDITOR-PANEL-008 — the same | `Escape` in the failure dialog throws away everything typed, with no confirmation. |
| **Difference 30** | Rule-EDITOR-PANEL-008 — a key means one thing | `Ctrl+Right` turns the page here, and moves to the next test case in the view panel. It is the same key on two panels a tester uses together. |

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone | Was |
|---|---|
| **Difference 20** | A filtered walk completed the whole test run and turned every other pending test case untested. Fixed 7 September 2026, [#214](https://github.com/mtb550/test-in/issues/214) |
| **Difference 21** | **Start Manual Execution** was live on a test run holding no test cases and on a filter matching nothing, and pressing it marked the test run **In Progress**. Fixed 7 September 2026, [#215](https://github.com/mtb550/test-in/issues/215) |
| **Difference 28** | Closing the tab left the automation running and its verdicts homeless. Fixed 7 September 2026, [#222](https://github.com/mtb550/test-in/issues/222) |

---

## Not decided

**Question 1** — Should **Start Manual Execution** walk the whole test run or
the filtered list? It walks the filtered list today, and finishing it completes
the whole test run. Either the walk should ignore the filter, or completing
should only count what was walked.

**Question 2** — Should a verdict be recordable on a completed or closed test
run? The keyboard allows it, the automation refuses it, and the tooltip says it
is refused.

**Question 3** — Should a test case that already has a verdict be offered again
by the walk? It is offered today, and judging it again re-stamps who and when.

**Question 4** — Four groups can be filtered on and never assigned. Either the
create dialog should offer them or the filter should not.

---

[Documentation](../README.md) › **The editor panel**
