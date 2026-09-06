[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-001

# UC-VIEW-PANEL-001: Open a test case's details

**As a** tester, **I want** the whole of one test case in front of me,
**so that** I can read its steps and its expected result without leaving the
screen I am working in.

`Enter` opens it on whatever is selected. Five gestures open it in all.

## Rules

- **Rule 10** — Five gestures open the panel. Each of them opens the tool window
  if it was closed.
- **Rule 11** — Four of the five move the keyboard into the panel. Choosing a
  search result does not.
- **Rule 12** — The panel opens on the **Details** tab, whichever tab was in
  front last time.
- **Rule 13** — Two gestures can hand over more than one test case. The panel
  shows the first, and the rest are reached with the paging keys.
- **Rule 14** — The panel is handed the folder the test case was opened from.
  That folder decides whether the run rows are drawn.

Rules 1 to 9 hold everywhere in the panel. They are on
[the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The five ways in

| The tester does this | Test cases handed over | Keyboard moves |
|---|---|---|
| Double-clicks a card in an editor | One | Yes |
| Presses `Enter`, or chooses **View Details** | Every one selected | Yes |
| Presses `Enter` on the number column of a grid row, or double-clicks it | One | Yes |
| Clicks the mark beside a generated test method | One | Yes |
| Chooses a test case in the search | One | No |

## Main flow

1. The tester selects one or more test cases in an editor.
2. The tester presses `Enter`, or chooses **View Details** from the menu.
3. The view panel opens on the right, if it was closed.
4. The **Details** tab comes to the front.
5. The panel draws the first test case in full.
6. The keyboard moves into the panel, so `F2` works straight away.

## What Testin refuses

**If nothing is selected** — **View Details** is gray, and `Enter` does nothing.

**If the double-click lands between two cards** — nothing opens, and nothing is
said.

**If the tester presses `Enter` on any grid column but the number** — the panel
does not open. The cell opens for editing instead, or nothing happens.

**If the tester clicks the mark beside a test method whose test case was
removed** — nothing opens and nothing is said. Only the log records it.

**If the search result is a package, a folder or a test project** — there is no
test case, so the panel is never touched.

## Where the plugin breaks its own rules

Opening the view panel before anything else in Testin can raise *Testin Setup
Required*, the message that asks for the Testin folder. The tester asked to read
a test case and was handed a settings notification. That is difference 12 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The view panel](main.md)
