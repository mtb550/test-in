[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-038

# UC-EDITOR-PANEL-038: Correct a verdict I got wrong

**As a** tester, **I want** to change a verdict I recorded by mistake,
**so that** the test run says what really happened.

Press the right verdict's key on the test case.

## Rules

- **Rule-EDITOR-PANEL-156** — A verdict is simply written over. There is no
  separate gesture for correcting one.
- **Rule-EDITOR-PANEL-157** — Correcting a verdict re-stamps who recorded it and
  when. The original tester and the original time are gone.
- **Rule-EDITOR-PANEL-158** — Changing a failed test case to passed asks first,
  because it clears four things.
- **Rule-EDITOR-PANEL-159** — Only passing clears anything. Failing and blocking
  clear nothing.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Passed                                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Passing this case clears the actual result and the          │
│  stacktrace, because a case that passed has nothing to       │
│  explain. There is no copy of it anywhere else.              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Passed       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the verdict being recorded.
2. **The message** — names exactly what will be cleared, from the four.
3. **The confirm word** — the verdict, again.

## Main flow

1. A test case is recorded as **Failed**, with an actual result and an error.
2. The tester realizes it really passed.
3. The tester selects it and presses `P`.
4. The confirmation opens, naming what will be cleared.
5. The tester presses `Enter`.
6. **Passed** is recorded, and the four fields are cleared.
7. A message reads *Passed*.

## What Testin refuses

**If nothing would be cleared** — no confirmation is shown. The verdict is
simply written over.

**If the tester presses `Escape`** — nothing is changed at all.

**If several test cases are selected** — the confirmation is asked once for the
whole selection, and its message says *these*, then the count, then *cases*.

## What cannot be undone

There is no undo for a verdict inside the test run editor. `Ctrl+Z` there
belongs to the test cases, not to the test run. A verdict written over is gone,
and so is anything clearing it removed.

**There is no way to clear a verdict back to nothing.** **Pending**,
**Untested** and **Removed** have no key and are on no menu.

## Where the plugin breaks its own rules

**The warning is only on this path.** An automated pass clears the same four
fields with no dialog at all. That is difference 26 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

---

[Documentation](../README.md) › [The editor panel](main.md)
