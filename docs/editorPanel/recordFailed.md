[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-034

# UC-EDITOR-PANEL-034: Record that a test case failed, and say why

**As a** tester, **I want** to write down what actually happened at the moment I
see it, **so that** the bug report writes itself later.

**Failed** is the one verdict that asks a question first. Testin opens a small
form before it records anything.

`F`.

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
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing and changes nothing.
  Paging, filtering, searching and opening the details panel are all silent.
  None of them throws a filter away: a test case the filter is hiding is
  reported rather than brought into view.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-143** — Failing is the one verdict that asks for detail.
  The dialog opens before the verdict is recorded.
- **Rule-EDITOR-PANEL-144** — `Escape` in the dialog records nothing at all,
  neither the detail nor the verdict, and asks first when there is something
  typed to lose. A dialog opened and closed unchanged still goes at once.
- **Rule-EDITOR-PANEL-145** — Nothing is written as the tester types. Only
  saving writes.
- **Rule-EDITOR-PANEL-146** — The dialog opens for one test case. Several at
  once are failed with no detail collected.
- **Rule-EDITOR-PANEL-147** — The bug severity starts at **Enhancement** and the
  bug priority at **Low**.
- **Rule-EDITOR-PANEL-148** — The four fields are the same four the failure form
  in light mode uses.

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

1. **Description** and **Expected** — what the test case says. They show the
   tester what should have happened. Neither can be typed into.
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

**If the tester presses `Escape`** — nothing at all is recorded, and the test
case keeps whatever verdict it had. When something has been typed, Testin asks
first before throwing it away. A dialog opened and closed unchanged goes at
once.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the run keeps what it recorded.*

**If the test case is gone but the dialog is reached anyway** — the description
row reads *No longer in the test set*, and the expected row is not drawn.

**If several test cases are selected** — the dialog does not open at all. All of
them are failed with no detail. The detail can be filled in afterwards, one test
case at a time, with `F2`.

---

[Documentation](../README.md) › [The editor panel](main.md)
