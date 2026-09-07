[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-039

# UC-EDITOR-PANEL-039: Record one verdict on many test cases

**As a** tester, **I want** to mark twelve test cases blocked at once,
**so that** an environment that is down does not cost twelve keystrokes and
twelve dialogs.

One key on a selection judges every test case in it.

Select several test cases, then `P`, `F` or `B`.

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
- **Rule-EDITOR-PANEL-163** — Every selected test case gets the verdict. Test
  cases that were removed from their test set are skipped and not counted.
- **Rule-EDITOR-PANEL-164** — Nothing is timed. Every one of them gets no
  duration.
- **Rule-EDITOR-PANEL-165** — One message with a count, however many were
  recorded.
- **Rule-EDITOR-PANEL-166** — Failing several does not open the failure dialog.
  They are failed with no detail.

## What the tester sees

This opens no screen. Every selected card takes the new verdict badge at once,
and the figures in the status bar move by the whole count.

One small message appears at the bottom of the IDE and fades. It reads the
verdict, then the count, such as *Blocked 12*.

## Main flow

1. The tester selects twelve test cases that all need the same environment.
2. The tester presses `B`.
3. All twelve are recorded as **Blocked**, with the tester's name and the time.
4. The test run is written to disk once.
5. One message reads *Blocked 12*.

## What Testin refuses

**If some of the selected test cases would lose something** — the confirmation
is asked once for the whole selection, not once each.

**If every selected test case was removed from its test set** — nothing is
recorded and no message is raised.

**If the walk was on one of the selected test cases** — the walk ends. It does
not move on.

## Failing several at once

The failure dialog does not open. All of them are failed with nothing written
about why. The detail can be filled in afterwards, one at a time, with `F2`,
which is [UC-EDITOR-PANEL-040](editFailureDetail.md).

For a real defect this is usually the wrong gesture. Failing one test case at a
time, with `F`, is what records what happened.

---

[Documentation](../README.md) › [The editor panel](main.md)
