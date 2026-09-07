[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-007

# UC-SETTING-007: Choose how much Testin writes to its log

**As a** tester, **I want** to turn Testin's own log up,
**so that** I can send a useful log when something goes wrong, and turn it back
down afterwards.

There is no key for this. It is the **Log level** row.

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
- **Rule-SETTING-024** — The level takes effect the moment **Apply** is pressed.
  The IDE does not have to restart.
- **Rule-SETTING-025** — Testin starts at **INFO** until the tester chooses
  otherwise.
- **Rule-SETTING-026** — The log sits beside the IDE's own log, so collecting
  the IDE's logs collects Testin's too.

## The choices

The drop-down offers seven, in this order.

| Level | What is written |
|---|---|
| **DISABLED** | Nothing at all |
| **TRACE** | Everything, including every step of a read |
| **DEBUG** | Nearly everything |
| **INFO** | What happened, without the detail. The starting choice |
| **WARN** | Only what went oddly |
| **ERROR** | Only what failed |
| **FATAL** | Only what stopped Testin |

## Main flow

1. The tester opens the **Log level** drop-down.
2. The tester picks **TRACE**.
3. The tester presses **Apply**.
4. Testin starts writing every step at once.
5. The tester reproduces the problem.
6. The tester sets the level back to **INFO**.

## What Testin refuses

Nothing. The drop-down cannot hold anything but one of the seven.

## Why it matters

A good deal of what Testin does is only ever written to the log. A folder
skipped for having no marker, a test case that would not read, an edit that was
dropped because there was nowhere to write it. None of those raises a message,
and at **INFO** most of them are not written either. A tester chasing one of
them needs **TRACE**.

---

[Documentation](../README.md) › [The settings page](main.md)
