[Documentation](../README.md) › Automation code and the gutter

# Automation code and the gutter

Testin writes a Java test method for every test case, and keeps it in step as
the tree changes. The marks in the gutter beside those methods lead back to the
test case they came from.

| | |
|---|---|
| **Part of Testin** | Automation code and the gutter |
| **Answers** | What Testin writes into the code, when it rewrites it, and what a tester sees when it cannot |
| **Numbering** | Use cases are `UC-CODEGEN-001` to `UC-CODEGEN-020`. Rules are `Rule-CODEGEN-001` to `Rule-CODEGEN-066` |
| **State** | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `779fe6b4`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

| | What the tester does | |
|---|---|---|
| | **Getting code written** | |
| **UC-CODEGEN-001** | [Get a class when I create a test set](getClassForTestSet.md) | |
| **UC-CODEGEN-002** | [Get a method when I create a test case](getMethodForTestCase.md) | |
| **UC-CODEGEN-003** | [Get a method for a test case that had none](getMissingMethod.md) | |
| **UC-CODEGEN-004** | [Get the whole subtree's code when I copy](copySubtreeCode.md) | |
| **UC-CODEGEN-005** | [Ask Testin to write the automation for me](automateTestCase.md) | |
| | **Moving between the two** | |
| **UC-CODEGEN-006** | [Go to the code from a test case](goToCode.md) | |
| **UC-CODEGEN-007** | [Go to the test case from the code](goToTestCase.md) | |
| | **Running it** | |
| **UC-CODEGEN-008** | [Run a test case's automation](runAutomation.md) | |
| **UC-CODEGEN-009** | [Stop a running test case](stopAutomation.md) | |
| | **Keeping the code in step** | |
| **UC-CODEGEN-010** | [Change a test case's description](renameTestCase.md) | |
| **UC-CODEGEN-011** | [Reorder the test cases in a test set](reorderTestCases.md) | |
| **UC-CODEGEN-012** | [Change a test case's groups](changeGroups.md) | |
| **UC-CODEGEN-013** | [Turn a test case off](disableTestCase.md) | |
| **UC-CODEGEN-014** | [Remove a test case](removeTestCase.md) | |
| **UC-CODEGEN-015** | [Rename a test set](renameTestSet.md) | |
| **UC-CODEGEN-016** | [Move a test set](moveTestSet.md) | |
| **UC-CODEGEN-017** | [Rename or move a package](renamePackage.md) | |
| **UC-CODEGEN-018** | [Remove a test set or a package](removeTestSet.md) | |
| | **When it cannot work** | |
| **UC-CODEGEN-019** | [Work in an IDE with no Java plugin](noJavaPlugin.md) | |
| **UC-CODEGEN-020** | [Work in a project with no Java test folder](noTestSourceFolder.md) | |

---

## What this part is for

A test case says what to do. A test method does it. Keeping the two in step by
hand is the work that makes teams give up on automation: a test case is renamed
and the method keeps the old name, a test case is removed and the method stays
forever.

So Testin writes the method, and rewrites it whenever the tree changes. The
tester never edits the parts Testin owns.

**Four words, before the rules use them.**

- The **test source folder** is the folder in the code project marked as holding
  Java tests. Testin writes everything under it.
- A **generated method** is the Java method Testin wrote for one test case.
- The **gutter** is the narrow strip down the left of a code editor, where the
  IDE draws its own marks.
- To be **in step** is for the code to say what the tree says. Testin's job here
  is keeping it that way.

---

## What Testin writes

For a test case, one method:

```java
@Test(description = "Log in with a valid user",
      testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",
      groups = {"REGRESSION", "SMOKE"},
      priority = 3)
public void logInWithAValidUser() {
    // TODO: Auto-generated test steps for logInWithAValidUser
}
```

1. **description** — the test case's description, exactly.
2. **testName** — the test case's identity. This is what ties the method to the
   test case, and it is the only part that must never be edited.
3. **groups** — the test case's groups. Written only when it has any.
4. **priority** — the test case's **position in its test set**, counting from
   one. It is not its High, Medium or Low. That is difference 1 below.
5. **The method name** — the description with everything but letters and digits
   removed, the first word lowercase and later words capitalized.
6. **The body** — one comment, and nothing else. The tester writes the rest.

For a test set, one class holding those methods. For a package, a folder.

---

## Rules that hold everywhere

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

---

## Every key

| Key | What it does | The page that owns it |
|---|---|---|
| `Shift+F5` | Goes to the generated method | [UC-CODEGEN-006](goToCode.md) |
| `F5` | Runs the selected test cases, or stops them | [UC-CODEGEN-008](runAutomation.md) |
| `Ctrl+F12` | **Automate Test Case**, which is not built | [UC-CODEGEN-005](automateTestCase.md) |

The gutter mark has no key. Stopping has no key of its own.

---

## Where the code lands

```
testin_example/
└── src/test/java/                     the test source folder
    └── demo/                          the test project
        └── accounts/                  a test set package
            └── LoginTest.java         a test set
                logInWithAValidUser()  a test case
```

1. **The test source folder** — found once per code project, and remembered.
2. **Each folder above the test set** — becomes a package.
3. **The test set** — becomes a class, whose name always ends in `Test`.
4. **Each test case** — becomes a method in that class.

The two fixed folders, test run packages and test runs generate nothing at all.

---

## Where the plugin breaks its own rules

Stated, not hidden. Each one is real and can be met today. None of them has a
bug report yet.

| | The rule it breaks | What a tester sees |
|---|---|---|
| **Difference 1** | Rule-CODEGEN-003 — what Testin writes means what it says | `priority` in the generated method is the test case's position in its test set, not its priority. A tester reading `priority = 3` in the code reads it as the test case's priority. Changing a test case's real priority writes nothing at all. |
| **Difference 2** | Rule-CODEGEN-002 — a menu entry does what it says | **Automate Test Case**, and `Ctrl+F12`, are live on every selected test case and always answer *Not built yet*. The one entry named after generating code is the one that does not. |
| **Difference 3** | Rule-CODEGEN-001 — one test case, one method | Two test cases whose descriptions differ only in punctuation share one method. The second gets none, cannot be run and cannot be jumped to. Nothing says so when it is created. |
| **Difference 4** | Rule-CODEGEN-006 — a tester can find out what happened | Clicking the gutter mark of a test case that was removed does nothing at all. Generated code outlives its test case, so this is the ordinary case. |
| **Difference 5** | Rule-CODEGEN-006 — one situation, one sentence | A test case with no method says *has no generated code yet* when it is run, and **Nothing to open** when it is jumped to. One state, two sentences, one keystroke apart. |
| **Difference 6** | Rule-CODEGEN-004 — the tree and the code agree | Moving a test set to a place Testin has not read leaves the class where it was. The tree and the code then disagree, and only the log says so. |
| **Difference 7** | Rule-CODEGEN-005 — a missing plugin is a skip | With TestNG but no Java plugin, **Run Test Case** is offered, every test case resolves to nothing, and the tester gets one *has no generated code yet* message per test case with no mention of the missing plugin. |
| **Difference 8** | Rule-CODEGEN-006 — a refusal names what happened | Removing a test set that sits outside a test cases folder says a class name could not be **built**, during an operation that was only going to delete one. |
| **Difference 9** | Rule-CODEGEN-003 — one name for one thing | Two test sets whose names come to nothing when the special characters are removed both write into one class called `DefaultTest`. |

---

## Not decided

**Question 1** — Should `priority` in the generated method carry the position or
the test case's priority? It carries the position on purpose, so a test run
executes in the tester's order. The attribute's name says otherwise.

**Question 2** — Should a test case whose method was skipped for a name clash be
told so at the moment it is created? Today the tester finds out at the first
`F5`.

**Question 3** — The gutter mark is drawn only for TestNG methods Testin wrote.
A tester who edits the identity loses the mark with nothing saying why.

---

[Documentation](../README.md) › **Automation code and the gutter**
