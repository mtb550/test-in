[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-010

# UC-VIEW-PANEL-010: Go to the test set the test case lives in

**As a** tester, **I want** to open the test set holding the test case I am
reading, **so that** I can see the test cases around it.

There is no key for this. The path is at the top of the panel.

## Rules

- **Rule-VIEW-PANEL-041** — The path shows one step for each folder above the
  test case.
- **Rule-VIEW-PANEL-042** — Only the last step opens anything. It opens the test
  set.
- **Rule-VIEW-PANEL-043** — Opening a test set that is already open brings it to
  the front.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│   Demo  >  Test Cases  >  Accounts  >  Login                               │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Every step** — gray, and underlined under the pointer.
2. **The last step** — the only one that opens anything.

## Main flow

1. The tester moves the pointer over the last step of the path.
2. The step turns to the link color and underlines itself.
3. The tester clicks it.
4. The test set's editor opens, or comes to the front if it was already open.

## What Testin refuses

**If the tester clicks any step but the last** — nothing happens, and nothing is
said. The step still shows a hand pointer and still underlines itself. That is
difference 4 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

**If the panel was handed no path** — no steps are drawn at all.

## Where the plugin breaks its own rules

**Clicking the last step fails when the panel was opened from a test run.** The
path then names a test run, and Testin looks for a test set there. It finds
none, and stops with an internal error the tester cannot read. That is
difference 3 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The view panel](main.md)
