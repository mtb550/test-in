[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-005

# UC-SETTING-005: Give my role

**As a** tester, **I want** to record what I do,
**so that** a report or a test case can say it was written by a test engineer.

There is no key for this. It is the **Tester role** row.

## Rules

- **Rule-SETTING-001** — One page for the whole IDE. Every code project open in
  it reads the same values.
- **Rule-SETTING-002** — Nothing on this page is checked. A folder that does not
  exist is stored exactly as typed.
- **Rule-SETTING-003** — Nothing on this page raises a message when it is saved.
- **Rule-SETTING-004** — Only a changed Testin folder makes Testin read the disk
  again. Every other setting is read where it is used, when it is used.
- **Rule-SETTING-005** — A password is never on this page. It is asked for when
  it is needed and kept in the IDE's password store.
- **Rule-SETTING-006** — Nothing on this page has a key of its own.
- **Rule-SETTING-020** — The role is stored on this machine and read by nothing.

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
