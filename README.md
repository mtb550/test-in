# Testin

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/31514-testin?label=marketplace)](https://plugins.jetbrains.com/plugin/31514-testin)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/31514-testin)](https://plugins.jetbrains.com/plugin/31514-testin)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/31514-testin)](https://plugins.jetbrains.com/plugin/31514-testin)

Test case management inside the IDE, next to the automation it drives.

Testin adds a tool window where you organize test projects, test sets and test
runs, write test cases, execute them, and generate reports — without leaving
IntelliJ IDEA and without a second tool to keep in sync. Test cases are stored
as JSON on disk in a Git repository of their own, so they are reviewed, branched
and merged like the code they test — without mixing into the automation project
they exercise.

## What it does

- **Organize** — test projects hold test sets and test runs, in a tree beside
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

Or install it from the listing: **[Testin on the JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31514-testin)**.

Then set the Testin root in **Settings → Tools → Testin** — the folder where
your test cases will live — and create your first test project from the panel.
Each test project under that root is its own Git repository, kept separate from
the automation project it tests, so test data and code have their own histories.
Testin's settings are per-IDE, not per-project: the root you choose is the one
every open project uses.

## Building from source

```bash
./gradlew build          # compile, run tests, build the plugin
./gradlew runIde         # launch a sandbox IDE with the plugin installed
pwsh tools/inspect.ps1   # run the IntelliJ inspections headlessly
```

The sandbox writes to `.sandbox/`; the inspection tooling writes to
`.inspection/`, deliberately outside `build/` so `./gradlew clean` does not
delete the findings list. Neither is committed.

## License and privacy

Testin is built and maintained by [Muteb Almughyiri](https://github.com/mtb550),
and released under the terms in [LICENSE.md](LICENSE.md).

It runs entirely on your machine: test cases, steps and JSON files stay on your
disk or in your own version control, and the plugin embeds no telemetry, no
analytics, and never uploads your source, test data or credentials anywhere. The
full statement is in [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Documentation

Read it on the web at **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**,
or in this repository under [`docs/`](docs/README.md).

**Written today:**

- **[Business requirements](docs/business-requirements.md)** — what Testin
  promises: who uses it, the things they work with, every capability and the key
  that triggers it, all 26 statuses, and what is still undecided. **Start here to
  understand what Testin is.**
- **[Light mode](docs/design/light-mode.md)** — the always-on-top window that
  shows one test case at a time, so a run can be executed with the IDE minimized.
  Its states are drawn in
  [Screens](https://mtb550.github.io/test-in/design/light-mode-screens.html)

**Being written**, each listed in [the index](docs/README.md) against the issue
that will write it: the keyboard reference, a first-run walkthrough, the formats
on disk, the architecture, contributing, and the standing decisions.

Questions, ideas and feedback are welcome in
[Discussions](https://github.com/mtb550/test-in/discussions).

## Contributing

Issues and plans live in [GitHub issues](https://github.com/mtb550/test-in/issues).
The architecture rules — indexer-owned file access, Swing on the EDT,
display-only formatting — are in `CLAUDE.md`, which is the single authority for
them.
