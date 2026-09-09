[Documentation](../README.md) › Sharing work with the team

# Sharing work with the team

Test data is files on one machine. This part is every way those files get to
somebody else and back: a spreadsheet handed over, a Git repository the whole
team pulls, or a server the plugin syncs with.

| | |
|---|---|
| **Part of Testin** | Sharing work with the team |
| **Answers** | How test cases leave Testin and come back, and how a test project is kept in step with a team |
| **Numbering** | Use cases are `UC-SHARE-001` to `UC-SHARE-023`. Rules are `Rule-SHARE-001` to `Rule-SHARE-106` |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `a53922a1`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

| | What the tester does | Why the tester would |
|---|---|---|
| | **Sending test cases out** | |
| **UC-SHARE-001** | [Export one test set](exportTestSet.md) | Send test cases to somebody who has no IDE. |
| **UC-SHARE-002** | [Export a package, one sheet for each test set](exportPackage.md) | Send many test sets to a reviewer as one file. |
| **UC-SHARE-003** | [Choose what goes into the export, and correct it](chooseWhatToExport.md) | Drop rows and fix typos before the file is written. |
| **UC-SHARE-004** | [Open the exported file, or copy its path](openExportedFile.md) | Check the new file right away. |
| | **Bringing test cases in** | |
| **UC-SHARE-005** | [Import into a test set](importIntoTestSet.md) | Bring a spreadsheet of test cases into one test set. |
| **UC-SHARE-006** | [Import into a package, one test set for each sheet](importIntoPackage.md) | Turn a whole workbook into many test sets. |
| **UC-SHARE-007** | [Choose what is imported, and correct it](chooseWhatToImport.md) | Fix what the file got wrong before it becomes test cases. |
| | **Working with Git** | |
| **UC-SHARE-008** | [Tell Git who I am](setGitIdentity.md) | Let Git put a name on the tester's commits. |
| **UC-SHARE-009** | [Put the test project under Git](putUnderGit.md) | Start versioning a test project Git does not know yet. |
| **UC-SHARE-010** | [See what I have not committed](reviewChanges.md) | See every change made since the last commit. |
| **UC-SHARE-011** | [Put one changed field back](revertOneChange.md) | Undo one field that was changed by mistake. |
| **UC-SHARE-012** | [Commit without pushing](commitChanges.md) | Record the work on this machine only, for now. |
| **UC-SHARE-013** | [Commit and push](commitAndPush.md) | Record the work and send it to the team at once. |
| **UC-SHARE-014** | [Commit onto a different branch](commitToBranch.md) | Keep these changes off the branch being tested now. |
| **UC-SHARE-015** | [Push a commit that never left this machine](pushOldCommit.md) | Find work the tester thinks the team already has. |
| **UC-SHARE-016** | [Send my changes and take the team's](syncWithGit.md) | Give and take work in one gesture. |
| **UC-SHARE-017** | [Resolve the conflicts a pull stopped on](resolveConflicts.md) | Finish a pull that two people's changes stopped. |
| **UC-SHARE-018** | [Answer which side wins for a field both changed](answerMergeQuestions.md) | Choose between the tester's wording and a colleague's. |
| | **Working with a server** | |
| **UC-SHARE-019** | [Sync with the team's server](syncWithServer.md) | Share test cases with a team that has no Git. |
| **UC-SHARE-020** | [Have my password kept for next time](keepServerPassword.md) | Type the server password once, not on every sync. |
| **UC-SHARE-021** | [Answer the conflicts the server sync could not settle](answerServerConflicts.md) | Say which version of a test case wins. |
| **UC-SHARE-022** | [Agree to remove files the server no longer holds](agreeToRemovals.md) | Approve a deletion before it reaches this machine. |

Cloning a test project from a repository is how a test project first arrives,
and it is [UC-TREE-PANEL-003](../treePanel/importTestProject.md).

---

## What this part is for

A test project is a folder of files. That is deliberate, and it is what makes
all of this possible: the files can be committed, synced, exported and read by
anything.

There are three ways to share them, and a team picks one.

| Way | What it suits |
|---|---|
| **Export and import** | Handing a set of test cases to somebody who has no IDE, or bringing in a spreadsheet somebody wrote |
| **Git** | A team that already uses Git, and wants the test data reviewed and versioned like code |
| **A server** | A team that wants the test data shared without a repository, over SFTP |

`testin.yml`, in the code repository, says which of the three this test project
uses. The settings page holds this machine's half of it.

**Four words, before the rules use them.**

- To **export** is to write test cases out to a file.
- To **import** is to read test cases from a file into a test set.
- To **sync** is to send what is here and take what is there, in one gesture.
- A **conflict** is a file two people changed since they last agreed.

---


## Every key

| Key | What it does | The page that owns it |
|---|---|---|
| `Shift+Enter` | **Review Changes**, on the message about uncommitted work | [UC-SHARE-010](reviewChanges.md) |
| `Enter` | Confirms the group picker, the Git identity, a merge answer, and a removal | The page that opens each |
| `Escape` | Cancels every dialog in this part | Everywhere |
| `Ctrl+Click` | Adds a group in the group picker | [UC-SHARE-003](chooseWhatToExport.md) |
| Right click | Puts one change back, in the review | [UC-SHARE-011](revertOneChange.md) |

**Nothing has a key** for: **Export**, **Import**, **Sync With Remote**, **View
Pending Commits**, **Sync With SFTP**, **Commit**, **Commit & Push**, and the
**Generate**, **Export** and **Import** buttons.

`Enter` does not confirm the export, import or review dialogs. Each has a button
instead.

---

## Which formats do what

| Format | A report | An export | An import |
|---|---|---|---|
| **XLSX** | Yes | Yes | Yes |
| **XLS** | No | No | **Yes** |
| **CSV** | No | Yes | Yes |
| **JSON** | No | Yes | Yes |
| **HTML** | Yes | Yes | No |
| **PDF** | Yes | No | No |
| **Word** | Yes | No | No |

The older spreadsheet format can be imported and not exported, on purpose.
Choosing it once produced a file in the newer format under the older name.

## What one exported row holds

Seventeen columns, in this order: **Description**, **ID**, **Expected Result**,
**Steps**, **Priority**, **FQCN**, **Reference**, **Test Data**, **Pre
Conditions**, **Group**, **Path**, **Module**, **Status**, **Created By**,
**Updated By**, **Created At**, **Updated At**.

The steps are joined by a comma, and so are the groups. A date is written in
full, with the time zone.

The JSON export is different. It writes every field of the test case as Testin
stores it, not the 17 columns.

## What an import reads

Thirteen columns: **Description**, **Expected Result**, **Steps**, **Priority**,
**Reference**, **Test Data**, **Pre Conditions**, **Group**, **Module**,
**Created By**, **Updated By**, **Created At**, **Updated At**.

**Order**, **ID**, **FQCN**, **Path** and **Status** are never imported. A file
carrying a status column has it ignored.

Headings are matched whatever their capitals, and spaces around them are
ignored. A heading Testin does not know is left alone.

---

## What needs the Git plugin

| Feature | Needs it |
|---|---|
| Export and import | No |
| Reports | No |
| Sync with a server | No |
| **Sync With Remote**, and everything reached from **View Pending Commits** | **Yes** |

Without the Git plugin those two menu entries are simply not there, and nothing
says why. A plugin that is installed but switched off counts as missing, and
switching it on needs the IDE restarted before Testin notices.

The Java plugin is needed for one thing here: an import generates test methods
for what it brought in. Without it the test cases still import, and a message
says once that the code was not generated.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | No rule covers it — nothing in this part says a refusal must be stated | Fixed. An empty folder, file name or format now turns that box's own placeholder red and puts the cursor in it, the way every dialog on the framework already did. |
| **Difference 6** | Rule-SHARE-003 — a conflict is put to the tester | Two fields are settled without asking. The order always takes the other side's value, and who changed it last always takes the later edit. |
| **Difference 11** | Rule-SHARE-005 — long work is watched | Parsing an import file has no progress bar at all, and runs on every keystroke in the source box. A large workbook makes the dialog look frozen. |
| **Difference 12** | Rule-SHARE-005 — the same | The export, import and report bars cannot be canceled. |

| **Difference 16** | Rule-SHARE-003 — the tester is told | A message can still be shown to nobody: with no status bar on the code project's window, the balloon is dropped. It is written to `testin.log` now rather than lost, so an export that finished with nobody told can be told from one that did not finish. Nothing is raised in its place — a notification about a failed notification is noise. |
| **Difference 18** | Rule-SHARE-004 — a value is checked before it is used | The Git email address is not checked. Any text is taken, and the failure arrives later in Git's own words. The remote address is checked now, by the rule the create project dialog already used. |
| **Difference 19** | Rule-SHARE-006 — a missing feature says so | Fixed. Both Git entries are in the menu in every IDE now, grayed and reading *(needs the Git plugin)* when it is missing, so the menu has one shape everywhere and the reason is on the entry (Rule-SHARE-105). |

**Retired.** A use case or a rule that is gone keeps its number forever, so an
issue that quotes one still leads somewhere and nothing is ever renumbered onto
it.

| Gone | Was | Read instead |
|---|---|---|
| **UC-SHARE-023** | *Remember the folder I use* — a **Set as default folder** tick box on the export, report and import dialogs wrote the machine's download folder. It was drawn only while no folder was set, so once one was there those dialogs could not change it back, and it overwrote a value the tester had set on the settings page without saying so. Removed 9 September 2026, [#240](https://github.com/mtb550/test-in/issues/240) | [UC-SETTING-006](../setting/setDownloadFolder.md) |
| **Rule-SHARE-102** | *The tick box is drawn only while no folder has been set yet.* | — |
| **Rule-SHARE-103** | *One folder is remembered, and every dialog uses it.* Said twice; the settings page owns the folder | [Rule-SETTING-021](../setting/setDownloadFolder.md) |
| **Rule-SHARE-104** | *The export dialog remembers the folder in its box. The import dialog remembers the folder holding the file that was chosen.* Two dialogs storing two different things under one name was half the defect | — |

**Fixed since this list was written.** The numbers are left out rather than
closed up, so an issue that quotes one still points at the right thing.

| Gone | Was |
|---|---|
| **Difference 3** | Escape on a merge question ended the whole sync and threw away every answer already given. Fixed 7 September 2026, [#258](https://github.com/mtb550/test-in/issues/258) |
| **Difference 4** | A merged file Git would not stage was reported only to the log. Fixed 7 September 2026, [#259](https://github.com/mtb550/test-in/issues/259) |
| **Difference 5** | An import that failed part way left what it had written and said nothing about how much. Fixed 7 September 2026, [#260](https://github.com/mtb550/test-in/issues/260) |
| **Difference 7** | Exporting a package walked only one level down, so nested test sets contributed nothing. Fixed 7 September 2026, [#262](https://github.com/mtb550/test-in/issues/262) |
| **Difference 8** | A test case file that would not read was dropped from an export in silence. Fixed 7 September 2026, [#263](https://github.com/mtb550/test-in/issues/263) |
| **Difference 10** | **No Group** was offered in the picker and thrown away on the way back in, because the label reached `Group.valueOf` and was read as an unknown group. Fixed 9 September 2026, [#265](https://github.com/mtb550/test-in/issues/265) |
| **Difference 15** | A message about none read *Exported 0*. A count of none says nothing now. Fixed 9 September 2026, [#269](https://github.com/mtb550/test-in/issues/269) |
| **Difference 13** | Choosing a file no format could read did nothing and said nothing. It names the file and the kinds Testin reads. Fixed 9 September 2026, [#267](https://github.com/mtb550/test-in/issues/267) |
| **Difference 17** | A refusal naming which nodes can be imported into, on nodes where the menu entry was already gray. The refusal is gone; the gray entry says it. Fixed 9 September 2026, [#271](https://github.com/mtb550/test-in/issues/271) |
| **Difference 14** | A Git sync faded, a push stayed, a server sync faded. All three stay: a sync lands on its own time, so it is the message a tester comes back to. Fixed 9 September 2026, [#268](https://github.com/mtb550/test-in/issues/268) |
| **Difference 2** | `Enter` did nothing in the export, import and review dialogs, which declared only `Escape`. All three answer it now, and the report dialog with them. Fixed 9 September 2026, [#252](https://github.com/mtb550/test-in/issues/252) |
| **Difference 9** | An unreadable value was quietly replaced four different ways - a priority became the lowest, a group was dropped from the list, a date became blank, a status kept whatever the row had - so 200 rows whose priority column read High, Medium and Low all arrived at the lowest priority in silence. One answer now, and it is said once with a count (Rule-SHARE-106). Fixed 9 September 2026, [#264](https://github.com/mtb550/test-in/issues/264) |

---

## Not decided

**Question 1** — Should an export walk the whole subtree rather than one level?

**Question 2** — Should an import be able to update a test case that is already
there, rather than always adding a new one?

**Question 3** — Two fields are merged without asking. Should the tester be told
afterwards which ones were settled for them?

---

[Documentation](../README.md) › **Sharing work with the team**
