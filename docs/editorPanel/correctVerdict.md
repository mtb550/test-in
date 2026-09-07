[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-038

# UC-EDITOR-PANEL-038: Correct a verdict I got wrong

**As a** tester, **I want** to change a verdict I recorded by mistake,
**so that** the test run says what really happened.

There is no special gesture. The tester records the right verdict, and it is
written over the wrong one.

Press the right verdict's key on the test case.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing. Paging, filtering,
  searching and opening the details panel are all silent. It changes nothing
  either: a test case the filter is hiding is reported, never brought into view
  by throwing the filter away.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-159** — A verdict is simply written over. There is no
  separate gesture for correcting one.
- **Rule-EDITOR-PANEL-160** — Correcting a verdict re-stamps who recorded it and
  when. The original tester and the original time are gone.
- **Rule-EDITOR-PANEL-161** — Changing a failed test case to passed asks first,
  because it clears four things.
- **Rule-EDITOR-PANEL-162** — Only passing clears anything. Failing and blocking
  clear nothing.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Passed                                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Passing this case clears the actual result and the          │
│  stacktrace, because a case that passed has nothing to       │
│  explain. There is no copy of it anywhere else.              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Passed       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the verdict being recorded.
2. **The message** — names exactly what will be cleared, from the four.
3. **The confirm word** — the verdict, again.

## Main flow

1. A test case is recorded as **Failed**, with an actual result and an error.
2. The tester realizes it really passed.
3. The tester selects it and presses `P`.
4. The confirmation opens, naming what will be cleared.
5. The tester presses `Enter`.
6. **Passed** is recorded, and the four fields are cleared.
7. A message reads *Passed*.

## What Testin refuses

**If nothing would be cleared** — no confirmation is shown. The verdict is
simply written over.

**If the tester presses `Escape`** — nothing is changed at all.

**If several test cases are selected** — the confirmation is asked once for the
whole selection. Its message says *these*, then the count, then *cases*.

## What cannot be undone

There is no undo for a verdict inside the test run editor. `Ctrl+Z` there
belongs to the test cases, not to the test run. A verdict written over is gone.
So is anything that clearing it removed.

**There is no way to clear a verdict back to nothing.** **Pending**,
**Untested** and **Removed** have no key and are on no menu.

## Where the plugin breaks its own rules

**The warning is only on this path.** An automated pass clears the same four
fields with no dialog at all. That is difference 26 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

---

[Documentation](../README.md) › [The editor panel](main.md)
