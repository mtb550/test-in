[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-005

# UC-CODEGEN-005: Ask Testin to write the automation for me

**As a** tester, **I want** Testin to fill in what the test method actually
does, **so that** I get a working test rather than an empty method with a
comment in it.

`Ctrl+F12`, or the menu entry **Automate Test Case**.

## Rules

- **Rule 25** — This is not built. The menu entry and the key both answer with a
  message saying so, rather than doing nothing quietly.

Rules 1 to 6 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

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

The entry is live on every selected test case, and its description reads
*Generate automation code for the selected test case*. A tester cannot tell from
the menu that it does nothing until they press it. That is difference 2 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

The thing that really writes a missing method is filling in the test case's
description, and no label anywhere says so. That is
[UC-CODEGEN-003](getMissingMethod.md).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
