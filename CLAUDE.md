# Testin — instructions for AI assistants

## Architecture rules

### File access goes through the indexer — no exceptions in tree/UI code

No class may read, write, or execute operations on virtual files (VFS) or
physical files directly. The **indexer** (`org.testin.indexer`) is the single
owner of file access, so its cache objects stay authoritative and every read
is a fast in-memory lookup.

- Need to know whether a node exists? Ask the indexer's cache — never `Files.exists`.
- Need to create/move/rename/copy/delete? Call the indexer; it performs the VFS
  operation and updates its cache in the correct order (VFS first, cache after).
- UI code (tree, actions, dialogs, editors) holds `DirectoryDto`/`TestCaseDto`
  objects served by the indexer and never touches disk.

**Exempt packages** (may access files directly): `git`, `importexport`, `logger`.

In particular: **test runs are saved and read only through the indexer**
(`putTestRun`, `persistRun`, `persistRunMarker`, `addTestRunDir`,
`updateRunMarker`, run lookups). The sequential run writer lives inside the
indexer. Known debt tracked in issue #49: `DirectoryMapper` reads markers
from disk itself.

### Ordering inside the indexer

The cache update (which may persist markers — and marker writes create
directories) must run **after** the VFS operation succeeds, never before.
Violating this creates phantom directories and "already exists in VFS" errors.

### Threading — Swing only on the EDT

Swing components are read and written only on the EDT. Anything else that runs
during a UI action moves off it.

- Short work with no UI of its own (badge recomputes, filtering, sorting):
  `ApplicationManager.getApplication().executeOnPooledThread(...)`, finishing
  with `invokeLater` to touch Swing. No progress indicator.
- Long work the user should see and be able to cancel (indexing, Git, report
  generation): `Task.Backgroundable`, which gets an indicator and participates
  in cancellation.

If a pooled recompute is slow enough to want a progress bar, cache the result
instead of backgrounding it harder. Actions declare `ActionUpdateThread.EDT`
when their `update()` reads Swing state.

### Formatting is display-only

Rendering may reformat a value; saving never does. The stored JSON is always
byte-identical to what the tester typed. Editable surfaces — grid cells, editor
fields — load the **raw** value when editing begins, so formatted text can never
be committed back into storage.

## Code conventions

- The `Project` object is always named `p`: `final @NotNull Project p`.
- Abstract parent classes are named `Abstract*`; non-abstract parents `Base*`.
- `final` on parameters and locals wherever possible.
- Nullability: org.jetbrains `@NotNull`/`@Nullable` everywhere; Lombok
  `@NonNull` only on DTO/marker fields (it generates runtime checks there).
  Never jspecify.
- Node behavior is declared on the node: capability flags on `DirectoryDto`
  (`isRenamable`, `isTransferable`, `acceptsTransferred`, ...) instead of
  instanceof chains at call sites. Enums carry their own presentation and
  actions (see `TestStatus`, `TestRunStatus`).
- Dialogs are built on the declarative framework (`org.testin.ui.framework`):
  a dialog assigns `title`, `components`, `shortcuts` in its constructor and
  implements `submit()`. Never hand-build popup layouts.
- **Every state-changing action confirms itself** with one soft notification at
  the point it succeeded: `Services.getInstance(p, Notifier.class).softShow(p,
  "Copied")`. The message names the **outcome in the past tense**, one or two
  words, no trailing dots — `Copied`, `Pasted`, `Renamed`, `Removed`, `Passed`.
  Three words only where two things on the same screen could be meant: `Node
  copied` against `Test case copied`. A bulk operation notifies once with a
  count — `Removed 4`, never four balloons.
  <p>
  Not around `actionPerformed`: an action that returns early, opens a
  confirmation, or hands off to a background task would report a success that has
  not happened. Put the call after the work, inside whatever `try` could fail.
  Actions that only move the view — paging, escape, opening a details panel —
  stay silent, or the tester learns to ignore all of them (#62).
- **American English**, in comments and in text a tester reads. The platform API
  this is written against is American (`Color`, `EditorColors`, `normalize`), so
  British spellings put two dialects in one sentence — a comment about "the caret
  row colour" directly above `EditorColors.CARET_ROW_COLOR`. It is a convention
  rather than a cleanup because it does not stay fixed otherwise: #48 normalized
  28 of them, and the next few comments written put six back.
- A method handles its own failures: no `throws` on the signature. Catch inside,
  log through `Logger`, and notify when a tester action triggered it — a catch
  that does neither is worse than the `throws` it replaced. Two exceptions, both
  real contracts: implementing a platform interface that declares it
  (`NodesTransferable.getTransferData`, AWT `Transferable`) and functional
  interfaces whose whole point is to let a lambda throw (`GitTaskWork.run`).
  Both carry a comment saying why. Existing sites are tracked in issue #63.

## Process

- **GitHub is the source of truth**: stories/bugs live in `mtb550/test-in`
  issues, not local files. Muteb works across several machines.
- Write new stories straight to an issue with `gh`; update an existing one with
  `gh issue edit <n> --repo mtb550/test-in --body-file ...` instead of opening a
  duplicate. Read current state with `gh issue view` before assuming anything.
- Tests live under `src/test` only; the plugin distribution must never
  contain test classes or compile-time-only dependencies (Lombok is
  `compileOnly` + `annotationProcessor`, never `implementation`).
- Verify with `./gradlew compileJava test` before presenting changes; do not
  commit until Muteb has sandbox-tested (`./gradlew runIde`) and approved.
