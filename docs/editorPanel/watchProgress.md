[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-042

# UC-EDITOR-PANEL-042: Watch how the test run is going

**As a** tester, **I want** to see how many have passed and how long I have
been at it, **so that** I can say when the test run will be finished.

The status bar reports. Nothing here changes a test case.

There is no key for this. The figures are in the status bar.

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
- **Rule-EDITOR-PANEL-175** — A run item status no test case carries is not
  drawn at all.
- **Rule-EDITOR-PANEL-176** — The untouched test cases read **Pending** while
  the test run is open, and **Untested** once it is signed off.
- **Rule-EDITOR-PANEL-177** — The figures are worked out from what the test run
  holds now, not from disk.
- **Rule-EDITOR-PANEL-178** — A test run that has measured nothing shows a blank
  clock, not a row of zeros.
- **Rule-EDITOR-PANEL-179** — The three test run labels are hidden, not blank,
  when there is nothing to say. A test case editor never shows them.
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
┌────────────────────────────────────────────────────────────────────────────┐
│  4 of 12 test cases   |< < 1 of 1 > >|   In Progress                       │
│                       Passed 6 · Failed 2 · Pending 4     00:14:22  [ 50 ] │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The test run status** — with the same icon the tree draws. Its tooltip
   reads *This test run's status. A completed or closed test run records no more
   run item statuses*.
2. **The figures** — one for each run item status any test case carries, each
   in that run item status's own color, separated by a dot. Their tooltip reads
   *How this test run is going*.
3. **The clock** — how long this test run has been executing. It ticks once a
   second while a test case is being timed. Its tooltip reads *Time spent
   executing this test run*.
4. **The page size** — how many test cases a page holds.

## Main flow

1. The tester starts executing.
2. The status becomes **In Progress**, and the clock starts.
3. Each run item status recorded moves one figure up and another down.
4. A run item status nobody has recorded yet is not drawn.
5. When the walk finishes, the status becomes **Completed** and the clock stops.

## What Testin refuses

Nothing. The status bar only reports.

---

[Documentation](../README.md) › [The editor panel](main.md)
