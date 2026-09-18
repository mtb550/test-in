[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-006

# UC-VIEW-PANEL-006: Read the whole stacktrace of a failure

**As a** tester, **I want** every line of the error behind a failure,
**so that** I can paste it into a bug report without going to the log.

The panel shows only the first three lines. This opens the rest in a window.

There is no key for this. The link is under the first three lines.

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
- **Rule-VIEW-PANEL-034** — The panel shows the first three lines of the error,
  and offers a link to the rest.
- **Rule-VIEW-PANEL-035** — An error of three lines or fewer is shown whole,
  with no link.
- **Rule-VIEW-PANEL-036** — The text in the dialog can be selected and copied.
  It can also be typed into, and nothing typed there is ever saved.
- **Rule-VIEW-PANEL-081** — Under the first lines of the error, the Show all
  link comes first, then one thumbnail for each screenshot pasted with the
  failure, the one the failure form shows. Hovering names the file, and a click
  opens that screenshot at its real size in a window of its own.

## The screen

The **Stacktrace** row shows the first three lines, then *Show all* and the
number of lines the error really has, then a thumbnail of each screenshot
pasted with the failure: the picture itself, 48 pixels high, as the failure
form shows it. Hovering over one names its file; clicking it opens that
screenshot in a window of its own.

```
│   STACKTRACE                                                               │
│   java.lang.AssertionError: expected [true]                                │
│     at org.testin.demo.LoginTest.valid                                     │
│     at org.testng.internal.Invoker.invoke                                  │
│   Show all 42 lines                                                        │
│   ┌──────┐  ┌──────┐                                                       │
│   │ pic  │  │ pic  │                                                       │
│   └──────┘  └──────┘                                                       │
```

*Show all* opens this dialog. It holds the text, and no screenshot.

```
┌──────────────────────────────────────────────────────────────┐
│  Error                                                       │
├──────────────────────────────────────────────────────────────┤
│  Test Case      Log in with a valid user                     │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ The session was dropped.                               │  │
│  │                                                        │  │
│  │ java.lang.AssertionError: expected [true]              │  │
│  │   at org.testin.demo.LoginTest.valid(LoginTest:41)     │  │
│  │   at org.testng.internal.Invoker.invoke(Invoker:583)   │  │
│  └────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│  Escape  Close                                               │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always the one word.
2. **Test Case** — the description of the test case that failed.
3. **The text** — the message, a blank line, then the whole error.
4. **The bottom line** — `Escape` closes it.

## Main flow

1. The panel shows a failed test case with an error recorded against it.
2. The **Stacktrace** row shows the first three lines.
3. The tester clicks *Show all*, then the number of lines.
4. The **Error** dialog opens. It is wide enough to show a whole line of the
   error without wrapping it.
5. The tester selects the text and copies it.
6. The tester presses `Escape`. Nothing is saved.

To look at a screenshot, the tester clicks its thumbnail instead. The screenshot opens in a window titled with its file name, at its real
size, and scrolls when it is larger than the window. `Escape` closes it, and
nothing is saved.

```
┌──────────────────────────────────────────────────────────────┐
│  k3f9a.png                                                   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│         the screenshot, at its real size                     │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Escape  Close                                               │
└──────────────────────────────────────────────────────────────┘
```

## What Testin refuses

**If there is no error and no screenshot** — no **Stacktrace** row is drawn at
all.

**If there are screenshots but no error** — the row shows only the
thumbnails.

**If a screenshot's file cannot be read** — its thumbnail is an empty square,
and a click opens a window that is empty too.

**If the error is three lines or fewer** — the whole error is shown in the panel
and there is no link.

**If the failure has no message** — the dialog shows only the error, with no
blank first line.

**If the failure has no error** — the dialog shows only the message.

**If the tester types in the dialog** — the typing goes when the dialog closes,
and nothing is written. The text accepts typing only so that it can be selected
and copied.

---

[Documentation](../README.md) › [The view panel](main.md)
