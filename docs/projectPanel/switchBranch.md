[Documentation](../README.md) › [The project panel](main.md) › UC-026

# UC-026: Switch the Git branch of the test project

> **No key.** Pick a branch from the box above the tree.

**As a** tester, **I want** to switch the test project's branch from the panel,
**so that** the tree follows the branch I am testing.

## Rules

- **Rule 71** — The branch box appears only when the test project's own project
  file says it is shared through Git **and** its folder really is a Git
  repository. A Git folder whose project file does not say so has no box.
- **Rule 72** — Switching with uncommitted changes asks first. Switching never
  loses them.
- **Rule 73** — A switch that succeeds does a full refresh: it re-reads which
  test project is bound, reads the test project again, reloads every open Testin
  editor, and **closes any editor whose node the new branch does not have**.

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
   A drop-down box above the tree lists its branches, alphabetically, with the
   current one selected. Remote branches are listed too, without their
   `remotes/` prefix.
2. The tester picks another branch. A progress bar reads *Checking branch
   \<branch\>*, then *Checking out branch: \<branch\>*.
3. Testin checks out the branch. Picking a remote branch that has no local
   branch yet creates one that follows it, and the message then names the local
   branch.
4. Testin does a full refresh, and shows *Switched to \<branch\>*. An editor
   whose node the new branch does not have is closed.

The box fills itself twice when the panel opens: once from what is on the
machine, showing *Reading branches*, then again after fetching from the remote,
showing *Fetching from remote*. So the list can grow a moment after it appears.

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

**If Git refuses the checkout** — the box goes back to the branch the tester was
on, and an IDE notification titled *Branch Not Switched* says *\<branch\> was not
checked out. There are uncommitted changes in this test project that switching
would overwrite - commit them first.* It carries a **Review Changes** link.

**If the branches cannot be read** — the box reads *Failed to load branches* and
cannot be opened. An IDE notification titled *Git Error* gives the reason.

**If the repository has no branches** — the box reads *No branches found* and
cannot be opened.

**While the branches are still loading** — the box reads *Loading branches...*
and cannot be opened.

---

[Documentation](../README.md) › [The project panel](main.md)
