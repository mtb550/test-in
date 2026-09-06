[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-034

# UC-EDITOR-PANEL-034: Record that a test case failed, and say why

**As a** tester, **I want** to write down what actually happened at the moment I
see it, **so that** the bug report writes itself later.

`F`.

## Rules

- **Rule 139** — Failing is the one verdict that asks for detail. The dialog
  opens before the verdict is recorded.
- **Rule 140** — `Escape` in the dialog records nothing at all. Neither the
  detail nor the verdict.
- **Rule 141** — Nothing is written as the tester types. Only saving writes.
- **Rule 142** — The dialog opens for one test case. Several at once are failed
  with no detail collected.
- **Rule 143** — The bug severity starts at **Enhancement** and the bug priority
  at **Low**.
- **Rule 144** — The four fields are the same four the failure form in light
  mode uses.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Failed Test Case Details                                    │
├──────────────────────────────────────────────────────────────┤
│  Description    Log in with a valid user                     │
│  Expected       The dashboard opens.                         │
│                                                              │
│  [ set actual result..                                    ]  │
│                                                              │
│  Bug Severity   ( ) Blocker  ( ) Major  ( ) Minor  (x) Enha. │
│                                                              │
│  Bug Priority   ( ) High     ( ) Medium            (x) Low   │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ paste error or exception or screenshot..               │  │
│  │                                                        │  │
│  └────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Save       Escape Cancel                         │
└──────────────────────────────────────────────────────────────┘
```

1. **Description** and **Expected** — what the test case says, so the tester can
   see what should have happened. Neither can be typed into.
2. **The first box** — what actually happened. It has no label, only its gray
   hint.
3. **Bug Severity** — four choices, with the least serious chosen.
4. **Bug Priority** — three choices, with the lowest chosen.
5. **The big box** — for the error, the exception or a note about a screenshot.
6. There is no button. `Enter` saves and `Escape` cancels.

## Main flow

1. The walk has selected a test case, and it does not work.
2. The tester presses `F`.
3. The **Failed Test Case Details** dialog opens, with the cursor in the first
   box.
4. The tester types what happened, picks a severity and a priority, and pastes
   the error.
5. The tester presses `Enter`.
6. The four fields are written onto the test run.
7. Only then is **Failed** recorded, with the tester's name, the time and the
   duration.
8. A message reads *Failed*.
9. The walk moves to the next test case.

## What Testin refuses

**If the tester presses `Escape`** — nothing at all is recorded. Everything
typed is thrown away, with no confirmation, and the test case keeps whatever
verdict it had. That is difference 29 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-executing-a-test-run).

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the run keeps what it recorded.*

**If the test case is gone but the dialog is reached anyway** — the description
row reads *No longer in the test set*, and the expected row is not drawn.

**If several test cases are selected** — the dialog does not open at all. All of
them are failed with no detail. That detail can be filled in afterwards, one at
a time, with `F2`.

---

[Documentation](../README.md) › [The editor panel](main.md)
