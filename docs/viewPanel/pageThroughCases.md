[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-003

# UC-VIEW-PANEL-003: Page through several test cases

**As a** tester, **I want** to walk the test cases I selected without going back
to the list, **so that** I can read four of them one after another with two
keys.

The panel was handed a list of test cases. These two keys walk that list.

`Ctrl+Right` moves forward. `Ctrl+Left` moves back.

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
- **Rule-VIEW-PANEL-019** — The two keys work from any of the three tabs.
- **Rule-VIEW-PANEL-020** — An arrow with nowhere to go is gray.
- **Rule-VIEW-PANEL-021** — Paging says nothing.
- **Rule-VIEW-PANEL-022** — Only two gestures hand over more than one test case.
  They are **View Details** on several selected cards, and following the
  selection.

## The screen

The two arrows sit in the panel's own title bar, above the tabs.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin                                                     ( < )  ( > )   │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The back arrow** — the previous test case. Gray on the first one.
2. **The forward arrow** — the next test case. Gray on the last one.

Neither arrow says which test case of how many is showing.

## Main flow

1. The tester selects four test cases and presses `Enter`.
2. The panel opens on the first of them.
3. The tester presses `Ctrl+Right`.
4. The panel draws the second.
5. The tester presses `Ctrl+Left` twice.
6. The panel draws the first again, and the back arrow goes gray.

## What Testin refuses

**If there is no next test case** — the forward arrow is gray and the key does
nothing.

**If there is no previous test case** — the back arrow is gray.

**If the panel was handed one test case** — both arrows are gray from the start.
That is every gesture except the two named in Rule-VIEW-PANEL-022.

**If the panel is showing nothing** — both arrows are gray.

## Where the plugin breaks its own rules

`Ctrl+Right` means two different things in two places. In the panel it moves to
the next test case. In an editor it turns the page. Both are on one screen, and
only the place the keyboard is decides which one happens.

---

[Documentation](../README.md) › [The view panel](main.md)
