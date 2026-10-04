[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-038

# UC-EDITOR-PANEL-038: Correct a run item status I got wrong

**As a** tester, **I want** to change a run item status I recorded by mistake, **so that** the test run says what really
happened.

There is no special gesture. The tester records the right run item status, and
it is written over the wrong one.

Press the key for the right run item status on the test case.

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
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, kept
  against the test set it is showing, and the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-159** — A run item status is simply written over. There is
  no separate gesture for correcting one.
- **Rule-EDITOR-PANEL-160** — Correcting a run item status re-stamps who
  recorded it and when. The original tester and the original time are gone.
- **Rule-EDITOR-PANEL-161** — Changing a failed test case to passed asks first,
  because it clears the actual result, the error, the screenshots and the bug
  issue link. It also puts a bug severity or priority the tester chose back to
  Enhancement and Low.
- **Rule-EDITOR-PANEL-162** — Only passing clears anything. Failing and blocking
  clear nothing.
- **Rule-EDITOR-PANEL-240** — Correcting a run item status keeps the test case
  it was given against. Only three things change: the run item status, who gave
  it and when.
- **Rule-EDITOR-PANEL-248** — A button that turns gray while the pointer is on
  it loses its hover look at once, so a control that has stopped working never
  looks ready to press.
- **Rule-EDITOR-PANEL-249** — A grid that held the keyboard when it was rebuilt
  holds it again afterward, so the next key lands in the grid rather than
  nowhere.
- **Rule-EDITOR-PANEL-250** — A grid row is drawn in its stripe, or in the
  selection color while it is selected, and in nothing else. The pointer passing
  over it changes nothing.
- **Rule-EDITOR-PANEL-251** — When the status bar is too narrow for everything,
  the figures on the right give way first, then the sentence on the left down to
  a short floor, and the page arrows keep their width to the last.
- **Rule-EDITOR-PANEL-252** — A badge's words are printed white or dark,
  whichever reads on its color, so a pale badge never carries white text.
- **Rule-EDITOR-PANEL-257** — An editor narrower than its toolbar or its status
  bar scrolls that bar sideways under the pointer, with the mouse wheel or a
  touchpad. No scrollbar is shown and the bar keeps its height, so nothing on it
  is cut off and the editor below does not move.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Passed                                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Passing this test case clears the actual result and the     │
│  stacktrace, because a test case that passed has nothing     │
│  to explain. There is no copy of it anywhere else.           │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Passed       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the run item status being recorded.
2. **The message** — names exactly what will be cleared, from the six.
3. **The confirmation word** — the run item status, again.

## Main flow

1. A test case is recorded as **Failed**, with an actual result and an error.
2. The tester realizes it really passed.
3. The tester selects it and presses `P`.
4. The confirmation opens, naming what will be cleared.
5. The tester presses `Enter`.
6. **Passed** is recorded, and everything the message named is cleared.
7. A message reads *Passed*.

## What Testin refuses

**If nothing would be cleared** — no confirmation is shown. The run item status
is simply written over.

**If the tester presses `Escape`** — nothing is changed at all.

**If several test cases are selected** — the confirmation is asked once for the
whole selection. Its message says *these*, then the count, then *test cases*.

## What cannot be undone

There is no undo for a run item status inside the test run editor. `Ctrl+Z`
there belongs to the test cases, not to the test run. A run item status written
over is gone. So is anything that clearing it removed.

**There is no way to clear a run item status back to nothing.** **Pending**, **Untested** and **Removed** have no key and are on
no menu.

## Where the plugin breaks its own rules

**The warning is only on this path.** An automated pass clears the same six
things with no dialog at all. That is difference 26 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

---

[Documentation](../README.md) › [The editor panel](main.md)
