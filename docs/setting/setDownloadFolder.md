[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-006

# UC-SETTING-006: Set the folder that files are saved to

**As a** tester, **I want** every report and every export to start in one
folder, **so that** I am not browsing to the same place several times a day.

There is no key for this. It is the **Default download folder** row.

## Rules

- **Rule 21** — The folder is where saving a report, saving an export and
  choosing a file to import all start.
- **Rule 22** — It is a starting point, not a rule. The tester can save anywhere
  from any of those dialogs.
- **Rule 23** — This page is not the only place the folder is set. The import
  and export dialogs can set it too.

Rules 1 to 6 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

## The screen

The browse button opens a folder chooser titled **Select Default Download
Folder**, whose line underneath reads *Choose the default folder for imports,
exports, and reports*.

## Main flow

1. The tester presses the browse button on the **Default download folder** row.
2. The folder chooser opens.
3. The tester picks a folder and confirms.
4. The tester presses **Apply**.
5. The next report or export opens with that folder already filled in.

## What Testin refuses

Nothing. A folder that does not exist is stored exactly as typed.

## The other way it gets set

The report dialog, the export dialog and the import dialog each carry a
checkbox reading **Set as default folder**. It is drawn only while no folder has
been set yet. Ticking it and confirming writes the folder here.

The two dialogs store slightly different things. The report and export dialogs
store the folder in the box. The import dialog stores the folder holding the
file that was chosen.

Once a folder is set the checkbox is not drawn again, so those dialogs cannot
change it. Only this page can. That is question 3 on
[the settings page](main.md#not-decided).

## Where the plugin breaks its own rules

This page is meant to own these values, and this one has three other writers.
That is difference 4 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The settings page](main.md)
