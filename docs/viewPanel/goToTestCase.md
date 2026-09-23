[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-009

# UC-VIEW-PANEL-009: Go to the test case

**As a** tester reading a test case in the panel, **I want** to open the test
case itself, **so that** I can change it where it is edited rather than where it
is read.

The panel is read-only. This is how a tester who reached it from a gutter mark,
a test run or a search gets to the editor that can change the test case.

There is no key for it. It is the third icon on the identity line, beside **go
to code** and **run**.

**The panel does not show the test case's identity.** It showed it in a pill
with a copy button until 24 September 2026; the identity is a grid column now,
off to start with and switched on from **Fields**
([UC-EDITOR-PANEL-003](../editorPanel/chooseFields.md)). A 36-character code at
the top of every panel was the loudest thing on it, and the thing it was for -
naming the exact test case in a bug report - is written into the report by
**Report a bug** without anyone copying anything.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
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
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-063** — Going to the test case is an icon of its own,
  the third on the identity line, and the only thing that does it. It opens the
  test case's own test set editor and selects it there. The tree does not move:
  the tester is already looking at the test case and asked for the editor, so
  revealing the node is an answer to a question nobody asked. The path is what
  moves the tree, and it still does. A test case with no test set to open — one
  shown from a test run after it was removed from its set — leaves the icon
  gray and says so.

## The screen

The icon is the last of the three, on the line under the title.

```
┌────────────────────────────────────────────────────────────────────────────┐
│   ( P1 )  ( Smoke )          [ go to code ]  [ run ]  [ tc ]               │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The icon** — the same **tc** mark the cards draw on hover, so the gesture
   looks the same wherever a tester meets it. Its tooltip reads **Navigate to
   Test Case**.
2. **The other two** — [UC-VIEW-PANEL-014](goToCode.md) and
   [UC-VIEW-PANEL-012](runFromPanel.md). All three are always drawn.

## Main flow

1. The tester clicks the third icon.
2. The test set holding the test case opens in an editor, with the test case
   selected.
3. Nothing else moves. The tree stays where it was.

The path above it does move the tree, and that is the difference between the
two: a path step names a place, and the icon names the test case.

## What Testin refuses

**If the test case has no test set to open** — the icon is gray, it does not
grow under the pointer, and clicking it reads *There is no test set to open
this test case in.* This is the only way to meet it: a test run keeps the
verdict of a test case its test set no longer holds, and the panel shows that
test case from the run.

## Why it works this way

The panel follows the tester and shows whatever they are looking at, from
wherever they are looking at it - a gutter mark, a test run, a search. None of
those is the place a test case is written. One icon closes that gap, and it
refuses rather than disappearing when there is nowhere to go
([Rule-VIEW-PANEL-056](goToCode.md) holds the other two to the same bargain).

---

[Documentation](../README.md) › [The view panel](main.md)
