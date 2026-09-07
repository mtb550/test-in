[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-019

# UC-CODEGEN-019: Work in an IDE with no Java plugin

**As a** tester, **I want** Testin to work in PyCharm or GoLand,
**so that** I can manage test cases where my team writes its code, even where
Testin cannot generate anything.

Nothing starts this. It is the state of the IDE.

## Rules

- **Rule-CODEGEN-061** — Test management works in full without the Java plugin.
  Only the code generation and the jumps into code are missing.
- **Rule-CODEGEN-062** — What is missing is not offered. The entries are absent
  from the menus rather than gray.
- **Rule-CODEGEN-063** — A plugin that is installed but switched off counts as
  missing, and switching it on needs the IDE restarted before Testin notices.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## What is missing

| Missing | What the tester sees |
|---|---|
| **Automate Test Case** | Not on any menu |
| **Navigate to Code** | Not on any menu, and the button is not drawn |
| The gutter marks | Not drawn in any editor |
| Every class and every method | Nothing is written, ever |

## What still works

The tree, both editors, the view panel, light mode, executing a test run by
hand, reports, export, import, and both kinds of sync. Everything except the
Java code.

## What Testin refuses

**If a generator is reached anyway** — a message titled **Java Plugin Not
Available** reads *Automation code generation and navigation require the Java
plugin, which is not available in this IDE.* It appears once for the whole code
project, not once for each test case.

**If navigation is reached anyway** — the same message, every time rather than
once.

## Where the plugin breaks its own rules

**TestNG without Java is worse than neither.** In an IDE that has TestNG and not
Java, **Run Test Case** is still offered. Every test case then resolves to no
method, and the tester gets one *has no generated code yet* message for each,
with nothing saying the Java plugin is the reason. That is difference 7 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
