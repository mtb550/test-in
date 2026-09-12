[Documentation](README.md) › Standing decisions

# Standing decisions

> Ten decisions in Testin look wrong until you know why they were made. Each
> one has been proposed for reversal at least once, and each reversal would have
> broken something the decision exists to protect. They are written here so a
> contributor reads the reason before writing the fix.

| | |
|---|---|
| **Part of Testin** | None. These are decisions about how the plugin is built, not about what a tester sees |
| **What the numbers mean** | `Decision-001` and up, in the order they were recorded. A number is never reused and never renumbered |
| **Answers** | Why a design that looks odd is deliberate, and what it costs to change |
| **State** | Written |
| **Checked against** | `main` at `fe73596e`, 9 September 2026 |
| **Written to** | [The standard](standard.md), as far as it applies. A decision is not a use case, so it has context, a decision and consequences instead of a flow |

---

## How to read this

Every decision has the same four parts.

| Part | What it gives you |
|---|---|
| **Context** | What was true when the decision was made, and what forced it |
| **Decision** | The decision itself, in one sentence |
| **Consequences** | What it costs, and what it buys |
| **If you are about to reverse it** | The specific thing that breaks. Read this one first |

**A decision is superseded, never edited into a different answer.** When one
stops being true, its section stays exactly as written and a new decision is
added below with a higher number, and the old one gains a *Superseded by* line.
That is the same rule the [rule numbers](standard.md) follow, for the same
reason: a decision nobody can find the history of gets made again every year.

---

## Decision-001 — The indexer owns every read and write of test data

**Context.** Test data is JSON on disk, and every panel wants it: the tree draws
nodes from it, both editors read cases out of it, the view panel shows one case,
the report writes a run out. When each of them read the disk for itself, "does
this node exist" was a `Files.exists` in one place, a cache lookup in another,
and a stale `DirectoryDto` in a third.

**Decision.** `org.testin.indexer` is the only package that touches test data.
Everything else holds `DirectoryDto` and `TestCaseDto` objects the indexer
served, and asks the indexer to create, move, rename, copy or delete. Inside the
indexer, the VFS operation happens first and the cache is updated after it
succeeds.

**Consequences.** Every read is an in-memory lookup, so the tree and the panels
answer at once. Six packages are exempt — `codegen`, `config`, `git`,
`importexport`, `report`, `setting`, `logger` — and what they have in common is
that none of them reads or writes test data: they handle generated source, the
automation repository's own `testin.yml`, the Git working tree, files outside
the tree, report output, the IDE settings path, and the log.

**If you are about to reverse it.** Updating the cache before the VFS operation
is the specific version of this that keeps getting written, because it reads as
"update the model, then persist". It creates phantom directories and
*already exists in VFS* errors, because a marker write creates directories on
its way to writing the marker.

---

## Decision-002 — What a tester types is stored byte for byte

**Context.** Testin capitalizes a description and closes it with a period so a
test set reads consistently. The grid and the editor fields are typed into.

**Decision.** Formatting is display-only. A surface may draw a value differently
and never saves the drawn form; an editable surface loads the **raw** value when
editing begins. `Rule-EDITOR-PANEL-005` states it for a tester, and
`Rule-VIEW-PANEL-026` names the four fields that are formatted at all.

**Consequences.** `TestEditorAttributes` answers both questions in one place:
`displayValue` for a reader and `gridValue` for a cell. Reference, module and
test data are never formatted — an identifier and a label are not sentences, and
test data is a value that gets used rather than read, so a character the display
decides to change is a value that no longer works.

**If you are about to reverse it.** The failure is silent and permanent. A cell
that loads the formatted value commits it back the first time a tester edits a
row they did not mean to change, and nothing in the JSON says a period was
Testin's rather than theirs.

---

## Decision-003 — Absence is an empty value, not a null

**Context.** A nullable field spreads its check across every reader. A sweep
through the plugin found the same `if (x != null)` written at thirty-six call
sites for one enum constant whose field was null.

**Decision.** A field that is "not set yet" holds an empty value of its own
type — `""`, `Duration.ZERO`, an empty list, an `EMPTY` enum constant, an epoch
timestamp — or an empty `Optional` where the thing genuinely arrives later. DTO
and marker fields carry Lombok's `@NonNull`, which generates the runtime check;
everything else carries the JetBrains `@NotNull` / `@Nullable` so the annotation
is the contract.

**Consequences.** `BugSeverity.EMPTY` and `BugPriority.EMPTY` are the pattern to
copy. `Optional` is used as a field type on purpose — a popup built on first
show, a service the application has not started, a run an editor has not loaded.

