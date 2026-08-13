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

**Exempt packages** (may access files directly): `git`, `importExport`, `logger`.

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
- Node behaviour is declared on the node: capability flags on `DirectoryDto`
  (`isRenamable`, `isTransferable`, `acceptsTransferred`, ...) instead of
  instanceof chains at call sites. Enums carry their own presentation and
  actions (see `TestStatus`, `TestRunStatus`).
- Dialogs are built on the declarative framework (`org.testin.ui.framework`):
  a dialog assigns `title`, `components`, `shortcuts` in its constructor and
  implements `submit()`. Never hand-build popup layouts.

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
