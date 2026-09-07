[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-020

# UC-CODEGEN-020: Work in a project with no Java test folder

**As a** tester, **I want** to know why no code is appearing,
**so that** I can mark a folder as holding tests and have it start working.

Nothing starts this. It is the state of the code project.

## Rules

- **Rule-CODEGEN-064** — Testin looks for the test source folder once when the
  code project opens, and remembers what it found.
- **Rule-CODEGEN-065** — Anything that would create code says so. Anything that
  only tidies up after a removal stays silent.
- **Rule-CODEGEN-066** — The first test source folder of the first module that
  has one is the one Testin uses.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester opens a code project with no folder marked as holding Java tests.
2. Testin looks through the modules in the background.
3. It finds none.
4. A message titled **Java Test Source Not Found** reads *Unable to find a Java
   test source package in this project - creation of automation packages,
   classes, and methods will be skipped.*
5. The tester marks a folder as a test source folder in the IDE's own project
   settings.
6. Testin looks again the next time it needs the folder.

## What Testin refuses

**Every creation** — a message titled **Java Test Source Not Found** reads
*Unable to find a Java test source package - automation code was not generated.*
The test set or test case is still created.

**Every removal, rename and move** — nothing at all is said. There was no code
to tidy up, and reporting that would be alarming for something that does not
matter.

## Why a missing folder is not a failure

Test management is the point of Testin, and the generated code is a convenience
on top of it. A team that writes its automation somewhere else, or has not
started yet, should be able to use every other part of the plugin. So a missing
folder is a skip with a message, never an error.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
