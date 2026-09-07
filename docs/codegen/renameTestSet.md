[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-015

# UC-CODEGEN-015: Rename a test set

**As a** tester, **I want** the Java class to be renamed with the test set,
**so that** the class name still says what the test set is called.

Rename a test set, and Testin renames its Java class to match.

There is no key for this. It happens when a test set is renamed, which is
[UC-TREE-PANEL-011](../treePanel/renameNode.md).

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — Testin writes only the parts listed above. The body is
  the tester's, and Testin never touches it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — Nearly everything that goes wrong here is written only
  to the log.
- **Rule-CODEGEN-051** — The class is renamed before the test set is, while the
  old name still finds it.
- **Rule-CODEGEN-052** — The new class name is the new test set name cleaned,
  with `Test` on the end.

## What the tester sees

The node in the tree takes its new name, and a message reads *Renamed*. Nothing
on screen mentions the code. In the Project tool window, the Java file has a new
name too, and every place that used the old class name has been updated by the
IDE.

## Main flow

1. The tester renames a test set from **Login** to **Sign in**.
2. Testin renames the class from `LoginTest` to `SignInTest`, using the IDE's
   own rename, so everything that referred to it follows.
3. Testin then renames the test set itself.

## What Testin refuses

**If the class cannot be found** — nothing is renamed, and only the log says so.
The test set is still renamed, so the tree and the code then disagree.

**If the code project has no Java test source folder** — nothing is renamed, and
nothing is said at all.

**If the IDE has no Java plugin** — nothing is renamed.

## Why the code changes first

If the test set were renamed first, the old name would be gone. There would be
nothing left to find the class with. Every rename and every move in this part
happens before the tree changes, for the same reason.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
