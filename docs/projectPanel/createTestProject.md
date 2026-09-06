[Documentation](../README.md) › [The project panel](main.md) › UC-002

# UC-002: Create a test project

**As a** tester, **I want** to create a test project by name, or clone one from
a Git address, **so that** a new product under test has a place before any test
is written.

## Rules

- **Rule 16** — A test project is a folder directly under the Testin folder.
  Any other folder there is ignored.
- **Rule 17** — Creating a test project binds this code project to it.
- **Rule 18** — Cloning needs two things. It needs the Git plugin. It also needs
  this code project to already name the test project it is cloning.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

The screens are drawn under [UC-001](reachTheTree.md).

## Main flow

**By name**

1. The tester presses the **New Test Project** button in the panel header, or
   the welcome link **Create your first test project**.
2. The tester types a name and presses `Enter`.
3. Testin creates the test project folder in the Testin folder.
4. Testin binds this code project to it, and the tree appears.
5. Testin shows *Project created*.

**By Git address**

1. The Git plugin is installed, and this code project already names the test
   project.
2. The tester pastes a repository address instead of a name, and presses
   `Enter`.
3. Testin clones the repository under that name.
4. Testin binds this code project to it, and the tree appears.
5. Testin shows *Project cloned*.

## What Testin refuses

**If a folder with that name already exists in the Testin folder** — nothing is
created, and *\<name\> Already Exists* is shown in red.

**If this code project names no test project, and the tester pastes an address**
— nothing is cloned. A warning titled *No Test Project Named* explains that the
code project must say which test project it is about.

---

[Documentation](../README.md) › [The project panel](main.md)
