[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-009

# UC-VIEW-PANEL-009: Copy a test case's identity, or go to it

**As a** tester, **I want** the test case's identity on my clipboard, **so that** I can paste it into a bug report and
anyone can find the exact test
case again.

The identity is a long code. It never changes, even when the title does. It
sits at the end of the path, because the path and the identity answer the same
question: which test case is this.

Getting to the test case itself is the third icon on the line below. That is
how a tester who opened this panel from a gutter mark reaches the test case,
when they want to.

There is no key for either. The pill and the button sit side by side.

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
- **Rule-VIEW-PANEL-039** — The button turns into a green tick for one and a
  half seconds, then turns back.
- **Rule-VIEW-PANEL-040** — Copying raises no message. The tick is the whole
  confirmation.
- **Rule-VIEW-PANEL-063** — Going to the test case is an icon of its own,
  the third on the identity line, and the only thing that does it. It opens the
  test case's own test set editor and selects it there. The tree does not move:
  the tester is already looking at the test case and asked for the editor, so
  revealing the node is an answer to a question nobody asked. The path is what
  moves the tree, and it still does. A test case with no test set to open — one
  shown from a test run after it was removed from its set — leaves the icon
  gray and says so.

## The screen

The identity sits in a gray pill at the end of the path, with the button to its
right.

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Demo > Test Cases > Login   ( 3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b )     │
│                                                            [copy]          │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The pill** — the test case's identity, in full.
2. **The button** — its tooltip reads **Copy ID**. The pointer becomes a hand
   over it.

## Main flow

1. The tester clicks the button beside the identity.
2. The whole identity goes on the clipboard.
3. The button becomes a green tick.
4. One and a half seconds later it becomes the copy button again.

## Going to the test case instead

1. The tester clicks the third icon on the identity line, the one that reads
   **Navigate to Test Case**.
2. The test set holding it opens in an editor, with the test case selected.
3. Nothing else moves. The tree stays where it was.

The path just above does move the tree, and that is the difference between the
two: a path step names a place, and the icon names the test case.

## What Testin refuses

Copying refuses nothing. There is no gray state and no way for it to fail.

**If the test case has no test set to open** — the icon is gray, it does not
grow under the pointer, and clicking it reads *There is no test set to open
this test case in.* This is the only way to meet it: a test run keeps the
verdict of a test case its test set no longer holds, and the panel shows that
test case from the run.

## Why it works this way

The identity ties three things to a test case: its generated test method, its
verdict in a test run, and its file on disk. A bug report that quotes the
identity still points at the right test case after the description has been
rewritten.

---

[Documentation](../README.md) › [The view panel](main.md)
