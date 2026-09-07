[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-044

# UC-EDITOR-PANEL-044: Run everything not yet judged

**As a** tester, **I want** to set the whole test run going and come back later,
**so that** a regression run of 200 test cases happens while I do something
else.

There is no key and no button in the editor. It starts from the tree, with
**Run Tests** on the test run.

## Rules

- **Rule-EDITOR-PANEL-180** — Only test cases with no verdict are run. A test
  case already judged is never run again.
- **Rule-EDITOR-PANEL-181** — The whole set is one configuration and one
  process.
- **Rule-EDITOR-PANEL-182** — A test run whose every test case has been judged
  is marked **Completed** on its own.
- **Rule-EDITOR-PANEL-183** — The order the methods run in is the order of the
  test cases in their test set.
- **Rule-EDITOR-PANEL-184** — A test case with no generated method is dropped,
  and the rest still run.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

**A pass clears what a tester wrote by hand.** A test case failed and written up
in the morning, re-run in the afternoon and passing, loses the actual result,
the error, the severity and the priority with no dialog. That is difference 26.

---

[Documentation](../README.md) › [The editor panel](main.md)
