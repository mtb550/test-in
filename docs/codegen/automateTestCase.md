[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-005

# UC-CODEGEN-005: Ask Testin to write the automation for me

**As a** tester, **I want** Testin to fill in what the test method actually
does, **so that** I get a working test rather than an empty method with a
comment in it.

Testin cannot do this yet. The entry says so when it is pressed.

`Ctrl+F12`, or the menu entry **Automate Test Case**.

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
- **Rule-CODEGEN-025** — This is not built. The menu entry and the key both
  answer with a message saying so, rather than doing nothing quietly.

## The screen

The only thing this use case draws. It is a small red message near the bottom
right of the IDE, above the status bar.

```
┌──────────────────────────────────────────────────────────────┐
│  Not built yet                                               │
│  Generating automation code for a test case is coming in a   │
│  later release.                                              │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always these three words.
2. **The line under it** — always the same sentence.
3. **The color** — red, because nothing happened.
4. **How long it stays** — about five seconds, then it fades on its own. It is
   not kept in the IDE's notification list.

## What happens today

1. The tester selects a test case and presses `Ctrl+F12`.
2. A message titled **Not built yet** reads *Generating automation code for a
   test case is coming in a later release.*
3. Nothing is written.

## What Testin refuses

**Always.** The entry never generates anything.

**If nothing is selected** — the entry is gray.

**If the IDE has no Java plugin** — the entry is not on the menu at all.

## Where the plugin breaks its own rules

The entry is live on every selected test case. Its description reads *Generate
automation code for the selected test case*. Nothing on the menu tells the
tester that it does nothing. They find out by pressing it. That is difference 2
on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

The thing that really writes a missing method is filling in the test case's
description, and no label anywhere says so. That is
[UC-CODEGEN-003](getMissingMethod.md).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
