[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-003

# UC-VIEW-PANEL-003: Page through several test cases

**As a** tester, **I want** to walk the test cases I selected without going back
to the list, **so that** I can read four of them one after another with two
keys.

`Ctrl+Right` moves forward. `Ctrl+Left` moves back.

## Rules

- **Rule-VIEW-PANEL-019** — The two keys work from any of the three tabs.
- **Rule-VIEW-PANEL-020** — An arrow with nowhere to go is gray.
- **Rule-VIEW-PANEL-021** — Paging says nothing.
- **Rule-VIEW-PANEL-022** — Only two gestures hand over more than one test case.
  They are **View Details** on several selected cards, and following the
  selection.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

`Ctrl+Right` means two different things in two places. Here it moves to the next
test case. In an editor it turns the page. The two are one keystroke apart on
one screen.

---

[Documentation](../README.md) › [The view panel](main.md)
