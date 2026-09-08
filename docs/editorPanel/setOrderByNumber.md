[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-009

# UC-EDITOR-PANEL-009: Move a test case by typing its number

**As a** tester, **I want** to say a test case is number three,
**so that** I can put it where I want without dragging it through 200 rows.

The tester types the position they want. Testin moves the test case there.

`O` on the selected card.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, and
  the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-054** — The number is the position in the whole test set,
  counting from one.
- **Rule-EDITOR-PANEL-055** — Positions are read with this test case taken out
  of the list, so typing three puts it third.
- **Rule-EDITOR-PANEL-056** — Only this test case's own file is written. The
  test cases around it are not touched.

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
4. Testin works out a new place for it, between the two test cases it now sits
   between.
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