**If you are about to reverse it.** The `OptionalUsedAsFieldOrParameterType`
inspection is switched off in `.idea/inspectionProfiles/Testin.xml`, and it is
switched off deliberately. What it argues for is a nullable field, which is the
thing the sweep removed. Turning it on is reversing this decision, not tidying
up.

---

## Decision-004 — SFTP is the maintained JSch fork, not JSch and not MINA SSHD

**Context.** The server sync needs an SFTP client inside the plugin.
`com.jcraft:jsch` last shipped in 2018 and cannot negotiate `rsa-sha2` against a
current `sshd`. Apache MINA SSHD is maintained and is the obvious alternative.

**Decision.** `com.github.mwiede:jsch` — the maintained fork — is the SFTP
client. MINA SSHD was ruled out on its default: `ClientBuilder` installs
`AcceptAllServerKeyVerifier.INSTANCE`, so a client built the documented way
trusts any host that answers.

**Consequences.** No transitive dependencies reach the distribution. A real SFTP
server runs inside the test JVM (`sshd-sftp`, test scope only), so the transport
is exercised against a real server on every build rather than against a machine
somebody set up.

**If you are about to reverse it.** Any replacement has to refuse an unknown
host key by default, not merely be able to. See Decision-007.

---

## Decision-005 — A Testin editor tab is a light virtual file on a deprecated file system

**Context.** A test set is a directory of JSON files, and the editor that opens
it is not a text editor. The platform's editor manager opens a `VirtualFile`,
so the node needs one.

**Decision.** `UnifiedVirtualFile` is a `LightVirtualFile` on the `testin://`
protocol, served by `TestinFileSystem`, which extends
`DeprecatedVirtualFileSystem` and implements `NonPhysicalFileSystem`.

**Consequences.** `TestinFileSystem.findFileByPath` returns nothing, always, and
cannot do otherwise: a file system is application level, so there is no project
to ask, and a node's kind lives in a marker file that only the indexer may read
(Decision-001). Testin therefore closes its own tabs as the project closes, so
the IDE's tab list holds nothing of ours and nobody asks.

