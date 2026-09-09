[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-006

# UC-SETTING-006: Set the folder that files are saved to

**As a** tester, **I want** every report and every export to start in one
folder, **so that** I am not browsing to the same place several times a day.

Testin fills this folder in for the tester when a file is saved or chosen.

There is no key for this. It is the **Default download folder** row.

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
- **Rule-SETTING-021** — The folder is where saving a report, saving an export
  and choosing a file to import all start.
- **Rule-SETTING-022** — It is a starting point, not a rule. The tester can save
  anywhere from any of those dialogs.
- **Rule-SETTING-023** — This page owns the download folder. Nothing else
  writes it.

## The screen

The row is the fifth one on the page.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Default download folder:  [ C:\Users\muteb\Downloads    ] [...]         │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The box** — the folder that saving starts in.
2. **The browse button** — opens a folder chooser.

The folder chooser is titled **Select Default Download Folder**. The line under
that title reads *Choose the default folder for imports, exports, and reports*.

The whole page is drawn on [the settings page](main.md#the-page).

## Main flow

1. The tester presses the browse button on the **Default download folder** row.
2. The folder chooser opens.
3. The tester picks a folder and confirms.
4. The tester presses **Apply**.
5. The next report or export opens with that folder already filled in.

## What Testin refuses

Nothing. A folder that does not exist is stored exactly as typed.

## The only way it gets set

This row, and nothing else. The report, export and import dialogs each used to
carry a **Set as default folder** tick box that wrote this value, drawn only
while no folder was set - so once one was there they could not change it back,
and ticking it overwrote what this page said without saying so. The box is gone
and the use case behind it, *Remember the folder I use*, is retired on
[the sharing page](../share/main.md#retired).

Those dialogs still **open** on this folder. They never write it.

---

[Documentation](../README.md) › [The settings page](main.md)
