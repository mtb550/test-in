[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-006

# UC-CODEGEN-006: Go to the code from a test case

**As a** tester, **I want** to open the method behind the test case I am looking
at, **so that** I can read or change what the automation really does.

This opens the Java method that runs the selected test case.

`Shift+F5` on the selected test case.

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
- **Rule-CODEGEN-026** — The method is found by the identity in `testName`, so a
  test case that has been renamed still finds its method.
- **Rule-CODEGEN-027** — Without the Java plugin the gesture is not offered at
  all.

## The three ways in

| The tester does this | Where |
|---|---|
| Presses `Shift+F5` | The list of test cases in either editor |
| Chooses **Navigate to Code** | The menu in either editor |
| Clicks the class button | A card under the pointer, or the view panel |

## The screen

The Java file opens in the IDE's own editor, and the caret lands on the method
that runs this test case.

```
┌──────────────────────────────────────────────────────────────────────────┐
│     40      @Test(description = "Log in with a valid user",              │
│     41            testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",     │
│     42            priority = 3)                                          │
│     43      public void |logInWithAValidUser() {                         │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The caret** — drawn here as `|`. It lands on the method, not at the top of
   the file.
2. **The line numbers** — the file is scrolled to the method, so a long class
   does not have to be searched.

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

**If the IDE has no Java plugin** — the entry is not on the menu, and the
button is not drawn. If it is reached anyway, a message titled **Java Plugin Not
Available** appears. It appears every time, not once.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
