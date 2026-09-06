[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-023

# UC-EDITOR-PANEL-023: Change how many a page holds

**As a** tester, **I want** more test cases on a page,
**so that** I can see a whole test set of 80 without turning pages.

There is no key for this. The box is at the right of the status bar.

## Rules

- **Rule 102** — A page holds 50 test cases until the tester says otherwise.
- **Rule 103** — The most a page can hold is 1000.
- **Rule 104** — Changing it goes back to the first page and returns the
  keyboard to the list.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester clicks the small box at the right of the status bar.
2. The tester types 200 and presses `Enter`.
3. The view is drawn again from the first page, 200 test cases at a time.
4. The keyboard goes back to the list.
5. Nothing is said.

## What Testin refuses

**Nothing is refused.** Every value is taken and quietly turned into something
Testin can use.

| The tester types | What the page holds |
|---|---|
| A number from 1 to 1000 | That many |
| Anything above 1000 | 1000 |
| `0`, or a negative number | 50 |
| Letters, or nothing at all | 50 |

The box is corrected in place, so the tester can see what happened. No message
is raised.

## Where the plugin breaks its own rules

Typing `5000` and getting 1000 is a refusal with no words. Typing `0` and
getting 50 is another. Every other refusal in Testin says what it did. That is
difference 11 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

---

[Documentation](../README.md) › [The editor panel](main.md)