**If you are about to reverse it.** "Deprecated" here names the base class, not
a scheduled removal, and the obvious improvement — making `findFileByPath`
answer — was measured and refused in
[#160](https://github.com/mtb550/test-in/issues/160). The IDE asks for the file
about twenty seconds before the index exists.

---

## Decision-006 — Testin writes its own log, beside the IDE's

**Context.** The plugin traces indexing, code generation, Git and the sync at a
level a tester can turn up to TRACE. Writing that through the platform's own
logger puts it in `idea.log` at whatever level the IDE is set to.

**Decision.** `LogWriter` is an application service holding a bounded queue
and one daemon thread named `Testin-Async-Logger`, writing to
`PathManager.getLogPath()/testin.log` and rolling at 5 MB. `Logger` is the
static front door and reads the caller's class name from a `StackWalker`.

**Consequences.** *Help → Show Log in Explorer* finds it and *Collect Logs and
Diagnostic Data* bundles it, because it sits beside `idea.log`. The location
never depends on an open project. Nothing on a caller's thread waits for a
write.

**If you are about to reverse it.** `Level.FATAL` and `Logger.fatal` have no
callers and are kept on purpose: the settings combo is built from
`Level.values()` and stores the choice by name, so deleting the constant makes a
stored setting fail to parse on the next start.

---

## Decision-007 — An unknown SSH host is refused, and trusting one is the tester's step

**Context.** The first sync against a new server always fails with a host-key
error, and the fix that removes the error in one line is to accept any key.

**Decision.** `SftpTransport` opens the connection with `StrictHostKeyChecking`
left at its default, which refuses a host this machine has not seen. Trusting a
new host is done once by the tester, in their own
`~/.ssh/known_hosts`, and never by a default in this code.

**Consequences.** A tester syncing to a new server has one manual step. Testin
never holds an SSH credential of its own — the same rule the Git integration
follows by handing every operation to `git4idea`.

**If you are about to reverse it.** The refusal is the feature. A client that
connects anyway connects to anything claiming to be the server, and the sync
would then upload a whole test project to it.


## Decision-008 — The source is Apache 2.0, and a contribution needs an agreement

**Context.** `LICENSE.md` was JetBrains' developer EULA for paid plugins —
Subscriptions, Fallback Versions, Paid Plugins — while the repository was being
opened up: public, with an issue tracker, contributor documents and starter
work. The two pull against each other. A proprietary EULA gives an outside
contributor no clear right to submit or reuse anything, so the documents invited
people to a door that was shut.

The plugin was also not being sold. `plugin.xml` carries no `product-descriptor`,
which is the tag that makes a Marketplace listing paid, so Testin was *licensed*
as a paid plugin and distributed as a free one. Nothing was earning, so nothing
was at risk in the decision itself.

The question that decided it was not "open or closed" but **"does this stop
Testin being sold later"** — to JetBrains, to a test-management vendor, to
anyone.

**Decision.** The source is licensed under the **Apache License 2.0**. The
EULA is kept as `EULA.md` for the distributed plugin, if and when it is offered
as paid or freemium. Contributions are accepted under Apache 2.0 **and** a
contributor license agreement.

**Consequences.**

A licence is not ownership, and that is the whole of why this is safe. Apache
2.0 says what others may do with the code; the copyright stays with the author.
Testin can be relicensed going forward, offered under a separate commercial
licence, or sold. What cannot be undone is a version already published: whoever
received it keeps that grant for that code, forever.

The contributor license agreement is the part that protects the rest. A patch
sent under Apache 2.0 leaves its author owning it, and Testin holding a licence
— which is enough to ship and not enough to relicense or to give a buyer clear
title. Collected at the start it is a sentence; collected afterwards it is every
past contributor, one at a time.

Going paid is unaffected and starts elsewhere. The Marketplace product code
"must be agreed with JetBrains in advance", so the paid route begins with a
conversation rather than a commit — and the freemium shape, a free plugin with
paid optional features, is compatible with an Apache-licensed source.

**If you are about to reverse it.** Check who owns the code first. Every
contribution merged without an agreement narrows what can be done with the whole
of it, and the narrowing is not visible in the repository — it looks exactly
like a merged pull request.

---

## Decision-009 — A test run's configuration answers are English, and the option lists are not translated

**Context.** A test run is created by answering eight questions: Test Type,
Platform, Component, Language, Browser, Device Type and two more. Each offers a
list to pick from, and what the tester picks is written into the run's JSON
word for word. `TestRunConfiguration.is(PLATFORM, Answer.WEB)` then reads that
stored string back to decide whether the Browser question is asked at all, and
all four report generators print it.

So one string is three things at once: a caption on a form, a value on disk, and
half of a rule about which questions follow which answers. The field **names**
are translated. The options are not.

`TestStatus` solved the same problem the other way round. The constant name is
what is stored, the label is what is shown, and an import matches the constant
whatever language the file was written in. The run configuration has no such
split: its option list is a `String[]` and the stored value is the display
string.

**Decision.** The eight option lists stay English. They are values, not labels,
and Testin does not translate values.

**Consequences.** A run created in a French IDE reads the same as one created in
an English IDE, in the file and in every report. Nothing that was saved before
today stops matching the cross-field rule. What it costs is that a French tester
picks *Web* from a list of English words under a French caption, which reads as
an oversight until somebody finds this page.

**If you are about to reverse it.** Translating an option changes what a saved
run says. Every run saved before the change keeps the English word, so the rule
that shows the Browser question stops matching them, and a run created in French
reads differently in a report generated in English. The way out is the
`TestStatus` shape - a small enum per field, the constant stored and the label
shown, plus a read that maps the old stored strings onto the constants - and it
is that read, not the enums, that is the work.

---

## Decision-010 — Testin's own translations are reached by the IDE's locale, not by a language plugin

**Context.** `Bundle` extends `DynamicBundle`, which picks its locale from the
IDE's language. JetBrains ships localization plugins for Chinese, Japanese and
Korean only, so Settings offers no French entry and no Hindi entry: no amount of
clicking selects either of Testin's bundles.

They do resolve. `DynamicBundle` falls back to the JVM's default locale, so an
IDE started with `-Duser.language=fr`, or with `hi`, reads `messages_fr` or
`messages_hi`. That is how the sandbox has to be run to see any of the
translation work at all.

**Decision.** Testin does not register a `DynamicBundle.LanguageBundleEP`. The
translations ship, and the way to reach one is the IDE's own locale.

**Consequences.** Nobody can switch Testin's language on its own, and nobody
sees a Testin language setting that the rest of the IDE would not follow. A
sandbox pass that starts the IDE normally reads English, which is correct and
is not evidence that the translation is broken - this line is here so that
finding is not made twice.

**If you are about to reverse it.** Registering the extension point is a
decision about what the plugin ships rather than about the strings: it puts
Testin's language in a list beside the IDE's own, so a tester can have a French
Testin inside an English IDE. Every dialog then has to read correctly with two
languages on screen at once, which is a thing to design rather than a flag to
set.

---

## Superseded decisions

None yet. When the first one is superseded it is listed here with the number
that replaced it, and its section above is left exactly as it was written.

| Decision | Superseded by | When |
|---|---|---|
| — | — | — |

---

## Adding one

A decision belongs here when reversing it would break something that is not
obvious from the code at the point where the reversal is written — that is the
whole test. A naming convention is not a decision; a naming convention that a
persisted key depends on is.

Take the next number, write the four parts, and link it from
[the home page](README.md).

---
