[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-003

# UC-INTERNAL-003: Pick up a change made outside the IDE

**As a** tester, **I want** Testin to notice when my test data changes on disk
without me, **so that** what I am reading is what the files say and not what
they said when the IDE started.

There is no key for this. It starts on its own. `Ctrl+Alt+R` does the same job
by hand, and is on [UC-TREE-PANEL-025](../treePanel/refreshTree.md).

## Rules

- **Rule 15** — Testin watches every file the IDE watches. A change counts only
  if the file is inside a test project under the Testin folder.
- **Rule 16** — Anything inside a `.git` folder is ignored, however deep it is.
- **Rule 17** — The Testin folder itself is not watched. It holds test projects,
  and is not one.
- **Rule 18** — Testin ignores its own writes for five seconds after making
  them.
- **Rule 19** — Testin waits four tenths of a second after the last change
  before it reads. A pull that brings 40 files costs one read, not 40.
- **Rule 20** — The whole test project is read again, never the one file that
  changed.
- **Rule 21** — A change arriving while a read is running books the next read.
  Nothing is missed and nothing is read twice at once.
- **Rule 22** — A code project whose Testin panel was never opened is left
  alone.
- **Rule 23** — `testin.yml` is not watched. Only **Refresh** reads it again.

## What is picked up, and what is not

| The change | Picked up |
|---|---|
| A test case, a test set or a marker file edited by hand | Yes |
| Files a pull brought in | Yes |
| Files a branch switch changed | Yes |
| Forty files at once | Yes, as one read |
| Anything inside a `.git` folder | No |
| The Testin folder itself | No |
| Anything outside the Testin folder | No |
| Anything at all, when no Testin folder is set | No |
| What Testin itself just wrote | No, for five seconds |
| A code project whose panel was never opened | No |
| `testin.yml` | No |

## The screen

One progress bar, whatever changed and however many test projects it touched.

```
┌──────────────────────────────────────────────────────────────┐
│  Reading test data that changed on disk                [ X ] │
│  ██████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  25% │
│  Checkout - test sets...                                     │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — the same words every time. It does not name the file.
2. **The cancel button** — stops the read. The tree is still redrawn.
3. **The line underneath** — the same lines UC-INTERNAL-002 shows.

## Main flow

1. Something changes a file on disk. A pull, a branch switch, an editor outside
   the IDE, or another program.
2. Testin checks the path. It keeps the test project the file belongs to.
3. Testin waits four tenths of a second, in case more changes are coming.
4. The progress bar opens, reading *Reading test data that changed on disk*.
5. Each changed test project is read again, exactly as UC-INTERNAL-002 reads it.
6. The tree redraws. Every open editor reloads.
7. The bar closes. Nothing else is said.

## What Testin refuses

**If the change is Testin's own write** — it is ignored for five seconds. A
tester who edits a file by hand within five seconds of Testin saving it is
ignored too. **Refresh** recovers it.

**If the tester cancels** — the read stops, and the tree and the editors are
still redrawn. What was read is on screen. What was not is still the old
reading, and nothing marks which is which.

**If the code project's panel was never opened** — nothing happens at all. The
change is on disk and Testin has not read it. It is picked up the first time the
panel is opened.

**If `testin.yml` changed** — nothing happens. It lives in the code repository,
outside the Testin folder, so Testin never sees it change. **Refresh** reads it
again.

## Why it works this way

A tester was told a sync had succeeded, and then read a panel still showing what
the files said before it. Nobody presses **Refresh** after being told the work
is done. So Testin watches instead.

Testin ignores its own writes because it must. Without that, saving a test case
would rebuild the tree underneath the tester who saved it. That is correct, and
it is unusable.

---

[Documentation](../README.md) › [Inside Testin](main.md)
