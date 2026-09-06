[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-008

# UC-CODEGEN-008: Run a test case's automation

**As a** tester, **I want** to run the generated method for one or more test
cases, **so that** the verdict is recorded without me judging it by hand.

`F5` on the selected test cases.

## Rules

- **Rule 31** — Whatever the tester selected is one run, not one run for each
  test case.
- **Rule 32** — The method is found by the test case's identity, never by its
  name.
- **Rule 33** — One message with a count, however many test cases started.
- **Rule 34** — A card turns to running the moment the tester presses the key,
  before the process exists.
- **Rule 35** — The order the methods run in is the order of the test cases in
  their test set.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## The three ways in

| The tester does this | Where |
|---|---|
| Presses `F5` | The list of test cases in either editor |
| Chooses **Run Test Case** | The menu in either editor |
| Clicks the run button | A card under the pointer, or the view panel |

Running everything a test run has not judged yet is different, and is
[UC-EDITOR-PANEL-017](../editorPanel/runWholeRun.md).

## Main flow

1. The tester selects three test cases and presses `F5`.
2. Every card turns to running at once.
3. Testin finds the method for each test case by its identity.
4. The three are handed to TestNG as one configuration.
5. A message reads *Running 3*.
6. Each result comes back and is recorded against the test case.

## What Testin refuses

**If nothing is selected** — the entry is gray and the key does nothing.

**If a test case has no method** — that test case is dropped, its card goes back
to how it was, and a message reads its description and then *has no generated
code yet*. The other test cases still run.

**If none of them has a method** — nothing starts, and no running message
appears.

**If every selected test case is already running** — nothing starts and nothing
is said.

**If the IDE is indexing** — every test case is put back and a message reads
*Cannot run tests while IntelliJ is indexing. Please wait a moment.*

**If indexing starts part way through** — every test case is put back and a
message reads *Indexing interrupted the test run. Please try again.*

**If the IDE has no TestNG plugin** — the entry is not on the menu and the
button is not drawn.

## What the run is called

The name in the IDE's run widget depends on the selection.

| The tester selected | The name |
|---|---|
| One test case | The class name, a dot, then the method name |
| Several in one test set | The class name |
| Several across test sets | The first class name, then *and*, then how many more |

## Where the plugin breaks its own rules

**TestNG alone is not enough.** Running needs the Java plugin to find the
method, and only TestNG is checked before **Run Test Case** is offered. In an
IDE with TestNG and no Java plugin, every test case resolves to nothing and the
tester gets one *has no generated code yet* message for each, with no mention of
the missing plugin. That is difference 7 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
