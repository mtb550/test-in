[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-016

# UC-VIEW-PANEL-016: Report a failed test case as a bug

**As a** tester, **I want** to file a failed test case as a GitHub issue already
written in my team's template, **so that** I retype nothing the run knows and
never report a bug twice.

Testin writes the bug from the test case, its test run and what the test run
recorded. The tester reads it, edits it if they want, and sends it. Testin files
it with the GitHub command line tool, `gh`. The screenshots pasted into the error
go with it.

There is no key for this. The **Report Bug** link is in the **Bug Issue** row of
the Details tab.

## Rules

- **Rule-VIEW-PANEL-001** — The panel is docked on the right of the IDE, and a
  tester can tell it from the tree panel at a glance.
- **Rule-VIEW-PANEL-002** — The panel shows one test case at a time.
- **Rule-VIEW-PANEL-003** — The panel never opens on its own. The tester asks
  for a test case's details, and it opens.
- **Rule-VIEW-PANEL-004** — Once open, the panel follows the tester. Once
  closed, it stays closed until the tester asks again.
- **Rule-VIEW-PANEL-005** — Every value is read again from Testin's memory each
  time the panel draws. The panel cannot show a value that was changed somewhere
  else.
- **Rule-VIEW-PANEL-006** — A field with nothing in it is not drawn. Its caption
  goes with it, so the panel is never a column of empty rows.
- **Rule-VIEW-PANEL-007** — Opening, paging and closing say nothing. There is no
  message for any of them.
- **Rule-VIEW-PANEL-008** — The panel has three tabs, and all three are drawn
  every time it refreshes.
- **Rule-VIEW-PANEL-009** — Closing a Testin editor empties the panel when the
  panel is showing one of that editor's test cases, and leaves it alone
  otherwise.
- **Rule-VIEW-PANEL-066** — A failed test case viewed under a test run has a **Bug Issue** row with a **Report Bug**
  link. The row stays for as long as the
  test case has a bug issue link, whatever its verdict.
- **Rule-VIEW-PANEL-067** — Report Bug prepares the bug under the IDE's progress
  bar, and reads `testin.yml` again first. Then the bug opens. Stopping the
  progress bar opens nothing.
- **Rule-VIEW-PANEL-068** — The bug is written in one template. Its title is the
  test case's description. Every value reads as the Details tab shows it. Every
  value Testin cannot get reads `n\a`. The
  template is not translated.
- **Rule-VIEW-PANEL-069** — The title and the body can be edited. **Send** files
  exactly what the dialog holds.
- **Rule-VIEW-PANEL-070** — Only a click on **Send** sends, and it sends once.
  `Enter` does not send. `Escape` closes the dialog without asking.
- **Rule-VIEW-PANEL-071** — When the bug cannot be sent, **Send** is gray.
  Hovering over it says why.
- **Rule-VIEW-PANEL-072** — Report Bug is gray while this test case's report is
  prepared, open or sent. It is also gray while another test case's report is
  open, and once the test case is reported. Hovering over it says why.
- **Rule-VIEW-PANEL-073** — Sending runs in the background and cannot be
  canceled. After 120 seconds it stops, and says it is not known whether the
  issue was created.
- **Rule-VIEW-PANEL-074** — The issue's address is stored on the test case's
  result only when the test run and the result still exist, and the test case is
  still failed. The message says *Reported* either way. It says why when the
  address was not stored. A completed or closed test run takes the address too.
  It is the one change a signed-off test run accepts, because it changes no
  verdict.
- **Rule-VIEW-PANEL-075** — A reported test case shows its issue as
  `owner/repo#123`. It is a link, and it opens the issue in the browser.
- **Rule-VIEW-PANEL-076** — Every screenshot pasted into the error is attached
  to the issue and shown under **Screenshots**. The error's text is folded under
  its first line.
- **Rule-VIEW-PANEL-077** — Edits that were not sent are kept for the test case
  until they are sent or thrown away. Report Bug after a refused or failed send
  brings them back.
- **Rule-VIEW-PANEL-078** — Testin stores no password and no token. `gh` holds
  the sign-in. `bugRepoUrl` in `testin.yml` names the repository.

## The Bug Issue row

Before the test case is reported:

```
┌──────────────────────────────────────────────────────────────┐
│   BUG SEVERITY                                               │
│   Blocker                                                    │
│   BUG PRIORITY                                               │
│   High                                                       │
│   BUG ISSUE                                                  │
│   Report Bug                                                 │
└──────────────────────────────────────────────────────────────┘
```

After:

```
┌──────────────────────────────────────────────────────────────┐
│   BUG SEVERITY                                               │
│   Blocker                                                    │
│   BUG PRIORITY                                               │
│   High                                                       │
│   BUG ISSUE                                                  │
│   mtb550/product#123    Report Bug                           │
└──────────────────────────────────────────────────────────────┘
```

1. **Report Bug** — prepares the bug and opens it. It is gray, with the reason on
   hover, whenever Rule-VIEW-PANEL-072 says so.
2. **`owner/repo#123`** — the issue this test case was reported as. Clicking it
   opens the issue in the browser.

## The Report Bug dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Report Bug                                                  │
├──────────────────────────────────────────────────────────────┤
│  [ Log in with a valid user                              ]   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ | Severity | Priority | Platform | Environment |       │  │
│  │ | 🔴 Blocker | 🔴 High | Web · Backend | n\a |          │  │
│  │                                                        │  │
│  │ ### Actual result                                      │  │
│  │ The session was dropped.                               │  │
│  └────────────────────────────────────────────────────────┘  │
│  SCREENSHOTS                                                 │
│  1                                                           │
│  REPOSITORY                                                  │
│  mtb550/product                                              │
│                                                  [ Send ]    │
├──────────────────────────────────────────────────────────────┤
│  Escape  Cancel                                              │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the test case's description, as the Details tab shows it. It
   can be edited. GitHub takes
   256 characters at most.
2. **The body** — the bug, written in the template below. It can be edited.
   GitHub takes 65,536 characters at most. Pasting a screenshot here pastes
   nothing.
3. **Screenshots** — how many screenshots go with the issue. They come from the
   error the tester pasted into the failure form.
4. **Repository** — where the issue is filed, from `bugRepoUrl` in `testin.yml`.
5. **Send** — files the issue. When it is gray, hovering over it says why.
6. **The bottom line** — `Escape` closes the dialog.

## The template

The body is Markdown. GitHub draws it as tables, headings and a folded error.
Anything Testin cannot get reads `n\a`.

| Part                             | Where it comes from                                               | Reads `n\a` when   |
|----------------------------------|-------------------------------------------------------------------|--------------------|
| **Severity**                     | The bug severity, with a colored dot                              | It is not set      |
| **Priority**                     | The bug priority, with a colored dot                              | It is not set      |
| **Platform**                     | The test run's platform and component                             | Both are empty     |
| **Environment**                  | Nothing in Testin                                                 | Always             |
| **Build**                        | Nothing in Testin                                                 | Always             |
| **Actual result**                | What the tester says actually happened, exactly as typed          | It is empty        |
| **Expected result**              | The test case's expected result, as the Details tab shows it      | It is empty        |
| **Steps to reproduce**           | The test case's steps, numbered, each as the Details tab shows it | There are no steps |
| **Test data**                    | The test case's test data, exactly as typed                       | It is empty        |
| **Impact**                       | Nothing in Testin                                                 | Always             |
| **Exception**                    | The error's text, folded under its first line                     | There is no text   |
| **Screenshots**                  | Every screenshot pasted into the error                            | There are none     |
| **Test run**                     | The test run's name                                               | Never              |
| **Executed**                     | Who recorded the verdict, and when                                | Nobody has         |
| **Browser, device and language** | The test run's answers, each on its own                           | Each one is empty  |
| **Commit**                       | The test run's commit                                             | It is empty        |
| **Test case**                    | The first eight characters of its identity, and its test set      | Never              |
| **The last line**                | The test set's name as a tag                                      | Never              |

The **Test case** part links to the test case's file on GitHub. The link is left
out when Git cannot say where the file is. That happens without the Git plugin,
without a remote, or when the test project folder is not its repository's root.
The link opens once the file has been pushed.

The tester's own words cannot break the tables or the folded error. They cannot
mention a GitHub user or link another issue either.

## Main flow

1. The tester opens a failed test case's details from a test run.
2. The **Bug Issue** row shows **Report Bug**. The tester clicks it.
3. The IDE's progress bar reads *Preparing the bug report*.
4. The **Report Bug** dialog opens, with the title and the body written.
5. The tester edits the title or the body, or leaves them.
6. The tester clicks **Send**. The dialog closes.
7. The IDE's progress bar reads *Sending the bug report*.
8. A message appears and stays in the notification list. Its title is *Reported*. It shows the issue as
   `owner/repo#123`, with an **Open** link.
9. The **Bug Issue** row shows the issue. **Report Bug** is gray, and hovering
   over it reads *Already reported*.

## What Testin refuses

**If `testin.yml` has no `bugRepoUrl`** — **Send** is gray. Hovering over it
reads *Add bugRepoUrl to testin.yml to say which repository bugs are filed in*.

**If `bugRepoUrl` is not a repository address** — **Send** is gray. Hovering
over it reads *bugRepoUrl is not a GitHub repository address*.

**If `gh` is missing, or the IDE cannot find it** — **Send** is gray. Hovering
over it reads *GitHub CLI (gh) is not installed, or the IDE cannot see it.
Install gh, then restart the IDE (and JetBrains Toolbox)*.

**If `gh` is older than 2.99.0** — **Send** is gray. Hovering over it reads *gh 2.87.0 is too old to attach screenshots.
Report Bug needs 2.99.0 or newer*,
with the numbers of this machine.

**If `gh` is not signed in to the repository's host** — **Send** is gray.
Hovering over it reads *Not signed in to \<host\>. Run gh auth login
--hostname \<host\>*, where \<host\> is the repository's host.

**If the title is empty** — **Send** is gray. Hovering over it reads *The title
is empty*.

**If the title is longer than 256 characters** — **Send** is gray. Hovering over
it reads *The title is 300 characters long, and GitHub takes 256 at most*, with
the real length.

**If the body is longer than 65,536 characters** — **Send** is gray. Hovering
over it reads *The body is 70,000 characters long, and GitHub takes 65,536 at
most*, with the real length. A long stacktrace is the usual reason.

**If more than 50 screenshots were pasted** — **Send** is gray. Hovering over it
reads *51 screenshots are pasted, and gh attaches 50 at most*, with the real
count.

**If the test case is no longer failed when Send is clicked** — nothing is sent. **Send** turns gray, and hovering over
it reads *This test case is no longer
failed*. The dialog stays open.

**If this test case's report is being prepared, is open or is being sent** — **Report Bug** is gray. Hovering over it
reads *Preparing the bug report*, *The
bug report is open* or *Sending the bug report*.

**If another test case's report is open** — **Report Bug** is gray. Hovering
over it reads *Finish the open bug report first*.

**If the test case is already reported** — **Report Bug** is gray. Hovering over
it reads *Already reported*.

**If `gh` does not answer within 120 seconds** — sending stops. A message titled *Report Bug Failed* reads *gh did not
answer within 120 seconds. It is not known
whether the issue was created, so check GitHub before reporting again*. Nothing
is stored. Report Bug brings the edits back.

**If `gh` refuses** — a message titled *Report Bug Failed* shows what `gh` said.
When `gh` said nothing, it reads *gh stopped with exit code 1 and said nothing*,
with the real code. Nothing is stored. Report Bug brings the edits back.

**If the bug cannot be written to a temporary folder** — a message titled *Report Bug Failed* reads *The bug report
could not be written to a temporary
folder:*, then the reason. Nothing is sent.

**If some screenshots do not upload** — the issue is still created and stored.
The *Reported* message also reads *Screenshots not uploaded: 1*, with the real
count.

**If the test run was renamed or removed while sending** — the issue exists, and
nothing is stored. The *Reported* message also reads *Not stored: the test run
was renamed or removed*.

**If the test case was no longer failed when the issue was created** — the issue
exists, and nothing is stored. The *Reported* message also reads *Not stored: the
test case was no longer failed when it was sent*.

## What cannot be undone

**An issue stays on GitHub.** Testin never closes, reopens or edits it.

**A pass clears the link.** Recording a pass on the test case clears its bug
issue link with the rest of what the failure recorded. The issue stays on
GitHub. When the test case fails again, **Report Bug** is back.

---

[Documentation](../README.md) › [The view panel](main.md)
