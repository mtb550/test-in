[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-004

# UC-VIEW-PANEL-004: Read what a test case says

**As a** tester, **I want** every field of the test case in one place,
**so that** I can follow its steps and judge it against its expected result.

This is what a tester reads while running a test.

There is no key for this. It is what the **Details** tab shows.

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
- **Rule-VIEW-PANEL-009** — Closing any Testin editor empties the panel.
- **Rule-VIEW-PANEL-023** — The rows are drawn in one fixed order, whatever the
  test case holds.
- **Rule-VIEW-PANEL-024** — A row with nothing in it is not drawn, and its
  caption goes with it.
- **Rule-VIEW-PANEL-025** — A test case with no description shows a dash for its
  title.
- **Rule-VIEW-PANEL-026** — The rows a tester writes as sentences are
  capitalized, and a period is added unless the text already closes itself:
  **Description**, **Expected Result**, **Steps** and **Pre Conditions**.
  **Reference**, **Module** and **Test Data** are left exactly as the tester
  typed them — a reference is an identifier and a module is a label, and a
  period after either reads as a typo. The same four rows are the ones the card
  and light mode capitalize, because all three ask the same question.
- **Rule-VIEW-PANEL-027** — A blank step is skipped, and its number is not given
  to the step after it. A test case with a blank third step reads one, two,
  four.
- **Rule-VIEW-PANEL-028** — Every value can be selected and copied, and none of
  them can be typed into.
- **Rule-VIEW-PANEL-061** — Who did something and when are one row, not two.
  **Created** reads the name, then *on*, then the date.
- **Rule-VIEW-PANEL-062** — **Order** is where the test case sits in its test
  set, read from the set rather than from the test case.

## The screen

```
┌──────────────────────────────────────────────────────────────────────────┐
│   Demo  >  Test Cases  >  Accounts  >  Login                             │
│                                                                          │
│   ( 3f2a05c1-...-9c1b )  [copy]                                          │
│                                                                          │
│   Log in with a valid user                                               │
│                                                                          │
│   [ go to code ]  [ run ]                                                │
│                                                                          │
│   ( P1 )  ( Smoke )  ( Failed )                                          │
│                                                                          │
│   Expected Result:    The dashboard opens.                               │
│   Steps:              1- Open the login page.                            │
│                       2- Type the credentials.                           │
│                       3- Press Sign in.                                  │
│   Pre Conditions:     An account exists.                                 │
│   Test Data:          user=admin                                         │
│   Module:             Accounts                                           │
│   Order:              3                                                  │
│   Created:            muteb on 2 September 2026                          │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The top part** — the path, the identity, the title, the two buttons and
   the badges. Each one is numbered on
   [the view panel page](main.md#the-panel).
2. **The captions** — the name of the field, ending in a colon.
3. **The values** — one column, all lined up. Each one can be selected and
   copied. None of them can be typed into.
4. **The rows that are missing** — this test case has nothing in its
   **Reference** field, so that row is not drawn and no gap is left for it.

## The rows, in order

| Caption | What it holds |
|---|---|
| **Expected Result** | What should happen |
| **Steps** | One numbered line for each step |
| **Pre Conditions** | What must be true before the test case starts |
| **Test Data** | The data the test case uses, exactly as typed |
| **Reference** | A link or a ticket number |
| **Module** | The part of the product this test case covers |
| **Order** | Where the test case sits in its test set |
| **Created** | Who made it, and when |
| **Updated** | Who last changed it, and when |

Above the rows sit the path, the identity, the title, the two buttons and the
badges. They are numbered on the panel drawing on
[the view panel page](main.md#the-panel).

## Main flow

1. The panel is filled with a test case.
2. Testin reads the test case again from memory, not from the copy it was
   handed.
3. The path is drawn, one step for each folder above the test case.
4. The identity is drawn in a gray pill, with a button that copies it.
5. The description is drawn as the title.
6. The badges are drawn: the priority, then one for each group, then the
   verdict.
7. Each row that has something in it is drawn, in the order above.

## What Testin refuses

**If a field is empty** — the row is not drawn at all. The panel is never a
column of captions with nothing beside them.

**If the description is empty** — the title reads a single dash.

**If a date was never set** — the row says the name alone rather than showing a
date in 1970. A row with neither a name nor a date is not drawn at all.

**If the priority is the lowest** — no priority badge is drawn. Only the two
higher priorities get a badge.

**If nobody has run the test case** — no verdict badge is drawn.

**If the test case has no steps, or every step is blank** — there is no
**Steps** row.

---

[Documentation](../README.md) › [The view panel](main.md)
