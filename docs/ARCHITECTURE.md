[Documentation](README.md) › How Testin is put together

# How Testin is put together

The plugin is 512 classes in 32 top-level packages. This page is the map: which
packages are layers and which are side modules, the four rules the whole thing
is built on, and two operations traced class by class — because everything else
is a variation on one of them.

Read this before your first change. It replaces reading 32 packages to find out
where anything lives. It does **not** describe what Testin does for a tester —
that is [the documentation](README.md) — and it does not describe any one
package in detail.

---

## The shape

```
                       the tester
                            |
   +------------------------+------------------------+
   |            |           |          |             |
explorer          editor           view          lightmode      the surfaces
  (tree)         (test/run)       (details)       (read-only)
   |            |           |          |             |
   +------------------------+------------------------+
                            |
              actions, ui.framework                             how a surface
              creator, clipboard, undo, search,                 asks for things
              navigate, open, order, rename, remove
                            |
              testcase  testproject  testrun                    the operations
                            |
                        services                                who to ask
             (Services, Notifier, settings, testin.yml)
                            |
                        indexer                                 the only door
             ProjectIndexer -> IndexerDataStore                 to test data
             -> TestCaseSequenceStore -> TestDataFiles
                            |
                       VFS / disk

   model  ..... the vocabulary every layer above speaks
   logger ..... written to by all of them, imports none of them

   side modules, each on the indexer and four pairs on each other:
   codegen   git   sftp   report   importexport   runner
```

