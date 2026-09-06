[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-023

# UC-SHARE-023: Remember the folder I use

**As a** tester, **I want** exports and imports to start in the same folder,
**so that** I am not browsing to my downloads folder several times a day.

There is no key for this. It is a tick box on the export and import dialogs.

## Rules

- **Rule 99** — The tick box is drawn only while no folder has been set yet.
- **Rule 100** — One folder is remembered, and every dialog uses it.
- **Rule 101** — The export dialog remembers the folder in its box. The import
  dialog remembers the folder holding the file that was chosen.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester exports a test set for the first time.
2. The dialog carries a tick box reading **Set as default folder**.
3. The tester picks a folder, ticks the box and presses **Export**.
4. The folder is written into this machine's settings.
5. Every later export, import and report opens on that folder.
6. The tick box is not drawn again.

## What Testin refuses

Nothing. A folder that does not exist is stored exactly as chosen.

## Changing it afterwards

The tick box is gone once a folder is set, so neither dialog can change it. Only
the settings page can, and that is
[UC-SETTING-006](../setting/setDownloadFolder.md).

That is question 3 on
[the settings page](../setting/main.md#not-decided).

## The label is different in each dialog

On the export and report dialogs the tick box stands alone. On the import dialog
it sits under a heading reading **Options:**. The box says the same thing in
both.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
