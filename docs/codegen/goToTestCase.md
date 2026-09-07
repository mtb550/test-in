[Documentation](../README.md) › [Automation code and the gutter](main.md) › UC-CODEGEN-007

# UC-CODEGEN-007: Go to the test case from the code

**As a** tester, **I want** to read the test case behind a method I am looking
at, **so that** I know what the automation is supposed to prove before I change
it.

This opens the test case that a generated method was written from.

There is no key for this. The mark is in the gutter beside the method.

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
- **Rule-CODEGEN-028** — The mark is drawn beside the identity inside the
  method's annotation, on the right of the gutter.
- **Rule-CODEGEN-029** — The mark is drawn only for a TestNG test whose identity
  Testin recognizes.
- **Rule-CODEGEN-030** — The mark is drawn while the IDE is still indexing, and
  the jump waits for indexing to finish.

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
2. The tester clicks the mark in the gutter.
3. Testin waits for indexing, then looks the test case up by its identity.
4. The view panel opens on that test case, with the Details tab in front.

## What Testin refuses

**If the identity is not one Testin recognizes** — no mark is drawn at all.

**If the method is not a TestNG test** — no mark is drawn.

**If the test case behind the identity no longer exists** — nothing opens and
nothing is said. Only the log records it.

**If anything else fails** — a message titled **Error** reads *Could not find
test case:* and then the reason.

**If the IDE has no Java plugin** — no mark is drawn anywhere.

## Where the plugin breaks its own rules

**Clicking the mark of a removed test case does nothing.** Generated code
outlives the test case it came from. So this is an ordinary state, not a rare
one. The tester clicks, and nothing happens. That is difference 4 on
[the automation code page](main.md#where-the-plugin-breaks-its-own-rules).

**A tester who edits the identity loses the mark**, with nothing saying why.
That is question 3.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
