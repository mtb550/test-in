[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-011

# UC-SETTING-011: Change the size of Testin's text

**As a** tester, **I want** Testin's text at the size I read code at,
**so that** I can read a test case on a projector or on a laptop without
squinting.

Hold `Ctrl` and turn the mouse wheel. On a Mac, hold `Cmd`.

## Rules

- **Rule-SETTING-037** — The gesture changes the IDE's own editor font size, so
  every Testin surface and every code editor change together.
- **Rule-SETTING-038** — Nothing is drawn smaller than eight points, and nothing
  larger than 72.
- **Rule-SETTING-039** — The gesture works over the tree panel, the editor
  panel, the view panel and light mode.

Rule-SETTING-001 to Rule-SETTING-006 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

## The screen

A small bubble appears while the size is changing.

```
┌──────────────────────────────────────────────────────────────┐
│  Font size: 14pt                                       [gear]│
└──────────────────────────────────────────────────────────────┘
```

1. **The size** — the size in points, as it changes.
2. **The gear** — closes the bubble and opens the IDE's own font settings.

The bubble hides itself after five seconds.

## Main flow

1. The tester holds `Ctrl` and turns the wheel over a Testin panel.
2. The size moves one point for each notch.
3. The bubble shows the new size.
4. Every Testin surface and every open code editor redraws at the new size.
5. Five seconds later the bubble goes.

## What Testin refuses

**If the size is already eight points** — turning down does nothing. The bubble
still appears, so it does not look broken.

**If the size is already 72 points** — turning up does nothing, the same way.

## What is not resized

Some text keeps a fixed distance from the base size rather than a fixed size. A
card's title is three points larger. A badge is two points smaller. The path at
the top of the view panel is one point smaller. None of them ever goes below
eight points.

---

[Documentation](../README.md) › [The settings page](main.md)
