[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-007

# UC-CODEGEN-007: See the test case from the code

**As a** tester, **I want** to see the test case behind a method I am looking at,
**so that** I can read what the automation is supposed to prove without leaving
the code I am reading.

The view panel opens on that test case, showing its details. Nothing else moves:
the tree stays where it was, no editor opens, and the caret stays on the method.

Going to the test case is one more click, and it is
[the identity in that panel](../viewPanel/copyIdentity.md).

There is no key for this. The mark is in the gutter beside the method.

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
- **Rule-CODEGEN-028** — The mark is drawn beside the identity inside the
  method's annotation, on the right of the gutter.
- **Rule-CODEGEN-029** — The mark is drawn only for a TestNG test whose identity
  Testin recognizes.
- **Rule-CODEGEN-030** — The mark is drawn while the IDE is still indexing, and
  the jump waits for indexing to finish.
- **Rule-CODEGEN-070** — The mark opens the view panel on the test case and
  stops there. It moves nothing else: not the tree, not the editor tabs, not the
  caret. A tester reading a method asked what it proves, and taking them out of
  the code to answer is a bigger answer than the question.
- **Rule-CODEGEN-069** — Clicking the mark of a method whose test case is gone
  says so, naming the method. Generated code outlives the test case it was
  written from, so this is an ordinary answer rather than a failure.

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│     40      @Test(description = "Log in with a valid user",                │
│  [*] 41           testName = "3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b",       │
│     42           priority = 3)                                             │
│     43      public void logInWithAValidUser() {                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The mark** — on the line holding the identity, not the line holding the
   method name. Its tooltip reads **View Test Case Details**.

## Main flow

1. The tester is reading a generated method.
2. The tester clicks the mark beside the identity.
3. Testin waits for indexing, then looks the test case up by its identity.
4. The view panel opens on that test case, with the Details tab in front.
5. Nothing else moves. The tester is still in the code, on the same line.

To go to the test case itself, click its identity in the panel that just
opened — its test set opens in an editor with the row selected, and the tree
still does not move (Rule-VIEW-PANEL-063).

## What Testin refuses

**If the identity is not one Testin recognizes** — no mark is drawn at all.

**If the method is not a TestNG test** — no mark is drawn.

**If the test case behind the identity no longer exists** — nothing opens, and
the message names the method: *logInWithAValidUser was generated from a test
case that is gone*. It fades (Rule-CODEGEN-069).

**If anything else fails** — a message titled **Error** reads *Could not find
test case:* and then the reason.

**If the IDE has no Java plugin** — no mark is drawn anywhere.

## Where the plugin breaks its own rules

**A tester who edits the identity loses the mark**, with nothing saying why.
That is question 3.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
