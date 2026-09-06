[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-009

# UC-VIEW-PANEL-009: Copy a test case's identity

**As a** tester, **I want** the test case's identity on my clipboard,
**so that** I can paste it into a bug report and anyone can find the exact test
case again.

There is no key for this. The button sits beside the identity.

## Rules

- **Rule 39** — The button turns into a green tick for one and a half seconds,
  then turns back.
- **Rule 40** — Copying raises no message. The tick is the whole confirmation.

Rules 1 to 9 hold everywhere in the panel. They are on
[the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

The identity sits in a gray pill under the path, with the button to its right.

```
┌────────────────────────────────────────────────────────────────────────────┐
│   ( 3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b )  [copy]                         │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The pill** — the test case's identity, in full.
2. **The button** — its tooltip reads **Copy ID**. The pointer becomes a hand
   over it.

## Main flow

1. The tester clicks the button beside the identity.
2. The whole identity goes on the clipboard.
3. The button becomes a green tick.
4. One and a half seconds later it becomes the copy button again.

## What Testin refuses

Nothing. There is no gray state and no way for it to fail.

## Why it works this way

The identity is what ties a test case to its generated test method, to a
verdict in a test run, and to its file on disk. Quoting it in a bug report means
the test case can be found again after its description has been rewritten.

---

[Documentation](../README.md) › [The view panel](main.md)
