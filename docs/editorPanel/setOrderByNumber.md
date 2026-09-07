[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-009

# UC-EDITOR-PANEL-009: Move a test case by typing its number

**As a** tester, **I want** to say a test case is number three,
**so that** I can put it where I want without dragging it through 200 rows.

`O` on the selected card.

## Rules

- **Rule-EDITOR-PANEL-053** — The number is the position in the whole test set,
  counting from one.
- **Rule-EDITOR-PANEL-054** — Positions are read with this test case taken out
  of the list, so typing three puts it third.
- **Rule-EDITOR-PANEL-055** — Only this test case's own file is written. The
  test cases around it are not touched.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Update Order                                                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Log in with a valid user                                    │
│                                                              │
│  [   3 ]  of 12                                              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Save       Escape Cancel                         │
└──────────────────────────────────────────────────────────────┘
```

1. **The box** — the test case's position now.
2. **of, then the number** — how many test cases the test set holds.

## Main flow

1. The tester selects a card and presses `O`.
2. The **Update Order** dialog opens with the current position in the box.
3. The tester types a new number and presses `Enter`.
4. Testin works out a place between the two test cases it now sits between.
5. Only this test case's file is written.
6. A message reads *Updated*.
7. Every card is renumbered.

## What Testin refuses

**If the number is below one, or above how many test cases there are** — a
message says so, in the platform's own words, and nothing is saved.

**If the number is where the test case already is** — nothing is written at all.

**If several test cases are selected** — a message reads *Order is set one test
case at a time*.

## Where the plugin breaks its own rules

This says *Updated* when it finishes. Dragging cards to do the same thing says
*Re-sorted*. One act, two words. That is difference 15 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

---

[Documentation](../README.md) › [The editor panel](main.md)
