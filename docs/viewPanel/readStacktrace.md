[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-006

# UC-VIEW-PANEL-006: Read the stacktrace behind a failure

**As a** tester, **I want** every line of the error behind a failure, **so that** I can paste it into a bug report
without going to the log.

The panel never shows the error. It is the application's own stacktrace, copied
out of a log, and it is far longer than anything else the panel holds - so the
panel offers one link, **Stacktrace**, and the whole thing opens in a window.

There is no key for this. The link sits where the value would have been.

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
- **Rule-VIEW-PANEL-034** — The panel never shows the error itself, however
  short it is. It offers one link, **Stacktrace**, and there is no caption above
  it: the link is its own name.
- **Rule-VIEW-PANEL-035** — The window is the only place the error is read, so it
  holds the test case and what the tester wrote about it above the error. Those
  two are read only; the error sits in a box of its own.
- **Rule-VIEW-PANEL-036** — The error can be selected and copied, and nothing in
  the window can be typed into. It is a window for reading a value the framework
  wrote, and the failure dialog is where a tester changes one. The test case and
  the actual result above the error are drawn as the framework draws every
  details row, which is to be read rather than selected - the error is the part a
  tester copies into a bug report, and that is the part that selects.
- **Rule-VIEW-PANEL-081** — The **Stacktrace** link comes first on its line, then
  one thumbnail for each screenshot pasted with the failure, the one the failure
  form shows. Hovering names the file, and a click opens that screenshot at its
  real size in a window of its own.

## The screen

One line holds **Stacktrace**, then a thumbnail of each screenshot pasted with
the failure: the picture itself, 48 pixels high, as the failure form shows it.
Hovering over one names its file; clicking it opens that screenshot in a window
of its own.

```
│   Stacktrace   ┌──────┐  ┌──────┐                                          │
│               │ pic  │  │ pic  │                                           │
│               └──────┘  └──────┘                                           │
```

**Stacktrace** opens this dialog. It holds the text, and no screenshot.

```
┌──────────────────────────────────────────────────────────────┐
│  Stacktrace                                                  │
├──────────────────────────────────────────────────────────────┤
│  TEST CASE                                                   │
│  Log in with a valid user                                    │
│  ACTUAL RESULT                                               │
│  The session was dropped.                                    │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ java.lang.AssertionError: expected [true]              │  │
│  │   at org.testin.demo.LoginTest.valid(LoginTest:41)     │  │
│  │   at org.testng.internal.Invoker.invoke(Invoker:583)   │  │
│  └────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│  Escape  Close                                               │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always the one word, the same word the link reads.
2. **Test Case** and **Actual Result** — the description of the test case that
   failed, and what the tester wrote about it. Both are there to be read: neither
   can be typed into, and neither is selected with the pointer.
3. **The box** — the error, and nothing else. It scrolls, and it cannot be typed
   into.
4. **The bottom line** — `Escape` closes it.

## Main flow

1. The panel shows a failed test case with an error recorded against it.
2. One line reads **Stacktrace**.
3. The tester clicks it.
4. The **Stacktrace** dialog opens. It is wide enough to show a whole line of the
   error without wrapping it.
5. The tester selects the text and copies it.
6. The tester presses `Escape`. Nothing is saved.

To look at a screenshot, the tester clicks its thumbnail instead. The screenshot opens in a window titled with its file
name, at its real
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

**If there is no error and no screenshot** — the line is not drawn at all.

**If there are screenshots but no error** — the line shows only the
thumbnails, with no **Stacktrace** link.

**If a screenshot's file cannot be read** — its thumbnail is an empty square,
and a click opens a window that is empty too.

**If the failure has no message** — the dialog shows only the error, with no
blank first line.

**If the failure has no error** — the dialog shows only the message.

**If the tester tries to type in the dialog** — nothing happens. The window is
for reading a value the framework wrote; the failure dialog is where a tester
changes one (Rule-VIEW-PANEL-036).

---

[Documentation](../README.md) › [The view panel](main.md)
