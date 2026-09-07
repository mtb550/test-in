[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-036

# UC-EDITOR-PANEL-036: Resume a run I stopped

**As a** tester, **I want** to pick a test run up where I left it,
**so that** a morning's work is not repeated after lunch.

There is no key for this. Press **Start Manual Execution** again.

## Rules

- **Rule-EDITOR-PANEL-150** — Starting again finds the first test case with no
  verdict, so the walk resumes rather than restarting. A test case judged in the
  first sitting is passed over wherever it sits (Rule-EDITOR-PANEL-127).
- **Rule-EDITOR-PANEL-151** — The clock adds to the time a test case already
  carried, rather than starting it again.
- **Rule-EDITOR-PANEL-152** — The stamp saying when execution began is kept.
  Only the stamp saying when it ended is written again.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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
