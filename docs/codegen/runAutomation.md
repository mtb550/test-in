[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-008

# UC-CODEGEN-008: Run a test case's automation

**As a** tester, **I want** to run the generated method for one or more test
cases, **so that** the verdict is recorded without me judging it by hand.

The code runs, and Testin writes down whether each test case passed.

`F5` on the selected test cases.

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — Testin writes only the method's annotation and its
  declaration. The body is the tester's, and Testin never touches it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-031** — Whatever the tester selected is one run, not one run
  for each test case.
- **Rule-CODEGEN-032** — The method is found by the test case's identity, never
  by its name.
- **Rule-CODEGEN-033** — One message with a count, however many test cases
  started, and it counts the ones that actually started. It appears once Testin
  has found their methods, not when the key was pressed.
- **Rule-CODEGEN-034** — A card turns to running the moment the tester presses
  the key, before the process exists.
- **Rule-CODEGEN-035** — The order the methods run in is the order of the test
  cases in their test set.
- **Rule-CODEGEN-074** — A test case that cannot run is reported once. One says
  its description; several say how many, because the descriptions are on the
  cards in front of the tester.

## The three ways in

| The tester does this | Where |
|---|---|
| Presses `F5` | The list of test cases in either editor |
| Chooses **Run Test Case** | The menu in either editor |
| Clicks the run button | A card under the pointer, or the view panel |

Running everything a test run has not judged yet is different, and is
[UC-EDITOR-PANEL-017](../editorPanel/runWholeRun.md).

## What the tester sees

Every selected card turns to running at once, and its run button becomes a stop
button. That is the answer to the key; the message comes a moment later, once
Testin has found the methods. It appears near the bottom right of the IDE,
reading *Running* and then how many actually started. The IDE's own run window
opens underneath and shows what TestNG is doing.

## Main flow

1. The tester selects three test cases and presses `F5`.
2. Every card turns to running at once.
3. Testin finds the method for each test case by its identity.
4. The three are handed to TestNG as one configuration.
5. A message reads *Running 3* — the number that started, said once Testin
   knows it.
6. Each result comes back and is recorded against the test case.

## What Testin refuses

**If nothing is selected** — the entry is gray and the key does nothing.

**If a test case has no method** — that test case is dropped. Its card goes
back to how it was, and the other test cases still run. One message says so: for
a single test case it reads its description and then *has no generated code
yet*; for several it reads how many *test cases have no generated code yet*.
There is never one message per test case.

**If none of them has a method** — nothing starts, and no running message
appears. The message saying how many have no generated code still does.

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
method. Only TestNG is checked before **Run Test Case** is offered. So in an IDE
with TestNG and no Java plugin, no test case finds its method. The tester is
told how many have no generated code, and nothing mentions the missing plugin.
That is difference 7 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
