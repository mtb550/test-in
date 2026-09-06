[Documentation](../README.md) › [The project panel](main.md) › UC-003

# UC-003: Import a test project that already exists

> **No key.** Press **New Test Project** at the top of the panel, and paste
> the address instead of a name.

**As a** tester, **I want** to bring a test project that already exists
somewhere else onto this machine, **so that** I can work on it without building
it again by hand.

## Rules

- **Rule 18** — Cloning needs two things. It needs the Git plugin. It also needs
  this code project to already name the test project it is cloning.
- **Rule 79** — The folder is named by the code project, never by the address.
  A repository called `nafath-test-case` is a place to clone from. What the test
  project is called is written down once, in the file that travels with the
  repository, so the tree, the reports and the server path all read the same
  name.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The dialog is drawn under [UC-002](createTestProject.md).

## Main flow

**From a Git address**

1. The Git plugin is installed, and this code project already names the test
   project.
2. The tester presses the **New Test Project** button in the panel header.
3. The tester pastes the repository address instead of a name, and presses
   `Enter`.
4. Testin clones the repository into the Testin folder, under the name the code
   project gives.
5. Testin binds this code project to it, and the tree appears.
6. Testin shows *Project cloned*.

## What Testin refuses

**If this code project names no test project** — nothing is cloned. A warning
titled *No Test Project Named* says that the project file must say which test
project this code project is about before one can be cloned, and that the
tester can set it there or pick a project with **Select Test Project**.

**If the Git plugin is not installed** — nothing is cloned, and Testin says the
plugin is needed.

**If no Testin folder is set** — the **New Test Project** button is gray.
(rule 77)

---

## From an SFTP server: not built

> **⚠️ A tester cannot do this today.** Testin can sync a test project it
> already has with an SFTP server, which is part of reports, export, import and
> sync. It cannot bring a test project down from one that it does not have yet.
> There is no button, no menu item and no key for it.
>
> What it would need: somewhere to type the server, the folder and the account,
> and the same rule 79 decision about what the test project is called. Until
> that is built, a tester who keeps test projects on an SFTP server creates the
> test project first, sets up the SFTP account, and then syncs.

---

[Documentation](../README.md) › [The project panel](main.md)
