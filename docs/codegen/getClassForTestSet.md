[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-001

# UC-CODEGEN-001: Get a class when I create a test set

**As a** tester, **I want** an empty Java class to appear when I make a test
set, **so that** the test cases I write next have somewhere to be written.

A test set becomes a Java class. The class is where its test methods go.

There is no key for this. It happens when a test set is created, which is
[UC-TREE-PANEL-007](../treePanel/createTestSet.md).

## Rules

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — Testin writes only the method's annotation and its
  declaration. The body is the tester's, and Testin never touches it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — What goes wrong while writing code goes to the log. The
  tester is not shown it.
- **Rule-CODEGEN-007** — The class is named after the test set, with everything
  but letters and digits removed, and always ends in `Test`.
- **Rule-CODEGEN-008** — Each folder above the test set becomes a package,
  keeping the capitals it already has.
- **Rule-CODEGEN-009** — A class that is already there is never written over.
- **Rule-CODEGEN-010** — The class is written empty. It holds no blank line
  inside its braces.
- **Rule-CODEGEN-011** — A test set whose name comes to nothing becomes a class
  called `DefaultTest`.

## What is written

```java
package demo.accounts;

public class LoginTest {
}
```

1. **The package line** — built from the folders above the test set.
2. **The class name** — the test set's name, cleaned, with `Test` on the end.

## What the tester sees

No screen opens, and Testin says nothing at all. The new Java file appears in
the Project tool window, inside the folder that holds the Java tests. Only a
problem speaks, as a small red message near the bottom right of the IDE, and
that message fades after about five seconds.

## Main flow

1. The tester creates a test set named **Login** under a package named
   **Accounts**.
2. Testin works out the class name and the package from the tree path.
3. Testin makes the package folder under the test source folder.
4. Testin writes the class file.

## What Testin refuses

**If the code project has no Java test source folder** — a message titled
**Java Test Source Not Found** reads *Unable to find a Java test source package
- automation code was not generated.* The test set is still created.

**If there is no name to build a class from** — nothing is written, and only the
log says so.

**If the class file is already there** — nothing is written and nothing is said.
The tester's own code is never overwritten.

**If the package folder cannot be made** — nothing is written, and only the log
says so.

**If the file cannot be written** — the same. Only the log says so.

**If the IDE has no Java plugin** — a message titled **Java Plugin Not
Available** appears once for the whole code project. Nothing is generated after
that, and nothing more is said.

## Where the plugin breaks its own rules

Two test sets whose names come to nothing when the special characters are
removed both write into one class called `DefaultTest`. That is difference 9 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
