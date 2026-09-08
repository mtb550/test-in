[Documentation](../README.md) › [Reports](main.md) › UC-REPORT-001

# UC-REPORT-001: Generate a report on a test run

**As a** tester, **I want** one test run written out as a document,
**so that** I can attach it to a ticket or send it to someone with no IDE.

This turns one test run into a file: a PDF, a Word file, a web page or a
spreadsheet.

`Ctrl+P` on the selected test run.

## Rules

- **Rule-REPORT-001** — A report is about exactly one test run.
- **Rule-REPORT-002** — Every format reads the same figures, so two reports on
  one test run cannot disagree.
- **Rule-REPORT-003** — A report is written after the dialog closes, under a
  progress bar the tester can cancel. Stopping it leaves no file behind: the
  report is written in one go at the end.
- **Rule-REPORT-004** — A report is written where the tester chose. It never
  lands under the Testin folder.
- **Rule-REPORT-005** — The project named in a report is the test project, not
  the code project the IDE has open.
- **Rule-REPORT-006** — The file name is filled in already. It names the test
  project, the test run, the date and the time.
- **Rule-REPORT-007** — Spaces and special characters are taken out of the
  project and run names in the file name.
- **Rule-REPORT-008** — A part of the name that is empty is left out, rather
  than leaving a gap.
- **Rule-REPORT-009** — The dialog closes before the work starts.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Generate Report                                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Save to      [ C:\Users\mtb\Downloads              ] [...]  │
│                                                              │
│  File name    [ TestRun_Demo_cycle2_07-09-2026_02-14-33PM ]  │
│                                                              │
│  Format       [ PDF                                      v ] │
│                                                              │
│  [ ] Set as default folder                                   │
│                                                              │
│                                        [ Generate ]          │
├──────────────────────────────────────────────────────────────┤
│  [k]  Escape Cancel                                          │
└──────────────────────────────────────────────────────────────┘
```

1. **Save to** — where the file goes. It opens on the folder the settings page
   names.
2. **File name** — filled in already, and can be changed.
3. **Format** — **XLSX**, **HTML**, **PDF** or **WORD**. **PDF** is chosen.
4. **Set as default folder** — drawn only while no default folder has been set.
   It is [UC-SETTING-006](../setting/setDownloadFolder.md).
5. **Generate** — writes the file. `Enter` does not.

## Main flow

1. The tester selects a test run in the tree and presses `Ctrl+P`.
2. The **Generate Report** dialog opens with the name already filled in.
3. The tester picks a folder, and a format.
4. The tester presses **Generate**.
5. The dialog closes.
6. A progress bar reads *Generating the*, the format, then *report for*, then
   the test run's name.
7. Testin reads the test run and every test case it names. Then it writes the
   file.
8. A message appears. Its title is the format and the words *Report Generated*.
   It reads *Saved successfully:* and then the file name.
9. The message carries two links, **Open report** and **Copy path**.

## The three ways in

| The tester does this | Where |
|---|---|
| Presses `Ctrl+P` | On a selected test run in the tree, or in a run editor |
| Chooses **Generate Report** | The tree's menu, or the run editor's menu |
| Presses the report button | The run editor's toolbar. Its tooltip reads **Generate Test Summary Report** |

## What Testin refuses

**If the selection is not a test run** — **Generate Report** is gray in the tree
menu. A report is about one test run, and nothing else.

**If the file name is empty** — nothing is written and nothing is said. The
cursor moves to the file name box and the dialog stays open.

**If the folder is empty** — the same, with the cursor moving to the folder box.

**If no format is chosen** — the same again.

**If the file cannot be written** — a message titled **Report Error** reads
*Failed to generate*, the format, *report:*, and then the reason.

**If the tester presses `Enter`** — nothing happens. The dialog answers only to
**Generate** and to `Escape`.

## Where the plugin breaks its own rules

**Three refusals are silent.** An empty folder, an empty file name and no format
each move the cursor and say nothing. That is difference 1 on
[the reports page](main.md#where-the-plugin-breaks-its-own-rules).

**The report button on the run editor is never gray.** Pressed where no test run
can be worked out, it does nothing at all. That is difference 3.

---

[Documentation](../README.md) › [Reports](main.md)
