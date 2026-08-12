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

### Ordering inside the indexer

The cache update (which may persist markers — and marker writes create
directories) must run **after** the VFS operation succeeds, never before.
Violating this creates phantom directories and "already exists in VFS" errors.

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
- Tests live under `src/test` only; the plugin distribution must never
  contain test classes or compile-time-only dependencies (Lombok is
  `compileOnly` + `annotationProcessor`, never `implementation`).
- Verify with `./gradlew compileJava test` before presenting changes; do not
  commit until Muteb has sandbox-tested (`./gradlew runIde`) and approved.
