[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-015

# UC-VIEW-PANEL-015: Close the panel

**As a** tester, **I want** the panel out of the way,
**so that** I have the whole width of the screen for the editor.

The panel takes width from the editor. Closing it gives that width back.

`Escape`, pressed in the editor, closes it.

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
- **Rule-VIEW-PANEL-058** — `Escape` in the editor does three things in order.
  It drops a pending cut, then closes the panel, then clears the selection. One
  press does one of them.
- **Rule-VIEW-PANEL-059** — A panel the tester closed stays closed. Moving the
  selection does not open it again.
- **Rule-VIEW-PANEL-060** — Closing the editor a test case came from closes the
  panel too.

## What the tester sees

This use case opens no screen of its own. It takes one away.

The panel goes from the right of the IDE, and the editor takes back the width.
No message appears anywhere. The test case is not changed, and nothing is
written.

## Main flow

1. The panel is open, and the keyboard is in the editor.
2. The tester presses `Escape`.
3. The panel closes. Nothing is said.
4. The tester presses `Escape` again.
5. The selection is cleared.

The tester can also press the IDE's own hide button on the panel.

## What Testin refuses

**If the panel is already closed** — the press clears the selection instead.

**If a cut is waiting to be pasted** — the first press drops the cut. The panel
stays open. A second press closes it.

## Where the plugin breaks its own rules

**`Escape` does nothing from inside the panel.** It closes the panel only when
the keyboard is in the editor. `F2` needs the keyboard in the panel, so a tester
who has just used `F2` cannot close the panel with `Escape`. That is difference
2 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

**Closing any Testin editor empties the panel.** A tester has two editors open
and is reading a test case from the first. They close the second, and the panel
goes blank. That is difference 9.

---

[Documentation](../README.md) › [The view panel](main.md)
