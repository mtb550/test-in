[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-001

# UC-SETTING-001: Open the settings page

**As a** tester, **I want** to reach Testin's settings quickly,
**so that** I can tell it where my test data is without hunting through the
IDE's own settings tree.

There is no key for this. The fastest way in is the gear button on the tree
panel's toolbar.

## Rules

- **Rule 7** — The gear button on the tree panel opens this page directly.
- **Rule 8** — **Apply** and **OK** stay gray until something on the page
  differs from what is stored.
- **Rule 9** — Pressing **Apply** writes every field at once, not only the one
  that changed.

Rules 1 to 6 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

## The four ways in

| The tester does this | Where it is |
|---|---|
| Presses the gear button | The tree panel's toolbar. Its tooltip reads **Configure Testin settings** |
| Opens **Settings**, then **Tools**, then **Testin** | The IDE's own settings window |
| Clicks **Open Settings** on the setup message | The message shown when no Testin folder is set |
| Clicks **Configure Testin settings** | The tree panel's empty state, when no Testin folder is set |

## Main flow

1. The tester presses the gear button on the tree panel's toolbar.
2. The IDE's settings window opens on Testin's page.
3. Every field is filled with what is stored now.
4. The tester changes a field. **Apply** and **OK** become live.
5. The tester presses **Apply**.
6. Every field is written to this machine's settings.
7. If the Testin folder changed, every code project that has opened the Testin
   panel reads the disk again.

## What Testin refuses

Nothing. No value on this page is checked, and pressing **Apply** never fails.

## Where the plugin breaks its own rules

A code project whose Testin panel was never opened does not read the disk again
when the Testin folder changes. It keeps the old folder until its panel is
opened. That is difference 5 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The settings page](main.md)
