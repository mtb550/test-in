[Documentation](../README.md) › Sharing work with the team

# Sharing work with the team

Test data is files on one machine. This part is every way those files get to
somebody else and back: a spreadsheet handed over, a Git repository the whole
team pulls, or a server the plugin syncs with.

| | |
|---|---|
| **Part of Testin** | Sharing work with the team |
| **Answers** | How test cases leave Testin and come back, and how a test project is kept in step with a team |
| **Numbering** | Use cases are `UC-SHARE-001` to `UC-SHARE-023`. Rules are `Rule-SHARE-001` to `Rule-SHARE-101` |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `a53922a1`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

| | What the tester does | |
|---|---|---|
| | **Sending test cases out** | |
| **UC-SHARE-001** | [Export one test set](exportTestSet.md) | |
| **UC-SHARE-002** | [Export a package, one sheet for each test set](exportPackage.md) | |
| **UC-SHARE-003** | [Choose what goes into the export, and correct it](chooseWhatToExport.md) | |
| **UC-SHARE-004** | [Open the exported file, or copy its path](openExportedFile.md) | |
| | **Bringing test cases in** | |
| **UC-SHARE-005** | [Import into a test set](importIntoTestSet.md) | |
| **UC-SHARE-006** | [Import into a package, one test set for each sheet](importIntoPackage.md) | |
| **UC-SHARE-007** | [Choose what is imported, and correct it](chooseWhatToImport.md) | |
| | **Working with Git** | |
| **UC-SHARE-008** | [Tell Git who I am](setGitIdentity.md) | |
| **UC-SHARE-009** | [Put the test project under Git](putUnderGit.md) | |
| **UC-SHARE-010** | [See what I have not committed](reviewChanges.md) | |
| **UC-SHARE-011** | [Put one changed field back](revertOneChange.md) | |
| **UC-SHARE-012** | [Commit without pushing](commitChanges.md) | |
| **UC-SHARE-013** | [Commit and push](commitAndPush.md) | |
| **UC-SHARE-014** | [Commit onto a different branch](commitToBranch.md) | |
| **UC-SHARE-015** | [Push a commit that never left this machine](pushOldCommit.md) | |
| **UC-SHARE-016** | [Send my changes and take the team's](syncWithGit.md) | |
| **UC-SHARE-017** | [Resolve the conflicts a pull stopped on](resolveConflicts.md) | |
| **UC-SHARE-018** | [Answer which side wins for a field both changed](answerMergeQuestions.md) | |
| | **Working with a server** | |
| **UC-SHARE-019** | [Sync with the team's server](syncWithServer.md) | |
| **UC-SHARE-020** | [Have my password kept for next time](keepServerPassword.md) | |
| **UC-SHARE-021** | [Answer the conflicts the server sync could not settle](answerServerConflicts.md) | |
| **UC-SHARE-022** | [Agree to remove files the server no longer holds](agreeToRemovals.md) | |
| | **Both** | |
| **UC-SHARE-023** | [Remember the folder I use](chooseFolderOnce.md) | |

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

## Rules that hold everywhere

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. The Git ones can be
  canceled. The export, import and report ones cannot.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.

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
| **Difference 1** | Rule-SHARE-005 — a refusal is stated | Leaving the folder, the file name or the format empty in the export or report dialog says nothing at all. The cursor moves and the dialog stays open. |
| **Difference 2** | Rule-SHARE-006 — a dialog answers `Enter` | `Enter` does nothing in the export, import and review dialogs. Only a button confirms them. |
| **Difference 3** | Rule-SHARE-002 — an answer the tester gave is used | `Escape` on a merge question throws away every answer already given for that sync, with no message, and nothing more is asked. |
| **Difference 4** | Rule-SHARE-003 — the tester knows what happened | A merged file Git will not take is reported only to the log. The pull stops again with no conflict on screen to explain it. |
| **Difference 5** | Rule-SHARE-002 — an import is all or nothing | An import that fails part way leaves what it already wrote. Nothing says how many landed. |
| **Difference 6** | Rule-SHARE-003 — a conflict is put to the tester | Two fields are settled without asking. The order always takes the other side's value, and who changed it last always takes the later edit. |
| **Difference 7** | Rule-SHARE-001 — an export writes what is there | Exporting a package walks only one level down. A test set nested two levels deep contributes nothing, and nothing says so. |
| **Difference 8** | Rule-SHARE-001 — the same | A test case file that cannot be read is left out of an export in silence, and the count in the message is of what was gathered. |
| **Difference 9** | Rule-SHARE-002 — a value the tester typed is kept or refused | An unreadable value is quietly replaced in four different ways. A priority becomes the lowest, a group is dropped, a date becomes blank, a status keeps its old value. |
| **Difference 10** | Rule-SHARE-002 — what is offered can be used | **No Group** is offered in the group picker and cannot be imported back. It is dropped without a word. |
| **Difference 11** | Rule-SHARE-005 — long work is watched | Parsing an import file has no progress bar at all, and runs on every keystroke in the source box. A large workbook makes the dialog look frozen. |
| **Difference 12** | Rule-SHARE-005 — the same | The export, import and report bars cannot be canceled. |
| **Difference 13** | Rule-SHARE-006 — one look for one thing | Choosing a file Testin cannot import does nothing and says nothing. |
| **Difference 14** | Rule-SHARE-003 — one word for one outcome | A Git sync says *Synced* and fades. A Git push says *Pushed* and stays in the notification list. A server sync says *Synced* and fades, unless there were conflicts, when it stays. |
| **Difference 15** | Rule-SHARE-003 — a count reads as a count | A message about one thing reads *Exported*. A message about none reads *Exported 0*. |
| **Difference 16** | Rule-SHARE-003 — the tester is told | A message can be shown to nobody. When the code project's window has no status bar, the message is dropped and nothing is reported. Every success here is that kind of message. |
| **Difference 17** | Rule-SHARE-006 — a refusal that cannot happen is not written | The import refusal naming which nodes can be imported into can never be seen. The menu entry is already gray in exactly that case. |
| **Difference 18** | Rule-SHARE-004 — a value is checked before it is used | Neither the remote address nor the Git email address is checked. Any text is taken, and the failure arrives later in Git's own words. |
| **Difference 19** | Rule-SHARE-006 — a missing feature says so | Sync with a server is offered in every IDE. The two Git entries vanish with no word, so a tester has no way to learn the Git plugin is why. |

---

## Not decided

**Question 1** — Should an export walk the whole subtree rather than one level?

**Question 2** — Should an import be able to update a test case that is already
there, rather than always adding a new one?

**Question 3** — Two fields are merged without asking. Should the tester be told
afterwards which ones were settled for them?

---

[Documentation](../README.md) › **Sharing work with the team**
