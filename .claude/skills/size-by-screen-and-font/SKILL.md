---
name: size-by-screen-and-font
description: In Testin's UI, a size is a part of the screen (or the IDE frame) or a count of characters in the IDE's current font, never a fixed pixel number. Use when writing or changing anything that sets a width, a height, a popup or dialog size, a text wrap width or an icon size - setPreferredSize, new Dimension, JBUI.scale with a big number, an HTML width, Toolkit.getScreenSize.
---

# Size by the screen and the font

> Our design must be based on screen dimensions by percentage, so it shows
> perfect on all screens, and if the IntelliJ font value is bigger.
>
> — Muteb, 5 October 2026

Testin runs on a laptop at 1366 pixels and on a 4K monitor, and the tester can
raise the IDE's font size. A width written as pixels is right on the screen it
was tried on and wrong everywhere else: too wide on a small screen, too narrow
for a large font, wrapping a sentence after three words.

## The rule

**A size is a part of something the tester has, never a number of pixels.**

| What is being sized                             | Size it by                                | How                                                                                                                                                |
|-------------------------------------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| A dialog                                        | A part of the IDE frame                   | The framework already does it: Rule-INTERNAL-100, `DialogSize` (six tenths of the frame wide, a named part tall). Use it, never a size of your own |
| A popup or panel that is not a framework dialog | A part of the screen it is on             | `ScreenUtil.getScreenRectangle(component)`, times a part such as `0.25`                                                                            |
| A block of text that wraps                      | A count of characters of the current font | `component.getFontMetrics(font).charWidth('m') * characters`, the way Swing sizes a text area's columns; or `JTextField(columns)`                  |
| Both of the above                               | The smaller of the two                    | Characters, capped at the part of the screen: a big font grows the box, a small screen stops it                                                    |
| A gap, a border, an icon                        | The IDE's scale                           | `JBUI.scale(8)`, `JBUI.Borders.empty(...)`: small numbers that follow the IDE's zoom                                                               |

## Never

- `Toolkit.getDefaultToolkit().getScreenSize()`: the primary screen, not the one
  the IDE is on. Ask the component's own screen with `ScreenUtil`.
- A fixed width in pixels for text - `JBUI.scale(300)`, `"width:420px"` in HTML.
  It ignores the font size.
- A large `JBUI.scale(...)` used as a layout size. `JBUI.scale` is for gaps and
  borders; a box size is a part of the screen or a count of characters.

## Example: the Testin Help popup

The hint text was first wrapped at a fixed 300 px. It now wraps at 48
characters of the label font, at most a quarter of the screen:

```java
// UC-INTERNAL-009
private int hintWidth() {
    final int characters = entry.getFontMetrics(UIUtil.getLabelFont()).charWidth('m') * HINT_CHARACTERS;
    return Math.min(characters, (int) (ScreenUtil.getScreenRectangle(entry).width * SCREEN_PART));
}
```

## Before you hand the change over

Raise the IDE font (Settings | Appearance | Use custom font, a bigger size) and
look again; then imagine the smallest laptop screen. If either breaks the
layout, a number is still hiding somewhere.
