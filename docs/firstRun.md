[Documentation](README.md) › First run

# First run — ten minutes, install to a verdict

> Nine steps, from a freshly installed plugin to a test case you wrote, a Java
> method Testin wrote for it, a recorded verdict and a report you can send.
> Nothing here needs the rest of the documentation.

| | |
|---|---|
| **Part of Testin** | All of it, once, in the shortest line through |
| **Answers** | What the plugin is for, shown rather than described |
| **State** | Written |
| **Checked against** | `main` at `d1b80428`, 9 September 2026 |
| **You need** | IntelliJ IDEA 2025.2 or later, the Java plugin enabled, and a Java project with a `src/test/java` |

The screens below are drawn rather than photographed, the same way every other
page in this documentation draws them. They show what is on the screen, not what
it looks like.

---

## Before you start

**Two folders, and they are not the same thing.**

| | What it holds | Who owns it |
|---|---|---|
| **The Testin folder** | Your test projects: sets, cases, runs, all as JSON | You. It is a setting on this machine |
| **The automation project** | The Java code — the test methods Testin writes | Your team. It is the repository you already have open |

Testin keeps them apart on purpose: test data changes when a tester writes a
case, code changes when a developer commits, and putting them in one folder
makes every test case edit a diff in the code repository.

Pick any empty folder for the first one — `Documents/Testin` will do.

---

## 1. Open the panel

Click **Testin** on the tool window bar, at the edge of the IDE. There is no
key for this; it is the IDE's own button.

With no Testin folder set yet, the panel says so rather than showing an empty
tree:

```
┌────────────────────────────────────────────────────────────┐
│  Welcome to Testin                                         │
│                                                            │
│  Choose the folder your test projects live in.             │
│                                                            │
│                    [ Open Settings ]                       │
└────────────────────────────────────────────────────────────┘
```

## 2. Point Testin at a folder

Press **Open Settings**, or go to **Settings → Tools → Testin**.

Set two things and leave the rest:

| Setting | What to put |
|---|---|
| **Testin folder** | The empty folder you picked |
| **Tester name** | Your name. It is written into every case and every verdict you record |

Everything on this page belongs to this machine and this person. Nothing here is
committed — [the settings page](setting/main.md) says where each value lives and
why.

## 3. Create a test project

A test project is one product under test. Select the root of the tree and press
`Ctrl+M`, or use the context menu, and choose **Test Project**.

Call it `Demo`. Testin makes the folder and the two fixed folders inside it:

```
Demo
├── Test Cases
└── Test Runs
```

Those two names are fixed. Test cases go in one, records of testing go in the
other, and nothing else at that level is read.

## 4. Create a test set

A test set is a group of test cases that belong together — usually one feature.

Select **Test Cases**, press `Ctrl+M`, choose **Test Set**, and call it `Login`.

Double-click it, or press `Enter`, and it opens in an editor.

## 5. Write a test case

In the test set editor, press `Ctrl+M`.

Fill in the description — *Log in with a valid user* — and the expected result —
*The dashboard opens*. Save.

The card appears:

```
┌──────────────────────────────────────────────────────────────────────────┐
│  1. Log in with a valid user.                    ( P1 ) ( Regression )   │
│     Expected Result: The dashboard opens.                                │
└──────────────────────────────────────────────────────────────────────────┘
```

Write two more, so the run in step 8 has something to move through.

> **The period is not in your data.** Testin capitalizes and closes a
> description when it draws it, and stores exactly what you typed. Open the
> `.json` file if you want to check — it is yours, in plain text.

Press `F2` on a card to change one field, or any of `D` `E` `M` `T` `B` `S` `P`
`G` `O` to open that field straight away.

## 6. Let Testin write the Java

Right-click the test set and choose **Generate Code**.

Testin writes a TestNG class into your project's `src/test/java`, one `@Test`
method per test case:

```java
@Test(priority = 1, description = "Log in with a valid user")
public void logInWithAValidUser() {
    // TODO: Auto-generated test steps for logInWithAValidUser
}
```

The method is empty and it is meant to be — Testin writes the skeleton and keeps
it in step with the tree. Renaming the case renames the method; removing the
case leaves the method alone, because the code is yours.

Press `Shift+F5` on a card to jump from a test case to its method.

## 7. Create a test run

A test run is one round of testing over the cases you choose.

Select **Test Runs**, press `Ctrl+M`, choose **Test Run**, call it `Cycle-1`,
and pick the `Login` set.

## 8. Record a verdict

Open the run. Each case is a card, and three keys record what happened:

| Key | Verdict |
|---|---|
| `P` | Passed |
| `F` | Failed — and it asks what went wrong |
| `B` | Blocked |

Press `P` on the first card. The cursor moves to the next one on its own, so a
whole run is `P P P` without touching the mouse.

Press `F` on one of them and fill in the actual result, so the report in step 9
has a failure in it.

> **Try light mode.** It is a small always-on-top window showing the case being
> executed, so the application under test can have the whole screen. The same
> three keys work in it.

## 9. Send the result to someone with no IDE

Select the run and press `Ctrl+P`.

Choose a format:

| Format | What it is for |
|---|---|
| **PDF** | The one to attach to a ticket |
| **Word** | The one to edit before sending |
| **HTML** | The one to open in a browser — it carries a light and dark switch |
| **Excel** | The one to filter and sort |

The document holds what the run recorded: the totals, the cases that passed, and
every failure with what you wrote about it.

---

## That is the loop

Write cases → generate the code → run them → record verdicts → send the report.
Everything else in Testin is a shorter or wider version of those five things.

## Where to go next

| If you want to | Read |
|---|---|
| Know every key | [Every shortcut](shortcuts.md) |
| Understand the tree | [The tree panel](treePanel/main.md) |
| Write and edit cases in bulk | [The editor panel](editorPanel/main.md) |
| Share test data with your team | [Sharing work with the team](share/main.md) |
| See what the files look like | [The formats on disk](formats.md) |

## If you cloned this repository

You do not need steps 1 to 5. `./gradlew runIde` opens a sandbox IDE pointed at
`samples/testin-root`, which already holds a `Demo` test project with two test
sets, six cases and two runs — one completed, one in progress. Start at step 6.
