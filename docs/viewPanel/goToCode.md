[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-014

# UC-VIEW-PANEL-014: Go to the automation code

**As a** tester, **I want** to open the test method Testin wrote for this test
case, **so that** I can read or change what the automation actually does.

There is no key that works here. The button's tooltip names `Shift+F5`, and
`Shift+F5` does nothing in the panel.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE. Its
  stripe reads **Testin**, exactly as the tree panel's does. The side they are
  on is the only thing that tells them apart.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has three tabs, and all three are drawn
  every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing any Testin editor empties the panel.
- **Rule-VIEW-PANEL-056** — The button is drawn only where the IDE has the Java
  plugin.
- **Rule-VIEW-PANEL-057** — The button is the first of the two, before the run
  button.

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
