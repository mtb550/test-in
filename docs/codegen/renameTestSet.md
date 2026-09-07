[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-015

# UC-CODEGEN-015: Rename a test set

**As a** tester, **I want** the Java class to be renamed with the test set,
**so that** the class name still says what the test set is called.

There is no key for this. It happens when a test set is renamed, which is
[UC-TREE-PANEL-011](../treePanel/renameNode.md).

## Rules

- **Rule-CODEGEN-051** — The class is renamed before the test set is, while the
  old name still finds it.
- **Rule-CODEGEN-052** — The new class name is the new test set name cleaned,
  with `Test` on the end.

Rule-CODEGEN-001 to Rule-CODEGEN-006 hold everywhere. They are on
[the automation code page](main.md#rules-that-hold-everywhere).

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

If the test set were renamed first, the old name would be gone and there would
be nothing left to find the class with. Every rename and every move in this part
happens before the tree changes, for the same reason.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
