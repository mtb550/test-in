[Documentation](../README.md) › [The project panel](main.md) › UC-019

# UC-019: Switch the Git branch of the test project

**As a** tester, **I want** to switch the test project's branch from the panel,
**so that** the tree follows the branch I am testing.

## Rules

- **Rule 71** — The branch box appears only for a test project shared through
  Git.
- **Rule 72** — Switching with uncommitted changes asks first. Switching never
  loses them.
- **Rule 73** — A switch that succeeds reloads the tree from the new branch.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The Uncommitted Changes dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Uncommitted Changes                                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  3 changes in this test project are not committed.       (1) │
│  Switching does not leave them behind - they come with       │
│  you, and can be committed onto release by mistake.          │
│                                                              │
│  From:  main                                             (2) │
│  To:    release                                              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Switch Anyway   Shift+Enter Review Changes        (3) │
│  Escape Cancel                                               │
└──────────────────────────────────────────────────────────────┘
```

1. **How many changes** — and the one sentence that matters. Switching does
   not leave the changes behind. They come with the tester. They can be
   committed onto the wrong branch by mistake.
2. **From** and **To** — the branch the tester is on, and the branch they
   picked.
3. **Two confirms** — `Enter` switches anyway. `Shift+Enter` opens the review
   instead.

## Main flow

1. The test project is shared through Git, and its folder is a Git repository.
   A drop-down box above the tree shows its branches, with the current one
   selected.
2. The tester picks another branch.
3. Testin checks out the branch, reloads the tree from it, and shows *Switched to
   \<branch\>*.

**With uncommitted changes**

1. The box goes back to the branch the tester was on.
2. The **Uncommitted Changes** dialog says how many changes there are, and that
   they would move to the new branch too.
3. `Enter`, which is **Switch Anyway**, runs the switch as above.
4. `Shift+Enter`, which is **Review Changes**, opens the list of changes not yet
   committed instead.
5. `Escape` changes nothing.

## What Testin refuses

**If the test project is not shared through Git** — there is no box.

---

[Documentation](../README.md) › [The project panel](main.md)
