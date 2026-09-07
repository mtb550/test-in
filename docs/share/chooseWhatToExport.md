[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-003

# UC-SHARE-003: Choose what goes into the export, and correct it

**As a** tester, **I want** to drop a few test cases and fix a typo before the
file leaves, **so that** what I send is what I meant to send.

The export dialog shows every test case in a table first. The tester unticks
rows and edits cells there, and only then writes the file.

There is no key for this. The table is in the export dialog.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. The Git ones can be
  canceled. The export, import and report ones cannot.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-017** — Every test case arrives ticked.
- **Rule-SHARE-018** — The box in the first column's heading ticks or unticks
  the whole tab.
- **Rule-SHARE-019** — Every column but the number can be typed into.
- **Rule-SHARE-020** — A correction made here changes the file, and never the
  test case itself.
- **Rule-SHARE-021** — Moving away from a cell saves what was typed in it.

## The screen

The table fills the middle of the export dialog.
[UC-SHARE-001](exportTestSet.md) draws the whole dialog around it.

```
┌──────────────────────────────────────────────────────────────────────┐
│  | Login | Checkout |                                                │
│                                                                      │
│  [x]  | #   | Description          | Priority  | Group               │
│  [x]  | 1   | Log in with a valid  | P1     v  | Smoke, Regression   │
│  [ ]  | 2   | Log in with a lock.  | P2     v  | Regression          │
│  [x]  | 3   | Log out              | P3     v  | <No Group>          │
└──────────────────────────────────────────────────────────────────────┘
```

1. **The box in the heading** — ticks or unticks every row on this tab.
2. **The box on each row** — every test case arrives ticked. Only ticked rows
   are written.
3. **The number column** — the one column that cannot be typed into.
4. **Priority** — a short list, drawn below.
5. **Group** — clicking the cell opens a picker, drawn below.

## The two special columns

**Priority** is a list offering **P1**, **P2** and **P3**.

**Group** opens a picker. Clicking the cell opens a window. It lists every
group, and the test case's own groups are already selected.

```
┌──────────────────────────────────────────────────────────────┐
│  Select Groups                                               │
├──────────────────────────────────────────────────────────────┤
│  Group                                                       │
│  <No Group>                                                  │
│  Regression                                                  │
│  Smoke                                                       │
│  Sanity                                                      │
│  Security                                                    │
│  UI                                                          │
│  Functional                                                  │
│  Validation                                                  │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Confirm   Ctrl+Click Add   Escape Cancel         │
└──────────────────────────────────────────────────────────────┘
```

`Ctrl+Click` adds a group. `Enter` confirms and `Escape` cancels. The chosen
groups are written back into the cell, joined by commas.

## Main flow

1. The export dialog opens with the table filled.
2. The tester unticks three test cases that are not ready.
3. The tester clicks an expected result and corrects it.
4. The tester clicks a **Group** cell and adds **Smoke**.
5. The tester presses **Export**.
6. Only the ticked rows are written, with the corrections in them.

## What Testin refuses

**If nothing is ticked on any tab** — a message titled **Export Empty** reads
*Select at least one test case to export.*

**A value Testin cannot read is not refused, it is replaced.**

| The tester types | What is written |
|---|---|
| A priority Testin does not know | The lowest |
| A status Testin does not know | Whatever the row had already |
| A group Testin does not know | Dropped from the list |
| A date Testin cannot read | An empty cell |

None of the four says anything. That is difference 9 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

**If the priority or the group column cannot be found** — neither picker is
drawn anywhere in the table. Only the log says so.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
