[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-044

# UC-EDITOR-PANEL-044: Run everything not yet judged

**As a** tester, **I want** to set the whole test run going and come back later,
**so that** a regression run of 200 test cases happens while I do something
else.

Testin runs every test case that has no verdict yet, in one go.

There is no key and no button in the editor. It starts from the tree, with
**Run Tests** on the test run.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, and
  the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-184** — Only test cases with no verdict are run. A test
  case already judged is never run again.
- **Rule-EDITOR-PANEL-185** — The whole set is one configuration and one
  process.
- **Rule-EDITOR-PANEL-186** — A test run whose every test case has been judged
  is marked **Completed** on its own.
- **Rule-EDITOR-PANEL-187** — The order the methods run in is the order of the
  test cases in their test set.
- **Rule-EDITOR-PANEL-188** — A test case with no generated method is dropped,
  and the rest still run.

## What the tester sees

This opens no screen of Testin's own. The test run's editor opens or comes to
the front, every pending card turns to running, and the IDE's own run window
opens below it.

Results land one at a time. Each one raises its own small message at the bottom
of the IDE, reading *Passed* or *Failed*.

## Main flow

1. The tester right-clicks the test run in the tree and chooses **Run Tests**.
2. The test run's editor opens, or comes to the front.
3. Testin gathers every test case with no verdict yet.
4. All of them are claimed by this editor.
5. They are handed to TestNG as one configuration.
6. Results come back one at a time and are written into the test run.
7. When the last one lands, the test run is marked **Completed**.

## What Testin refuses

**If the test run is completed or closed** — the tree entry is gray.

**If the test run is already running** — a message reads the test run's name,
then *is already running*.

**If nothing is pending** — a message reads the test run's name, then *has no
test cases to run*.

**If the tester is executing it by hand** — the same refusal as already running.

**If the IDE has no TestNG plugin** — nothing is offered.

## Where the plugin breaks its own rules

**One message per test case.** Two hundred test cases is two hundred messages.
That is difference 25 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

**A pass clears what a tester wrote by hand.** A test case is failed and written
up in the morning. It is re-run in the afternoon and passes. The actual result,
the error, the severity and the priority all go, with no dialog. That is
difference 26.

---

[Documentation](../README.md) › [The editor panel](main.md)