Two content modules sit outside this entirely, loaded only where their platform
plugin is: `testin-java` writes and reconciles the generated Java, and
`testin-testng` starts a TestNG run. The core declares extension points and never
learns whether anything answered — see [The two content
modules](#the-two-content-modules).

### The layers, and what each is allowed to do

| Layer | Packages | May touch test data files |
|---|---|---|
| Surfaces | `explorer`, `editor`, `view`, `lightmode` | No |
| Gestures | `actions`, `ui`, `creator`, `clipboard`, `undo`, `search`, `navigate`, `open`, `order`, `rename`, `remove` | No |
| Operations | `testcase`, `testproject`, `testrun` | No |
| Services | `services`, `notifications`, `setting`, `config` | `config` and `setting` only, and neither touches test data |
| Data | `indexer`, `model` | `indexer` only |
| Side modules | `codegen`, `git`, `sftp`, `report`, `importexport`, `runner` | See the exempt list below |
| Leaves | `logger`, `util` | `logger` only, and only its own log |

**Four names left this table on 11 September 2026, and the root package emptied.**
`dialogs` held one class whose only caller is in `ui`, one letter away from
`ui.dialogs` - two names that near each other are a coin toss rather than a
choice. `automate` held one action, and automating a test case *is* generating
its code, which is what `codegen` is. Neither move crossed a layer: each went to
a package in the row it was already in.

`EscapeAction` and `ShowNodeDetailsAction` were in `org.testin` itself, which is
in no row of this table at all. They are in `actions` and `view.marker` now -
where the first already extends `AbstractProjectAction`, and the second opens
`MarkerDetailsViewDialog` and does nothing else.

`testset` held one action and the group that built it, both of them a copy of
the package pair two directories away and of the test project pair one
directory further - three classes setting a status, differing in the name of a
DTO and in nothing else. There is one now, in `explorer.tree` where the menu
that offers it lives: a marker says which statuses its node has and applies the
one it is given, so the tester sees the statuses of whatever they right-clicked
and a fourth status anywhere appears with nothing else to change.

`run` merged into `runner`, which is the documented pair #110 asked for. They
were one concern split over two names - `run` started an execution and `runner`
watched it - and a side module owning the actions that invoke it is what `git`
and `importexport` already do. What is left is a pair that cannot be confused:
`runner` executes test cases, `testrun` is the test run a tester creates, names
and records verdicts into. Merging those two would lose the distinction, which
is why the triple folded to two rather than to one.

`statusbar` left the table on the same day, and it was never the surface this
picture drew. Nothing in Testin adds a widget to the IDE's status bar; what the
package held was the shortcut-hint strip along the bottom of a dialog, plus the
two interfaces a row on one implements. The strip is `ui/framework/StatusBarBase`
now, beside `StatusBarShortcut`, which is what fills it; the interfaces are in
`model`, beside the four enums that implement them (#111).

**The packages that are small and staying that way** are small because this table
says so. `open`, `order` and `remove` hold two files, one and one; they are
Gestures, and the feature each acts on is a Surface. Merging a gesture into the
surface it acts on is this table inverted, and a short package is a smaller price
than a layer that is drawn here and not in the tree (#110).

`logger` imports nothing from the plugin and is imported by 30 packages. `util`
imports only `logger` and `model`, so reaching for a helper can never drag an
editor into the classpath (#112). Keep both that way: a helper that needs
`editor`, `view`, `ui`, `services` or `notifications` is feature glue, and it
belongs beside the feature.

### Where the graph is not a tree

Not every import points down that picture. Three shapes account for almost all
of the ones that do not, and all three are deliberate, so they are described
here rather than listed one by one:

- **An action holds the surface it acts on**, and a feature owns the actions
  that invoke it. `git/SyncActionAction` reaching `explorer` is a gesture
  reaching its surface, filed under `git` because that is where sharing lives.
- **A dialog reaches the dialog framework.** `ui/framework` is drawn in the
  gestures row and is infrastructure: everything that opens a dialog reaches it,
  which is what it is for.
- **`testcase` and `testrun` hold vocabulary as well as operations.** #111 moved
  `TestEditorAttributes` and `RunEditorAttributes` out of `model` into the
  packages that own those fields, so a feature asking what a test case's columns
  are now reaches the operations row to ask. `importexport` does it 30 times,
  `report` 8, `git` twice. That is what the move cost, and it is not a package
  reaching up to *do* anything.

What is left is below. **There is no count here on purpose.** The sentence that
stood here said eleven, defined as "a package importing one strictly above it in
the table" - and by that definition the real number is about ninety, because the
three shapes above are everywhere. A number nothing measures is a number that
goes stale the same week (#66, findings 61 and 77).

| From | To | Why |
|---|---|---|
| `indexer/ProjectIndexer` | `editor/LastOpenEditors` | Indexing finishes, and the editors the tester had open are reopened. |
| `indexer/Rescan` | `explorer/TreePanel`, `editor/TestinEditors` | A rescan has to tell the open surfaces that what they are showing has changed. The alternative is a listener the indexer publishes to, and the reason it has not been done is below this table. |
| `setting/SettingsConfigurable` | `explorer/TreePanel` | Applying the settings page rebuilds the tree, because the Testin root it names is what the tree is built from. Not an action, so it is not the first shape above. |
| `codegen/AutomationState` | `navigate/CodeNavigation` | Whether a case has automation behind it is answered by resolving the generated method, and resolving is what `navigate` knows how to do. |
| `codegen/ExecutionPosition` | `testcase/TestCaseOrder` | The number a generated method carries is the case's place in its set, and the set's order is `testcase`'s answer. The third shape above, in one import. |
| `actions/TestinData`, `actions/Declared` | `editor`, `model`, `util`, `logger` | Deliberate, and new with #119. A declared action is built by the platform with a no-arg constructor, so it asks the surface that has the keyboard what is selected - and a data key has to name the type it answers with. `actions` was a leaf until then, and typing the keys as `Object` to keep it one would be worse than the edge. |
| `testcase/TestEditorAttributes`, `testrun/RunEditorAttributes` | `ui/Badges` | Deliberate. An enum carries its own presentation and its own action rather than being read by an `instanceof` chain at every call site — see the conventions in [CLAUDE.md](https://github.com/mtb550/test-in/blob/main/CLAUDE.md). What is new is where it points *from*: these two were in `model` until 11 September 2026, so the vocabulary every layer speaks pulled the badge painter in behind it (#111). A field of a test case is a fact about `testcase`. What is left is one import each, for the badge a card draws; the other four - `codegen`, `importexport`, `notifications`, `indexer` - are a feature calling a side module and a service, which points down. |

**Side modules are not quite "none on each other".** The drawing above says they
are, and four pairs say otherwise:

| | |
|---|---|
| `sftp` -> `git` | Both merge an incoming test case against the local one, and `git/TestCaseMerge` is the one that knows how. A copy in `sftp` would be a second answer to "what does a conflict look like", which is the divergence the merge dialog exists to prevent. |
| `report` -> `importexport` | The report dialog offers four formats and `importexport/FileTypes` is where a format's extension lives. |
| `importexport` -> `report` | And back: `FileTypes` names the four report generators, one per format. The pair is a cycle, and it is the only one between side modules. |
| `importexport` -> `codegen` | An import creates test cases, and a created case gets its generated method like any other. |

**Why the rescan still calls the surfaces.** `TestCaseExecutionListener` is the
shape that would fix it - a topic the runner publishes to, with no idea who is
listening - and the indexer saying "this path changed" is the same statement. It
is not a rename, though. `Rescan` asks `Services.isNotCreated(p, TreePanel.class)`
before it does anything at all, and that question decides whether the project is
scanned, not only whether anything is redrawn: a project that never opened the
Testin tool window must not be indexed behind the tester's back (#77). Moving the
notification to a topic leaves that decision with nothing to ask, so the move is
a new question for the indexer to answer about itself plus two subscribers plus
the ordering - record first, tell the surfaces second - and it is worth doing on
its own rather than inside a sweep (#66, finding 60).

What is **not** there: **`model` imports nothing above it at all** - not
`editor`, not `explorer`, not `view`, and since 11 September not `indexer`,
`codegen`, `creator`, `services`, `notifications`, `importexport`, `ui` or
`statusbar` either. The leaf rule is at zero and `ArchitectureTest` no longer
carries a single exception to it (#111).

Four of the six that had to move were tables: an enum naming, per node kind,
what some feature does for it. Each is an enum of its own now, in the package
that knows the answer, with one constant per kind **named after it** -
`creator/NodeCreators`, `codegen/JavaCode`, `remove/Removals`,
`NodeCounter.Gathered`. The bridge is `valueOf(type.name())`, so nothing
branches on a node kind and no call site asks what it is holding; what the
enum constructor used to guarantee - that a new kind supplies every column -
`NodeKindTablesTest` guarantees instead, by name.

---

## The four rules

### 1. All test data file access goes through the indexer

`org.testin.indexer` is the single owner of reading and writing test data. Its
cache is therefore authoritative, and every read a surface makes is an in-memory
lookup rather than a disk hit.

- Need to know whether a node exists? Ask the indexer's cache — never
  `Files.exists`.
- Need to create, move, rename, copy or delete? Call the indexer. It performs the
  VFS operation and updates the cache, in that order.
- UI code holds `DirectoryDto` and `TestCaseDto` objects the indexer served, and
  never touches disk.

Test runs in particular are saved and read only through the indexer —
`putTestRun`, `persistRun`, `persistRunMarker`, `addTestRunDir`,
`updateRunMarker`. The sequential run writer lives inside it.

The rule is enforced by the compiler rather than by review: `TestDataFiles` and
`VfsExecutor` are package-private and live in `indexer`, so nothing outside the
package can reach the writer at all.

**The exempt packages**, which may open files directly: `codegen`, `config`,
`git`, `importexport`, `report`, `setting`, `logger`.

What they have in common is that none of them read or write **test data**. They
handle generated source, the automation repository's own `testin.yml`, the Git
working tree, files outside the tree, generated report output, the IDE settings
path, and the log. `config` in particular reads a file that lives in the
automation repository rather than under the Testin root, and it runs before the
indexer exists — it is what tells the indexer which project to index.

One package not on that list opens a file anyway, and it is worth knowing why
before you grep and think you have found a violation. `sftp/BaselineStore` reads
and writes one gzipped document under `PathManager.getSystemPath()`, recording
what this machine last agreed with the server. It is not test data, it must never
be committed, and it belongs to this machine's copy of the project rather than to
the project. Every test file SFTP actually syncs goes through
`ProjectIndexer.filesUnder`, `acceptIncoming` and `removeIncoming`, exactly as
Git does.

### 2. The VFS operation succeeds first, then the cache is updated

Never the other way round. The cache update may persist markers, and a marker
write creates directories — so a cache update that runs first produces phantom
directories and "already exists in VFS" errors.

`ProjectIndexer.moveNode` is the shape to copy: `VfsExecutor.executeVfsAction`
performs the move and takes two callbacks, and `store.renameNode` is called from
the success one.

### 3. Swing is read and written only on the EDT

Anything else that runs during a UI action moves off it.

| Work | How | Gets an indicator |
|---|---|---|
| Short, no UI of its own — badge recomputes, filtering, sorting | `ApplicationManager.getApplication().executeOnPooledThread(...)`, finishing with `invokeLater` to touch Swing | No |
| Long, and the tester should be able to cancel it — indexing, Git, report generation | `Task.Backgroundable` | Yes, and it participates in cancellation |

If a pooled recompute is slow enough to want a progress bar, cache the result
instead of backgrounding it harder. Actions declare `ActionUpdateThread.EDT` when
their `update()` reads Swing state.

### 4. Formatting is display-only

Rendering may reformat a value. Saving never does. The stored JSON is always
byte-identical to what the tester typed.

Two methods on `model/TestEditorAttributes` are the whole rule:

- `gridValue(tc)` — the raw value, for anything typed into.
- `displayValue(tc)` — the same value with a sentence made of it where the tester
  wrote prose, for anything only read.

A grid cell and an editor field load `gridValue`, so a period `displayValue`
would have added is never committed back into the JSON the first time a tester
opens a cell they had not changed. That is #22, and it is why the two methods are
not one.

---

## Walkthrough one: a test case is saved

A tester edits a cell in the grid and presses Enter. Ten steps later the JSON on
disk is either changed or byte-identical, and which one it is decides whether
anything else happens at all.

| # | Where | What happens |
|---|---|---|
| 1 | `editor/listeners/GridEditListener` | Reads what was typed, parses it for the column's type, and compares it to what the case already held. **Unchanged, and it stops here.** |
| 2 | `editor/listeners/GridEditListener.persistAndGenerate` | Moves off the EDT with `executeOnPooledThread`, because the codegen in step 10 schedules its own write commands. |
| 3 | `indexer/ProjectIndexer.putTestCase` | The public door. Returns a boolean: did this have anything to save. |
| 4 | `indexer/IndexerDataStore.putTestCase` | Delegates the write, then stamps the **set's** marker as modified — but only if the write happened. |
| 5 | `indexer/TestCaseSequenceStore.put` | The funnel every save arrives at: the update dialog, a grid cell, the details panel, a paste. |
| 6 | `indexer/TestDataFiles.alreadyHolds` | Serializes the case and compares the bytes to the file. **Identical, and nothing below runs.** |
| 7 | `indexer/TestCaseSequenceStore.put` | Stamps the audit — `touch` if the index already knows this id, `stampCreated` if it does not. |
| 8 | `indexer/TestCaseSequenceStore.store` | Updates the two maps, then writes. |
| 9 | `indexer/TestDataFiles.write` | Refuses a zero-byte write, claims the path in `OwnWrites` **before** `Files.write`, writes, then records what landed. |
| 10 | back in `GridEditListener` | The attribute's `GenType` regenerates the test method, and `TestCaseSnapshot.record` files the undo entry. |

**Why the bytes are identical.** Step 6 asks the question the rule states, in the
rule's own terms: would this write leave the file the same. It has to be asked as
bytes, and it has to be asked *before* step 7, because the stamp is itself a
change — `touch()` writes a new `updatedAt`, and anything compared after it
differs by the one field the check exists to avoid writing. It also cannot be
asked in memory: the index hands out its own objects and the dialogs edit them in
place, so by the time a save arrives the indexed case and the case being saved
are the same object, and the file is the only record of what it looked like
before. Opening a field to read it and pressing Enter used to record the tester as
having edited the case (#164).

Two things follow from step 6 answering true. Nothing is written, so a tester's
next commit does not contain a file they never changed; and step 4 does not stamp
the set either, because a save that changed nothing did not modify the set, and
stamping it would move the lie one level up.

**Step 9's ordering is a bug fix, not a preference.** The VFS event can arrive
while the thread is still inside `Files.write`, and a file claimed a moment too
late looks to the watcher like somebody else's edit (#20). Claim first, record
what landed afterwards, so an edit the tester makes inside that window is told
from this write rather than swallowed with it (#278).

There is one deliberate bypass. `putTestCaseVerbatim` stores without stamping: an
import writes the audit the imported file carries, and an undo writes the audit
the case had before the change being taken back — stamping either would record
the tester as having modified a case at the moment they un-modified it (#164,
#165).

## Walkthrough two: a test set is run

The tester right-clicks a test set and picks Run Tests. The plugin does not know
how to run anything; a content module does.

| # | Where | What happens |
|---|---|---|
| 1 | `runner/RunTestsAction` | Asks the selected node which gesture this is. A node that *holds* cases hands them straight to the runner; a **test run** is opened in its editor first, because a verdict reaches a named run only through the editor that claimed the case. |
| 2 | `runner/RunTestCases.run` | Refuses if the TestNG plugin is absent, drops the cases already running, and starts what is left **as one run** — one compile and one JVM, not twelve. Notifies once, with a count. |
| 3 | `runner/TestRunner.available()` | The extension point `org.testin.testRunners`. Empty in an IDE where nothing can run — an answer, not a missing one. |
| 4 | `testin-testng/TestNGRunner.run` | Finds each case's generated method as a `PsiClass`, reports the ones with no generated code, and builds a `TestNGConfiguration` whose pattern set names class and method. |
| 5 | `runner/TestNGExecution.launch` | Hands the configuration to the platform and remembers which cases this environment covers, so a stop knows what to put back. |
| 6 | `runner/TestNGExecution.starting` | Broadcasts each case as running before the process exists, so the cards change at the click rather than at the first report. |
| 7 | the platform | Compiles, starts a JVM, runs TestNG. |
| 8 | `runner/TestCaseExecutionTracker` | Subscribed to `SMTRunnerEventsListener.TEST_STATUS` for the life of the project. Turns each started and finished event into a `TestCaseExecutionListener.broadcast`. |
| 9 | `runner/TestCaseExecutionSubscriber.record` | On the EDT. The test name **is** the case's id, because that is what Testin named the generated method — so there is nothing to look up. A name that is not an id is nobody's, and is logged. |
| 10 | `runner/TestCaseExecutionSubscriber.report` | Decides what the report means: a case the tester stopped reports itself failed, and that is not a failure. Records the verdict against the **id**, then tells the surfaces. |
| 11 | `editor/test/TestEditor`, `editor/run/RunEditor`, `view/ViewPanel` | Repaint. |

**One recorder per project, not one per surface.** Step 10 runs whether or not
anything is open. Each surface used to subscribe and record for itself, so the
model was updated once per open surface and not at all when none was open —
running a test set from the tree in a session where no editor had been opened
stored no verdict at all, and every card afterwards showed no result for a run
that had passed (#66).

The order in step 10 is guaranteed rather than hoped for. A surface asked to
redraw reads what is running from `TestNGExecution`, so it must not be told before
`TestNGExecution` has been. One subscriber that records and then calls the
surfaces gives that; two independent subscriptions would have run in whichever
order the message bus chose.

**The verdict is recorded against the id, not the object.** The `TestCaseDto` in
hand is replaced by the next rescan, and a verdict that lived on it went with it —
which is how a case that had just passed lost its badge at the tester's next
keystroke (#116).

---

## The two content modules

The core plugin runs in every IDE. Anything that needs another plugin's classes
lives in a content module, which the platform loads only where that plugin is
present.

| Module | Needs | Contributes |
|---|---|---|
| `testin-java` | the Java plugin | Writing, moving, renaming and reconciling the generated test classes and methods, and the gutter mark beside a generated method |
| `testin-testng` | the TestNG plugin | `TestNGRunner`, the one implementation of `runner/TestRunner` |

The core declares the extension point and never learns whether anything answered:
`TestRunner.available()` returns a runner that logs and starts nothing when the
list is empty. A module running a different framework on a different IDE
contributes to the same point, and nothing in the core changes.

---

## What this page does not cover

| | |
|---|---|
| What Testin does for a tester | [the documentation](README.md) — 152 use cases, every rule numbered |
| Why a design that looks wrong is that way | [Standing decisions](decisions.md) |
| Every file Testin writes, field by field | [The formats on disk](formats.md) |
| Setup, the checks, and how to contribute | [CONTRIBUTING.md](https://github.com/mtb550/test-in/blob/main/CONTRIBUTING.md) |
| Naming, nullability and the conventions a change is reviewed against | [CLAUDE.md](https://github.com/mtb550/test-in/blob/main/CLAUDE.md) |
| What one package does | its own classes — the javadoc is the authority there |
