[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-005

# UC-CODEGEN-005: Ask Testin to write the automation for me

**As a** tester, **I want** Testin to fill in what the test method actually
does, **so that** I get a working test rather than an empty method with a
comment in it.

Testin cannot do this yet. The entry is on the menu, gray, and its own name says
so.

`F12`, or the menu entry **Automate Test Case**.

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — Testin writes only the method's annotation and its
  declaration. The body is the tester's, and Testin never touches it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-071** — The entry is gray until there is something behind it,
  and its own name says why: **Automate Test Case (not built yet)**. A control
  that cannot work is shown and disabled with the reason, never left out — a
  tester who cannot see it cannot learn it is coming.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-025** — This is not built. The menu entry is gray and says so
  in its name. The key does nothing.

## The screen

The only thing this use case draws is its own menu entry, gray.

```
┌──────────────────────────────────────────┐
│  Automate Test Case (not built yet)      │
└──────────────────────────────────────────┘
```

1. **The name** — the entry's name, with *(not built yet)* after it.
2. **The gray** — the entry cannot be chosen.
3. **The description** — shown where the IDE shows one, such as **Find Action**.
   It says a test case's method is written when the case is saved with a
   description, and that generating one for a case that already exists is a
   later release.

## What happens today

1. The tester right-clicks a test case.
2. The menu shows **Automate Test Case (not built yet)**, gray.
3. Pressing `F12` does nothing. Nothing is written.

## What Testin refuses

**Always.** The entry never generates anything.

**If nothing is selected** — the entry is gray.

**If the IDE has no Java plugin** — the entry is still on the menu, grayed,
reading *(needs the Java plugin)*.

## What really writes a missing method

Filling in the test case's description. That is
[UC-CODEGEN-003](getMissingMethod.md), and this entry's own description says so
while it is gray.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
