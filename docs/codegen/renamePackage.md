[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-017

# UC-CODEGEN-017: Rename or move a package

**As a** tester, **I want** the Java package folder to follow a package I rename
or move, **so that** every class beneath it still declares the package it is
really in.

There is no key for this. It happens when a package is renamed or moved.

## Rules

- **Rule-CODEGEN-056** — Renaming a package renames its folder, then rewrites
  the package line in every Java file beneath it.
- **Rule-CODEGEN-057** — Moving a package does the same, after moving the
  folder.
- **Rule-CODEGEN-058** — A package dropped into itself is not a move, and
  nothing is rewritten.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

## What this covers

| The tester does this | What happens to the code |
|---|---|
| Renames a test set package | The folder is renamed, every file below declares the new package |
| Moves a test set package | The folder is moved, every file below declares the new package |
| Renames the test project | The folder at the top is renamed, and every file below follows |
| Removes the test project | The folder at the top is deleted, with everything under it |

Test run packages and test runs have no code, so nothing happens for them.

## Main flow

1. The tester renames a package from **Accounts** to **Identity**.
2. Testin renames the matching folder under the test source folder.
3. Testin walks every Java file beneath it.
4. Each file's package line is rewritten from where the file now sits.
5. Testin then renames the package itself in the tree.

## What Testin refuses

**If the folder cannot be found** — nothing is renamed, and only the log says
so.

**If the move goes to a place Testin has not read** — the folder is left where
it is, and only the log says so.

**If the code project has no Java test source folder** — nothing happens, and
nothing is said.

**If the IDE has no Java plugin** — nothing happens.

## Why every file below is rewritten

The package a Java file declares must match the folder it is in. Moving one
folder changes that for every file underneath it, however deep. Testin works
each one out from where the file now sits rather than by editing the old text,
so a file that was already wrong is put right too.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
