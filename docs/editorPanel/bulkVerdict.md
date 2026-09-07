[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-039

# UC-EDITOR-PANEL-039: Record one verdict on many test cases

**As a** tester, **I want** to mark twelve test cases blocked at once,
**so that** an environment that is down does not cost twelve keystrokes and
twelve dialogs.

Select several test cases, then `P`, `F` or `B`.

## Rules

- **Rule-EDITOR-PANEL-161** — Every selected test case gets the verdict. Test
  cases that were removed from their test set are skipped and not counted.
- **Rule-EDITOR-PANEL-162** — Nothing is timed. Every one of them gets no
  duration.
- **Rule-EDITOR-PANEL-163** — One message with a count, however many were
  recorded.
- **Rule-EDITOR-PANEL-164** — Failing several does not open the failure dialog.
  They are failed with no detail.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-010 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

For a real defect this is usually the wrong gesture. Failing one at a time, with
`F`, is what records what happened.

---

[Documentation](../README.md) › [The editor panel](main.md)
