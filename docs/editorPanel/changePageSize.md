[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-023

# UC-EDITOR-PANEL-023: Change how many a page holds

**As a** tester, **I want** more test cases on a page,
**so that** I can see a whole test set of 80 without turning pages.

This is the small box at the right of the status bar. It holds 50 to start with.

There is no key for this. The box is at the right of the status bar.

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
- **Rule-EDITOR-PANEL-105** — A page holds 50 test cases until the tester says
  otherwise.
- **Rule-EDITOR-PANEL-106** — The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-107** — Changing it goes back to the first page and
  returns the keyboard to the list.

## What the tester sees

This opens no screen. The tester types into the small box at the right of the
status bar, and the list is drawn again from the first page.

Nothing is said. A number Testin cannot use is corrected in the box itself, so
the tester can see what happened.

## Main flow

1. The tester clicks the small box at the right of the status bar.
2. The tester types 200 and presses `Enter`.
3. The view is drawn again from the first page, 200 test cases at a time.
4. The keyboard goes back to the list.
5. Nothing is said.

## What Testin refuses

**Nothing is refused.** Every value is taken. Testin quietly turns it into
something it can use.

| The tester types | What the page holds |
|---|---|
| A number from 1 to 1000 | That many |
| Anything above 1000 | 1000 |
| `0`, or a negative number | 50 |
| Letters, or nothing at all | 50 |

The box is corrected in place, so the tester can see what happened. No message
is raised.

## Where the plugin breaks its own rules

Typing `5000` and getting 1000 is a refusal with no words. Typing `0` and
getting 50 is another. Every other refusal in Testin says what it did. That is
difference 11 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

---

[Documentation](../README.md) › [The editor panel](main.md)
