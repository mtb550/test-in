[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-036

# UC-EDITOR-PANEL-036: Resume a run I stopped

**As a** tester, **I want** to pick a test run up where I left it,
**so that** a morning's work is not repeated after lunch.

There is no separate resume button. Starting again is resuming, because the walk
always begins at the first test case with no verdict.

There is no key for this. Press **Start Manual Execution** again.

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
- **Rule-EDITOR-PANEL-153** — Starting again finds the first test case with no
  verdict, so the walk resumes rather than restarting. A test case judged in the
  first sitting is passed over wherever it sits (Rule-EDITOR-PANEL-130).
- **Rule-EDITOR-PANEL-154** — The clock adds to the time a test case already
  carried, rather than starting it again.
- **Rule-EDITOR-PANEL-155** — The stamp saying when execution began is kept.
  Only the stamp saying when it ended is written again.

## What the tester sees

This opens no screen. It looks exactly like starting. The editor turns to the
first test case with no verdict, selects its row, and the clock starts again.

Every test case judged in the first sitting keeps its verdict, and the walk
passes over it.

## Main flow

1. The tester stopped a walk after judging 20 of 80 test cases.
2. The tester comes back, opens the test run, and presses **Start Manual
   Execution**.
3. Testin finds test case 21, the first with no verdict.
4. The editor turns to the page holding it and selects it.
5. The clock starts again.

## What Testin refuses

**If the test run is completed or closed** — the button is gray, and its tooltip
says which status is stopping it. A test run signed off cannot be resumed.

**If every test case already has a verdict** — the walk reaches the end at once,
and the test run is marked completed.

## What is kept from before

| Kept | Written again |
|---|---|
| Every verdict already recorded | When execution ended |
| How long each test case took, added to | The verdicts recorded from now on |
| When execution began | |

---

[Documentation](../README.md) › [The editor panel](main.md)
