[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-006

# UC-VIEW-PANEL-006: Read the whole stacktrace of a failure

**As a** tester, **I want** every line of the error behind a failure,
**so that** I can paste it into a bug report without going to the log.

There is no key for this. The link is under the first three lines.

## Rules

- **Rule 34** — The panel shows the first three lines of the error, and offers a
  link to the rest.
- **Rule 35** — An error of three lines or fewer is shown whole, with no link.
- **Rule 36** — The text in the dialog can be selected and copied. It can also
  be typed into, and nothing typed there is ever saved.

Rules 1 to 9 hold everywhere in the panel. They are on
[the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

The link reads *Show all* and the number of lines the error really has.

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
4. The **Error** dialog opens, wide enough to read a frame without wrapping.
5. The tester selects the text and copies it.
6. The tester presses `Escape`. Nothing is saved.

## What Testin refuses

**If there is no error** — no **Stacktrace** row is drawn at all.

**If the error is three lines or fewer** — the whole error is shown in the panel
and there is no link.

**If the failure has no message** — the dialog shows only the error, with no
blank first line.

**If the failure has no error** — the dialog shows only the message.

**If the tester types in the dialog** — the change closes with the dialog and
nothing is written. The text can be typed into so it can be selected and
copied.

---

[Documentation](../README.md) › [The view panel](main.md)
