[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-022

# UC-SHARE-022: Agree to remove files the server no longer holds

**As a** tester, **I want** to be asked before a sync deletes test cases from my
machine, **so that** somebody else's removal does not take my work with it.

There is no key for this. The question comes after the sync.

## Rules

- **Rule 96** — A sync never deletes anything on its own. It asks.
- **Rule 97** — Only files this machine has not touched since are offered for
  removal.
- **Rule 98** — Saying no offers the same choice again on the next sync.

Rules 1 to 6 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Removed On The Server                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  3 files here were deleted on the server by somebody else,   │
│  and this machine has not touched them since: Test Cases/    │
│  Login/a.json, Test Cases/Login/b.json, Test Cases/Login/    │
│  c.json. Removing them here agrees with that. Keeping them   │
│  offers the same choice again on the next sync.              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Remove 3 files       Escape Cancel               │
└──────────────────────────────────────────────────────────────┘
```

1. **The message** — the count, then every file by name.
2. **The confirm word** — **Remove**, then how many files.

## Main flow

1. A colleague deleted three test cases and synced.
2. The tester syncs.
3. Testin sees the three are gone from the server and untouched here.
4. The window opens, naming all three.
5. The tester presses `Enter`.
6. The three are removed from this machine.
7. A message reads *Removed 3*.

## What Testin refuses

**If the tester presses `Escape`** — nothing is removed. The same question comes
again on the next sync.

**If this machine has changed one of them since** — it is not offered. It
becomes a conflict instead, and the tester is asked which version wins. That is
[UC-SHARE-021](answerServerConflicts.md).

## Why the files are named

Three file names in a message is long, and it is the point. A tester agreeing to
delete test cases should be able to see which ones without opening anything
else.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
