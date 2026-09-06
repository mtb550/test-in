[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-018

# UC-SHARE-018: Answer which side wins for a field both changed

**As a** tester, **I want** to choose between my wording and a colleague's,
**so that** a merge does not silently throw away one of them.

There is no key that opens this. It opens during a merge.

## Rules

- **Rule 77** — Only the fields both sides really changed are asked about.
  Everything else is merged without a question.
- **Rule 78** — One window for each test case, holding one question for each
  field that disagrees.
- **Rule 79** — The tester's own value is chosen to start with.
- **Rule 80** — A value is shown on one line, cut at 70 characters, and an empty
  one reads as such.
- **Rule 81** — `Escape` answers nothing and stops the whole merge.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## The screen

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Both Changed Log in with a valid user                                     │
├────────────────────────────────────────────────────────────────────────────┤
│  Description    (x) Mine: Log in with a valid user                         │
│                 ( ) Remote: Sign in with a valid account                   │
│                                                                            │
│  Steps          (x) Mine: ["open the app", "sign in"]                      │
│                 ( ) Remote: (empty)                                        │
│                                                                            │
│                                              [ Keep Selected ]             │
├────────────────────────────────────────────────────────────────────────────┤
│  [k]  Enter Keep Selected       Escape Cancel                              │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The title** — the words **Both Changed**, then the test case's description.
2. **Each row** — the field, then the two values. **Mine** is chosen.
3. **Keep Selected** — writes the answers for this test case.

## Main flow

1. The merge finds a test case both sides changed.
2. The window opens with one row for each field that disagrees.
3. The tester reads both values and picks one for each row.
4. The tester presses `Enter`.
5. The merged test case is written, and the merge moves to the next one.

## What Testin refuses

**If the tester presses `Escape`** — nothing is written for this test case, and
nothing more is asked for the rest of the sync. Every answer already given is
thrown away, with no message. That is difference 3 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

**If a value is too long to show** — it is cut at 70 characters. The whole value
is still what gets written.

## The same window serves both syncs

This window is used for a Git pull and for a server sync. The two reach it
differently and it behaves the same in both.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
