# Testin

Test case management inside the IDE, next to the automation it drives.

Testin adds a tool window where you organise test projects, test sets and test
runs, write test cases, execute them, and generate reports — without leaving
IntelliJ IDEA and without a second tool to keep in sync. Test cases are stored
as JSON on disk in your automation repository, so they are reviewed, branched
and merged like the code they test.

<!-- TODO: screenshot of the project panel with a test editor open, saved under docs/ -->

## What it does

- **Organise** — test projects hold test sets and test runs, in a tree beside
  your code. Create, rename, move, copy and delete nodes with the keyboard.
- **Write** — a test case has a description, expected result, module,
  pre-conditions, steps, test data, priority and groups. The editor offers list
  and grid views, paging, filtering, search and bulk edits, with completion
  drawn from what you have already written.
- **Generate** — Testin writes and maintains the TestNG classes and `@Test`
  methods for your cases, and keeps them in step when you rename or remove one.
- **Execute** — run a case, a set, or a whole test run from the gutter, the tree
  or the editor. Results are recorded per case with status, duration and
  failure details.
- **Report** — export a run to PDF, Word, Excel or HTML, and import or export
  test cases as XLSX, CSV or JSON.
- **Version** — commit and sync test case changes from inside the panel, with a
  diff of what changed in each case.

## How the data is stored

Everything Testin owns is plain files under the Testin root you configure:

| Marker | Node |
|---|---|
| `.tp` | Test project |
| `.tcd` / `.trd` | The Test Cases and Test Runs containers |
| `.tsp` / `.ts` | Test set package, test set |
| `.trp` / `.tr` | Test run package, test run |

Test cases are JSON, one file each. **Stored values are byte-identical to what
you typed** — Testin formats for display only, never on save — so a diff shows
the change you made and nothing else.

## Requirements

- IntelliJ IDEA **2026.1** or later (build 261+)
- **Java 21** toolchain
- **TestNG** for execution, and the Java plugin for code generation

The Java, TestNG and Git integrations are optional dependencies: Testin loads
without them and disables the features that need them rather than failing.

## Install

From the IDE: **Settings → Plugins → Marketplace**, search for *Testin*, install
and restart.

<!-- TODO: Marketplace listing URL -->

Then set the Testin root in **Settings → Tools → Testin** — the folder in your
automation repository where test cases will live — and create your first test
project from the panel. Testin's settings are per-IDE, not per-project: the root
you choose is the one every open project uses.

## Building from source

```bash
./gradlew build          # compile, run tests, build the plugin
./gradlew runIde         # launch a sandbox IDE with the plugin installed
pwsh tools/inspect.ps1   # run the IntelliJ inspections headlessly
```

The sandbox writes to `.sandbox/`; the inspection tooling writes to
`.inspection/`, deliberately outside `build/` so `./gradlew clean` does not
delete the findings list. Neither is committed.

## Contributing

Issues and plans live in [GitHub issues](https://github.com/mtb550/test-in/issues).
The architecture rules — indexer-owned file access, Swing on the EDT,
display-only formatting — are in `CLAUDE.md`, which is the single authority for
them.
