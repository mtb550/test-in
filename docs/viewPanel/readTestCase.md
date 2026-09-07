[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-004

# UC-VIEW-PANEL-004: Read what a test case says

**As a** tester, **I want** every field of the test case in one place,
**so that** I can follow its steps and judge it against its expected result.

There is no key for this. It is what the **Details** tab shows.

## Rules

- **Rule-VIEW-PANEL-023** — The rows are drawn in one fixed order, whatever the
  test case holds.
- **Rule-VIEW-PANEL-024** — A row with nothing in it is not drawn, and its
  caption goes with it.
- **Rule-VIEW-PANEL-025** — A test case with no description shows a dash for its
  title.
- **Rule-VIEW-PANEL-026** — Text is capitalized and given a full stop, unless it
  already ends in one. The test data row is left exactly as the tester typed it.
- **Rule-VIEW-PANEL-027** — A blank step is skipped, and its number is not given
  to the step after it. A test case with a blank second step reads one, two,
  four.
- **Rule-VIEW-PANEL-028** — Every value can be selected and copied, and none of
  them can be typed into.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The rows, in order

| Caption | What it holds |
|---|---|
| **Expected Result:** | What should happen |
| **Steps:** | One numbered line for each step |
| **Pre Conditions:** | What must be true before the test case starts |
| **Test Data:** | The data the test case uses, exactly as typed |
| **Reference:** | A link or a ticket number |
| **Module:** | The part of the product this test case covers |
| **Created By:** | Who made it |
| **Updated By:** | Who last changed it |
| **Created At:** | When it was made |
| **Updated At:** | When it was last changed |

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
column of empty captions.

**If the description is empty** — the title reads a single dash.

**If a date was never set** — that row disappears, rather than showing a date
in 1970.

**If the priority is the lowest** — no priority badge is drawn. Only the two
higher priorities are worth a badge.

**If nobody has run the test case** — no verdict badge is drawn.

**If the test case has no steps, or every step is blank** — there is no
**Steps:** row.

## Where the plugin breaks its own rules

The captions here end in a colon. The captions on the run rows above them do
not. That is difference 8 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The view panel](main.md)
