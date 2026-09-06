[Documentation](../README.md) › The settings page

# The settings page

The settings page is where Testin learns about this machine and this person:
where the test data lives, who is using it, and how to reach the team's server.

| | |
|---|---|
| **Part of Testin** | The settings page |
| **Answers** | What every setting does, where each one is kept, and what happens when one is wrong |
| **Numbering** | Use cases are `UC-SETTING-001` to `UC-SETTING-011`. Rules are numbered 1 and up, and belong to the settings page |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `779fe6b4`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

| | What the tester does | |
|---|---|---|
| **UC-SETTING-001** | [Open the settings page](openSettings.md) | |
| **UC-SETTING-002** | [Set the Testin folder](setTestinFolder.md) | |
| **UC-SETTING-003** | [Open the Testin folder on this machine](openTestinFolder.md) | |
| **UC-SETTING-004** | [Give my name](setTesterName.md) | |
| **UC-SETTING-005** | [Give my role](setTesterRole.md) | |
| **UC-SETTING-006** | [Set the folder that files are saved to](setDownloadFolder.md) | |
| **UC-SETTING-007** | [Choose how much Testin writes to its log](setLogLevel.md) | |
| **UC-SETTING-008** | [Turn the shortcut hints off](hideShortcutHints.md) | |
| **UC-SETTING-009** | [Name my account on the team's server](setSftpAccount.md) | |
| **UC-SETTING-010** | [Name the key file this machine offers](setSftpKeyFile.md) | |
| **UC-SETTING-011** | [Change the size of Testin's text](changeTextSize.md) | |

Choosing which test project a code project uses is not on this page. It is
written into a file the whole team shares, and it is
[UC-TREE-PANEL-004](../treePanel/chooseTestProject.md).

---

## What the page is for

Two things have to be true before Testin can show anything. It has to know which
folder on this machine holds the test projects, and each code project has to say
which test project it is about.

The two are kept apart on purpose, and the reason is who they belong to.

- **A setting belongs to this machine and this person.** The Testin folder, the
  tester's name, the server account. None of them is committed, and each one is
  the same in every code project open in this IDE.
- **The choice of test project belongs to the team.** It is written into a file
  in the code repository, so a colleague who clones the repository gets it too.
  That file names no machine and no person.

**Three words, before the rules use them.**

- The **Testin folder** is the one folder on this machine that holds test
  projects. The page calls it *Testin source root*.
- **This machine's settings** are one set of values shared by every code project
  open in this IDE.
- The **password store** is the IDE's own keychain. Testin puts passwords there
  and nowhere else.

---

## Rules that hold everywhere on the page

- **Rule 1** — One page for the whole IDE. Every code project open in it reads
  the same values.
- **Rule 2** — Nothing on this page is checked. A folder that does not exist is
  stored exactly as typed.
- **Rule 3** — Nothing on this page raises a message when it is saved.
- **Rule 4** — Only a changed Testin folder makes Testin read the disk again.
  Every other setting is read where it is used, when it is used.
- **Rule 5** — A password is never on this page. It is asked for when it is
  needed and kept in the IDE's password store.
- **Rule 6** — Nothing on this page has a key of its own.

---

## The page

The page is at **Settings**, then **Tools**, then **Testin**.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Testin                                                                    │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   Testin source root:      [ Example -> C:\Users\...\Testin ] [...] [Open] │
│                                                                            │
│   Log level:               [ INFO                                     v ]  │
│                                                                            │
│   Tester name:             [                                            ]  │
│                                                                            │
│   Tester role:             [                                            ]  │
│                                                                            │
│   Default download folder: [                                     ] [...]   │
│                                                                            │
│   SFTP account:            [                                            ]  │
│                                                                            │
│   SFTP key file:           [                                     ] [...]   │
│                                                                            │
│   [x] Show keyboard shortcuts in dialogs                                   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Testin source root** — the folder holding every test project. The only
   field with a gray example in it.
2. **The browse button** — opens a folder chooser. Three fields have one.
3. **Open** — opens the Testin folder in the file manager. It is gray until the
   box names a folder that is really there.
4. **Log level** — how much Testin writes to its own log.
5. **Tester name** — stamped on everything this machine writes.
6. **Tester role** — stored, and read by nothing. That is difference 2 below.
7. **Default download folder** — where saving a report, an export or an import
   starts.
8. **SFTP account** — who this machine is on the team's server.
9. **SFTP key file** — the key this machine offers that server.
10. **Show keyboard shortcuts in dialogs** — the strip of key hints along the
    bottom of every Testin dialog.

**OK**, **Apply** and **Cancel** belong to the IDE's own settings window, not to
this page.

---

## Where each setting is kept

| Setting | Where it lives | Committed |
|---|---|---|
| Testin source root | This machine's settings | No |
| Log level | This machine's settings | No |
| Tester name | This machine's settings | No |
| Tester role | This machine's settings | No |
| Default download folder | This machine's settings | No |
| SFTP account | This machine's settings | No |
| SFTP key file | This machine's settings | No |
| Show keyboard shortcuts | This machine's settings | No |
| Which test project this repository is about | `testin.yml`, in the code repository | **Yes** |
| How to reach the team's server | `testin.yml`, in the code repository | **Yes** |
| A password, or a key file's passphrase | The IDE's password store | No |

Nothing on this page is ever committed. That is the reason the page exists
rather than putting these values in `testin.yml`.

---

## Why it is built this way

**Two stores, not three.** A value that differs between colleagues goes in
`testin.yml`, so a clone needs no setting up. A value that differs between
machines goes here, so it cannot be committed by accident. A third store would
be a second answer to the same question, and the two would start disagreeing.

**The server address is shared, the account is not.** `testin.yml` carries the
host. If someone writes an account into the host as well, Testin drops it and
says so in the log. The file is shared with everyone, and an account is one
person's.

**Passwords are never in a file Testin writes.** They go to the IDE's password
store, one entry per server and account, so two servers do not overwrite each
other. Testin never writes a password to the log.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | Rule 2 — nothing is checked, so nothing warns | A Testin folder that does not exist, or is a file, is stored without a word. The tree then shows its empty state, and nothing connects that to the path just typed. |
| **Difference 2** | Rule 1 — a setting is read by something | **Tester role** is stored and read by nothing at all. It is on no marker, no report and no message. |
| **Difference 3** | Rule 2 — a value is stored as typed | The Testin folder is stored exactly as typed, spaces included, and then trimmed the next time a code project opens. The value in the file changes on its own. |
| **Difference 4** | Rule 1 — this page owns these values | The default download folder is also written by the import dialog and the export dialog. Choosing a folder there and ticking a box overwrites what this page says. |
| **Difference 5** | Rule 4 — changing the folder makes Testin read again | Only code projects that have opened the Testin panel read again. A project whose panel was never opened keeps the old folder until it is opened. |

---

## Not decided

**Question 1** — Should **Tester role** exist? Nothing reads it. Either
something should, or the field should go.

**Question 2** — Should the Testin folder be checked when it is typed? Every
other refusal in Testin is stated at the moment it happens.

**Question 3** — There is no way to clear the default download folder from the
import or export dialogs. The box that sets it disappears once it is set. Only
this page can change it back.

---

[Documentation](../README.md) › **The settings page**
