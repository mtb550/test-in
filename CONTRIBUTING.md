# Contributing to Testin

Everything between a clone and a reviewed change. If something here is wrong or
missing, that is a bug in this page — say so.

## What you need

| | |
|---|---|
| **JDK 21** | The toolchain is pinned to it (`build.gradle.kts`). Gradle will fetch one if your machine has none. |
| **IntelliJ IDEA** | Any recent build. The sandbox the plugin runs in is downloaded by Gradle, not by you. |
| **PowerShell 7** (`pwsh`) | Only for `./gradlew inspect`. It is cross-platform, and the script asks for version 7. |

Nothing else. There is no local database, no service to start, no account.

## From clone to a running plugin

```bash
git clone https://github.com/mtb550/test-in.git
cd test-in
./gradlew compileJava test      # must be green
./gradlew runIde                # a sandbox IDE with Testin installed
```

`runIde` opens a second IDE with the plugin loaded. It keeps its own settings and
its own logs under `.sandbox/`, so nothing it does touches the IDE you work in.

**To see Testin in French or Hindi, start the sandbox with the locale.** The
IDE's own language setting cannot reach either bundle: JetBrains ships
localization plugins for Chinese, Japanese and Korean only, so Settings offers no
French or Hindi entry to pick. The bundles resolve from the JVM's default locale
instead.

```bash
./gradlew runIde -Ptestin.language=fr     # or hi
```

A sandbox started the ordinary way reads English, and that is correct rather
than a broken translation. [Decision-010](docs/decisions.md) says why the plugin
does not register a language of its own.

**If `prepareSandbox` fails with *"cannot be performed on a file with a
user-mapped section open"***, a sandbox IDE is still running and holding the
jars. Close it; nothing else clears it.

### Where the sandbox keeps things

```
.sandbox/Testin/IU-<version>/log/
    idea.log      the IDE
    testin.log    the plugin
```

Both together, deliberately: `testin.log` is written to `PathManager.getLogPath()`
so *Help → Collect Logs and Diagnostic Data* bundles it with `idea.log`.

The plugin's log level defaults to **INFO**, and most of what is worth tracing is
`Logger.debug` and `Logger.trace`. Set **Settings → Testin → Log Level → TRACE**
in the sandbox before a session that matters, or the log will be nearly empty for
the paths you care about.

### The run configurations are committed

Open the project in IntelliJ IDEA and four are already there, for the same reason
the inspection profile and the code style are committed — nobody should have to
retype them:

| | |
|---|---|
| **Run IDE with Testin** | `runIde`. The one you want. |
| **Run PyCharm with Testin** | `runPyCharm`. Proves the plugin still loads where there is no Java; downloads a second IDE the first time. |
| **Tests** | `compileJava test` |
| **Inspection gate** | `inspect` |

## Two branches at once means two working trees

A clone is one working tree, and `git checkout` moves **all** of it. So two
pieces of work on two branches cannot share one directory: switching branches
under work in progress takes the files out from under it, and nothing warns you.

```bash
git worktree add ../testin-<what> <branch>     # its own directory, same repository
git worktree remove ../testin-<what>           # when the work is merged or abandoned
```

Each worktree gets its own `build/`, so the first Gradle run in a new one is a
full build. The downloaded IDE and the dependency cache are shared through
`~/.gradle`, so it is minutes rather than a fresh setup.

This is not hypothetical: it was written down after a branch was checked out in a
directory that already had work running in it, which would have destroyed that
work had it not been noticed immediately.

## The checks

| Command | What it settles | When |
|---|---|---|
| `./gradlew compileJava test` | It compiles, and the unit tests and the documentation guards pass | Every change, before you offer it |
| `./gradlew runIde` | It actually works | Anything a tester can see — see below |
| `./gradlew inspect` | The six gate rules and the display-string ratchet | Before offering a change for a sandbox test, when it touched nullability, annotations, or many files |
| `git worktree add ../testin-<what> <branch>` | A second branch, checked out at once | Whenever two pieces of work run at the same time — see below |
| `./gradlew verifyDistribution` | No test classes and no compile-only dependencies reached the jar | Runs in CI; run it if you touched packaging |

### A hundred compile errors are usually one

Lombok writes most of this codebase's boilerplate, and it writes it during
annotation processing. **An error in an annotation stops that processing**, so
every getter, every all-args constructor and every enum field it would have
generated is simply absent — and javac then reports one error per use of them.

What you see is a hundred `cannot find symbol` errors blaming `TestCaseDto` and
half the model. What is wrong is one line somewhere else entirely. A duplicated
`@NotNull` produced exactly this: *"NotNull is not a repeatable annotation
interface"* at the top of the list, and ninety-nine consequences under it.

**Read the first error, not the last, and not the loudest.** If the list is long
and blames generated members, look for an annotation error above it before you
open any of the files it names.

### A green build is not evidence of a working plugin

This is the single most useful thing to know about this codebase.

`@NotNull` is not a compile-time contract: javac ignores it, and the IDE's
instrumenter rewrites it into a throw that exists only inside a running IDE.
`return null` from a method declared to return `Optional` compiles. A null passed
to a `@NotNull` parameter compiles. Both throw in front of a tester.

Guarded blocks, caret gestures, tab colours, dialog layout, action registration
and anything the platform calls back into are reached by **no** unit test. If
your change touches one of those, it has not been tested until it has been run.

### The inspection gate

```bash
./gradlew inspect
```

It costs one indexing pass — ten to twenty minutes — which makes it a sweep gate,
not a per-commit one. Run it after the last edit, on a still tree: editing a file
while the inspector is reading it produces findings about a version that no
longer exists, which reads exactly like a real defect.

