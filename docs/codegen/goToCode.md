[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-006

# UC-CODEGEN-006: Go to the code from a test case

**As a** tester, **I want** to open the method behind the test case I am looking
at, **so that** I can read or change what the automation really does.

`Shift+F5` on the selected test case.

## Rules

- **Rule 26** — The method is found by the identity in `testName`, so a test
  case that has been renamed still finds its method.
- **Rule 27** — Without the Java plugin the gesture is not offered at all.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## The three ways in

| The tester does this | Where |
|---|---|
| Presses `Shift+F5` | The list of test cases in either editor |
| Chooses **Navigate to Code** | The menu in either editor |
| Clicks the class button | A card under the pointer, or the view panel |

## Main flow

1. The tester selects a test case and presses `Shift+F5`.
2. Testin works out the class from the test case's place in the tree.
3. Testin finds the method carrying that test case's identity.
4. The Java file opens with the caret on that method.

## What Testin refuses

**If the test case has no method** — a message titled **Nothing to open** reads
*No automation has been generated for*, then the description, then *yet*.

**If the IDE is still indexing** — a message reads **Waiting for indexing**, and
the jump happens when indexing finishes.

**If the class cannot be found** — nothing opens, and only the log says so.

**If the IDE has no Java plugin** — the entry is not on the menu and the button
is not drawn. Reached anyway, a message titled **Java Plugin Not Available**
appears, every time rather than once.

## Where the plugin breaks its own rules

**One state, two sentences.** A test case with no method says *has no generated
code yet* when it is run and **Nothing to open** when it is jumped to. That is
difference 5 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

**One action, two pictures.** The menu draws an arrow. The card and the view
panel draw a class icon.

**The key does not work in the view panel**, though the button there says it
does. That is difference 1 on
[the view panel page](../viewPanel/main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
