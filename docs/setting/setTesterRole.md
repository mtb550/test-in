[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-005

# UC-SETTING-005: Give my role

**As a** tester, **I want** to record what I do,
**so that** a report or a test case can say it was written by a test engineer.

There is no key for this. It is the **Tester role** row.

## Rules

- **Rule 20** — The role is stored on this machine and read by nothing.

Rules 1 to 6 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

## Main flow

1. The tester types a role into the **Tester role** row.
2. The tester presses **Apply**.
3. The value is stored.
4. Nothing else happens.

## What Testin refuses

Nothing.

## Where the plugin breaks its own rules

**The role is read by nothing at all.** It appears on no marker, in no report,
in no message and in no log line. The field takes a value and the value is never
used again. That is difference 2 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

A tester filling this row in reasonably expects it somewhere. It is nowhere.
Either something should read it, or the row should go. That is question 1 on
[the settings page](main.md#not-decided).

---

[Documentation](../README.md) › [The settings page](main.md)