It exits non-zero for six rules and no others: `DataFlowIssue`, `ReturnNull`,
`WrappedMethodDeclaration`, `StaticMutableState`, `HandWrittenPrivateConstructor`
and `DriftedCaption`. Everything else it reports is a judgement call and is
listed rather than gated.

The report lands in `.inspection/`, deliberately outside `build/` so
`./gradlew clean` does not delete the list you are working from. Start with
`summary.txt` for the counts and `findings.txt` for the lines.

**An "unused" verdict is evidence, not a fact.** Two runs minutes apart over the
same tree returned 74 findings and 13. The 74 included a whole cascade — three
classes and twenty-eight unused imports — that the second run did not reproduce
and that reading the code disproved: a global unused check depends on how far the
index had got. So confirm one by finding the call site before deleting anything,
and re-run before reporting a count.

**The display-string ratchet** lives in `.github/display-string-baseline.txt`. A
string a tester reads should have one owner; the number may go down and never up.

## What CI runs

| Workflow | When |
|---|---|
| `build.yml` | Every push and pull request |
| `verify.yml` | The JetBrains plugin verifier, against IntelliJ IDEA, PyCharm and Rider |
| `inspect.yml` | Every two days |
| `release.yml` | On a release |

## Compatibility: `since-build`, never `until-build`

`plugin.xml` declares `sinceBuild 261` and **no** `untilBuild`, and that is a
policy rather than an oversight.

An `untilBuild` makes the plugin stop loading the day the IDE crosses it, whether
or not anything actually broke — so every user is blocked by a date rather than
by a defect. Without one the plugin keeps loading, and a real incompatibility is
found by the verifier and fixed. Raise `sinceBuild` when the plugin starts using
an API that needs it; do not add an upper bound.

The Gradle `platformVersion` is the other end of the same range and is what the
Marketplace checks.

## Conventions

The rules this project holds itself to live in the repository, beside the code
they govern, so they travel with a clone:

| | |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | **Start here.** The layer map, the four rules the plugin is built on, and two operations traced class by class |
| [`CLAUDE.md`](CLAUDE.md) | The code conventions a change is reviewed against — naming, nullability, threading, what belongs in which class |
| [`docs/`](docs/README.md) | What Testin does, one use case at a time, with every rule numbered |
| [`docs/standard.md`](docs/standard.md) | How those documents are written, and what a machine checks about them |
| [`docs/decisions.md`](docs/decisions.md) | Decisions that look wrong until you know why, and what each costs to reverse |

Three that catch people out:

- **Every method a tester can reach cites its rule** — `// UC-TREE-PANEL-012,
  Rule-TREE-PANEL-038`. A test fails if it cites one no document writes.
- **Behaviour and its document change in the same commit.** Not afterwards.
- **Lombok writes the boilerplate.** It is `compileOnly` plus
  `annotationProcessor` and never `implementation`, so it cannot reach the
  distribution. `@NonNull` goes on DTO and marker fields only, where it generates
  a runtime check; nullability everywhere else is
  `org.jetbrains.annotations`.

## Issues

Work lives in [GitHub issues](https://github.com/mtb550/test-in/issues), never in
a local file — this is developed across more than one machine.

**Every issue carries statistics, measured rather than estimated.** "Several call
sites" is not a statistic; "18 call sites across 11 files" is, and it is the
difference between an afternoon and a week. Counting routinely changes the plan:
one issue written as "remove the 20 nullable annotations from the indexer" turned
out to be 9 once counted, with the file holding the joint-highest count entirely
out of scope.

Two labels on every issue:

| Label | Values |
|---|---|
| `priority:` | `critical`, `high`, `medium`, `low` |
| `cost:` | `nocost`, `minor`, `major`, `expensive` |

`cost:` is regression scope in the words testers already use — `minor` is
contained and the mechanism exists, `major` reaches several surfaces, `expensive`
changes many classes and refactors them, `nocost` is text or a list.

`out of scope` means parked: still open, still scored, not being worked on.

The issue templates produce the house format. Use them.

## Commits

The subject says what changed for a tester, not which files moved. The body says
what was wrong, what it is now, and why the fix is that one — a reader six months
later has the diff and needs the reasoning.

Reference the issue in the subject: `Say so when the gutter mark's test case is
gone (#245)`.

## The license, and the terms your contribution is accepted under

Testin's source is licensed under the **Apache License 2.0** — see
[LICENSE](LICENSE).

[EULA.md](EULA.md) is a separate document: the end user agreement for the plugin
as distributed through JetBrains Marketplace, which applies only if and when
Testin is offered there as paid or freemium. It grants nothing over the source.

Two terms, and the second one is the one people do not expect:

1. **Your contribution is licensed under Apache 2.0**, the same as the rest.
2. **A contributor license agreement is required before an outside contribution
   is merged.** Open the pull request and it will be discussed there; nothing is
   lost by starting work first.

### Why the second one

Because a licence is not ownership, and the difference decides what can happen to
Testin later.

Apache 2.0 says what *you* may do with the code. It does not move the copyright:
a patch sent under it leaves **you** owning your patch, and Testin holding a
licence to it. That is fine for using the code and awkward for everything else —
relicensing a future version, offering a commercial licence, any arrangement with
a company that wants clear title. Each of those would otherwise need every past
contributor to agree, one at a time, forever.

A contributor license agreement settles that once, at the start, in writing. It
is what almost every company-backed open source project uses, and it is not a
trap: the code stays Apache 2.0, published, and yours to use like anyone else's.

There are no outside contributors yet, so this is written down before it is
needed rather than after. See
[Decision-008](docs/decisions.md) for the whole reasoning.
