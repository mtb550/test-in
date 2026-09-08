[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-023

# UC-SHARE-023: Remember the folder I use

**As a** tester, **I want** exports and imports to start in the same folder,
**so that** I am not browsing to my downloads folder several times a day.

One folder is remembered for this machine. Every export, import and report
opens on it afterwards.

There is no key for this. It is a tick box on the export and import dialogs.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. A Git step that
  only reads can be canceled; one that is writing to the repository or to the
  remote cannot, because a push or a rebase stopped half way leaves the
  repository in a state nobody asked for. The export, import and report ones
  cannot be canceled either.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-102** — The tick box is drawn only while no folder has been set
  yet.
- **Rule-SHARE-103** — One folder is remembered, and every dialog uses it.
- **Rule-SHARE-104** — The export dialog remembers the folder in its box. The
  import dialog remembers the folder holding the file that was chosen.

## The screen

The tick box sits under the three boxes of the export dialog.
[UC-SHARE-001](exportTestSet.md) draws the whole dialog.

```
┌──────────────────────────────────────────────────────────────┐
│  Destination:  [ C:\Users\mtb\Downloads           ] [ ... ]  │
│  File name:    [ Login                            ]          │
│  Format:       [ XLSX                            v]          │
│                                                              │
│  [ ] Set as default folder                                   │
└──────────────────────────────────────────────────────────────┘
```

1. **The tick box** — it reads **Set as default folder**.
2. **When it is drawn** — only while no folder has been set yet.
3. **What is remembered** — the folder in **Destination**. On the import
   dialog it is the folder that held the chosen file.

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

The tick box is gone once a folder is set, so neither dialog can change it.
Only the settings page can. That is
[UC-SETTING-006](../setting/setDownloadFolder.md).

That is question 3 on
[the settings page](../setting/main.md#not-decided).

## The label is different in each dialog

On the export and report dialogs the tick box stands alone. On the import
dialog it sits under a heading reading **Options:**. The box says the same
thing in both.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
