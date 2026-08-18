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

**Exempt packages** (may access files directly): `codegen`, `git`, `importexport`,
`report`, `setting`, `logger`.

What they have in common: none of them read or write **test data**. They handle
generated source, the Git working tree, files outside the tree, generated report
output, the IDE settings path, and the log. The rule exists to keep the indexer's
cache authoritative over test data, and none of these touch it.

`util` is deliberately not on the list. `FilesUtil` and `VfsExecutor` are the file
layer the indexer itself calls, not callers of it — whether that counts as inside
or outside the rule is still open, and tracked in issue #49.

In particular: **test runs are saved and read only through the indexer**
(`putTestRun`, `persistRun`, `persistRunMarker`, `addTestRunDir`,
`updateRunMarker`, run lookups). The sequential run writer lives inside the
indexer.

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
- **Packages are all-lowercase and named by feature, never by kind.** One word
  where one will do: `editor`, `explorer`, `report`, `creator`, `model`. A
  sub-package does not repeat its parent — `editor/run`, not `editor/runeditor`.
  There is no `enums`, `listeners` or `mappers` package: a type lives with the
  feature that owns it, and shared domain vocabulary lives in `model` beside the
  DTOs and markers that carry it (#53).
- **No `I` prefix on interfaces**, and no `Impl` suffix unless an interface of
  that exact name exists. A class is named for what it does — `VfsExecutor`, not
  `TreeUtilImpl`; `SaveOnProjectClose`, not `ProjectCloseListenerImpl`. Where the
  plain noun would collide with a platform type, the plugin prefixes with
  `Testin`: `TestinEditor` against `com.intellij.openapi.editor.Editor`, as
  `TestinFileSystem` and `TestinTabColorProvider` already do.
- **Renaming anything is a three-step check, not a find-and-replace.** Persisted
  keys read like code — `@State(name = "testin.settings.AppSettingsState")`,
  `"testin.pageSize"` — and log lines contain ordinary words like `Setting`, so
  rewrite the qualified form rather than the bare word, then diff every string
  literal in `src` before and after. On Windows a case-only package rename also
  needs `--no-build-cache`: Gradle restores the pre-rename casing and javac
  verifies classpath entries case-sensitively, which surfaces as a
  package-private class being invisible to a test in its own package.
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
  <p>
  **Which kind: can it finish while the tester is not looking?** `softShow` is a
  balloon on the status bar that fades and leaves no trace; `info` is a real IDE
  notification that stays in the Notifications log. Work that happens under the
  tester's hand takes the balloon — that is almost everything. Work that runs in
  the background and completes on its own time takes `info`, so a sync or a push
  that lands while they are reading a bug report is still there afterwards. Those
  keep a short title and one line of detail — `Synced` / "Up to date with the
  remote" — never a sentence and never an exclamation mark.
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
