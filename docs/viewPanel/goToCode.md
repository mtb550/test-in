[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-014

# UC-VIEW-PANEL-014: Go to the automation code

**As a** tester, **I want** to open the test method Testin wrote for this test
case, **so that** I can read or change what the automation actually does.

There is no key that works here. The button's tooltip names `Shift+F5`, and
`Shift+F5` does nothing in the panel.

## Rules

- **Rule-VIEW-PANEL-055** — The button is drawn only where the IDE has the Java
  plugin.
- **Rule-VIEW-PANEL-056** — The button is the first of the two, before the run
  button.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The panel is showing a test case that has automation code.
2. The tester clicks the first button, whose tooltip reads **Navigate to Code**.
3. The Java file opens with the caret on the test method for this test case.

The button is drawn on [UC-VIEW-PANEL-012](runFromPanel.md).

## What Testin refuses

**If the IDE has no Java plugin** — the button is not drawn.

**If the action is reached without the Java plugin** — a message titled **Java
Plugin Not Available** reads *Automation code generation and navigation require
the Java plugin, which is not available in this IDE.*

**If the test case has no automation code** — a message titled **Nothing to
open** reads *No automation has been generated for*, then the test case's
description, then *yet*.

## Where the plugin breaks its own rules

**The tooltip names a key that does nothing here.** It reads `Shift+F5`, and
that key is bound to the cards in the editor. That is difference 1 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

**The same action has two pictures.** On a menu it is an arrow. Here and on a
card it is a class icon.

---

[Documentation](../README.md) › [The view panel](main.md)
