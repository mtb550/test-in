[Documentation](README.md) › The formats on disk

# The formats on disk

> Everything Testin stores is a plain file you can open. Ten formats, all of
> them JSON except one YAML and the screenshots, which are PNG, and this page is
> the contract for every one — what
> a field means, which are required, and what a reader must do with a field it
> does not recognize.

| | |
|---|---|
| **Part of Testin** | None. This is the shape of the data, not a screen |
| **Answers** | What is on disk, what may change, and what a version bump promises |
| **State** | Written |
| **Checked against** | `main` at `f369f1d6`, 9 September 2026, and against a real tree at `Testin/NAFATH` |
| **Written to** | [The standard](standard.md), as far as it applies. A format is not a use case, so it has a shape and an example instead of a flow |

---

## The tree

```
<Testin folder>/
└── NAFATH/                       a test project
    ├── .tp                       the test project marker
    ├── Test Cases/
    │   ├── .tcd                  the Test Cases directory marker
    │   ├── Login/                a test set package
    │   │   └── .tsp
    │   └── ts2/                  a test set
    │       ├── .ts
    │       └── 4fd2a19b-….tc     one test case, named by its id
    └── Test Runs/
        ├── .trd                  the Test Runs directory marker
        ├── Cycles/               a test run package
        │   └── .trp
        └── cycle31/              a test run
            ├── .tr
            ├── run.json          everything the run recorded
            └── k3f9a.png         a screenshot a failure names
```

**A marker is a file whose whole name is the extension.** `.ts`, not `foo.ts`.
A directory is a test set because it holds a file called `.ts`, and that is the
only thing that makes it one — the folder name is the node's name and carries no
meaning beyond it.

**The names `Test Cases` and `Test Runs` are fixed.** They are the folder
names of `DirectoryType.TCD` and `DirectoryType.TRD` and a test project has
exactly one of each.

---

## What is true of every JSON file

| | |
|---|---|
| **Encoding** | UTF-8, pretty-printed with two spaces, and a space either side of the colon — Jackson's default pretty printer, unchanged |
| **Unknown fields** | Ignored, never an error. Every type carries `@JsonIgnoreProperties(ignoreUnknown = true)` |
| **Missing fields** | Take the field's default, which is always an empty value of its own type — never null |
| **Dates** | `EEEE dd-MM-yyyy 'At' HH:mm:ss '['VV']'` in `en_US`, for example `Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]`. The zone is part of the value |
| **"Never happened"** | The Unix epoch in UTC, `Thursday 01-01-1970 At 00:00:00 [Z]` — `Config.NOT_EXECUTED`. Not a missing field, and not null |
| **Enums** | Written by constant name in capitals. A name this build does not know is a read failure for that file, not a silent default — see *Versioning* |

---

## The seven markers

Every marker shares the same five fields, from `AbstractMarker`, and most add a
`status` of their own.

### Shared by all seven

| Field | Type | Required | Meaning |
|---|---|---|---|
| `order` | integer | no | Where the node sits among its siblings. **Omitted entirely when the node is unordered**, which is `Integer.MAX_VALUE` in memory — an absent `order` is the normal case, not a defect |
| `createdBy` | string | no, defaults `""` | The tester's name as the settings hold it |
| `createdAt` | date | no, defaults to now | When the directory was made |
| `modifiedBy` | string | no, defaults `""` | Who last changed it. Also read from `updatedBy`, which is what older markers wrote |
| `modifiedAt` | date | no, defaults to the epoch | When it last changed. Also read from `updatedAt` |

**A blank `modifiedBy` reads back as `createdBy`, and a blank `modifiedAt` as
`createdAt`** — a node that was never modified was last touched when it was
made. The answer is given once, in `AbstractMarker`, so nothing downstream tests
for it, and what is written back is that answer rather than an invented one.

### The seven, and what each adds

| File | Node | Adds | Values |
|---|---|---|---|
| `.tp` | Test project | `status` | `ACTIVE` `INACTIVE` |
| `.tcd` | The `Test Cases` directory | — | |
| `.trd` | The `Test Runs` directory | — | |
| `.tsp` | Test set package | `status` | `ACTIVE` `ARCHIVED` |
| `.ts` | Test set | `status` | `ACTIVE` `DEPRECATED` |
| `.trp` | Test run package | `status` | `ACTIVE` `ARCHIVED` |
| `.tr` | Test run | `status`, and the run's own facts: `configuration`, `resultAnalysis`, `executionStartedAt`, `executionEndedAt` — see below | `CREATED` `IN_PROGRESS` `COMPLETED` `ASSIGNED` `CLOSED` |

**A directory carrying two markers of one family is read as the more specific
one.** Under `Test Cases` the order is `.ts` then `.tsp`; under `Test Runs` it is
`.tr` then `.trp` — a set is what holds cases and a package is what holds sets.
`DirectoryType.UNDER_TEST_CASES` and `UNDER_TEST_RUNS` are that precedence, and
nothing else may ask in a different order.

```json
{
  "createdBy" : "Sara Al-Otaibi",
  "createdAt" : "Wednesday 02-09-2026 At 23:29:16 [Asia/Riyadh]",
  "modifiedBy" : "Sara Al-Otaibi",
  "modifiedAt" : "Wednesday 09-09-2026 At 05:28:11 [Asia/Riyadh]",
  "status" : "ACTIVE"
}
```

**The test run marker adds four more**, because they are facts about the run
rather than about any one result — what it was executed against, what the tester
wrote about the verdicts afterwards, and when execution started and last
stopped. They are the run's, so they are in the run's own file:

| Field | Type | Required | Meaning |
|---|---|---|---|
| `configuration` | map | no, **omitted when empty** | What the run was executed against. Keys are `TEST_TYPE` `CHANGE_LOG` `COMMIT_ID` `PLATFORM` `COMPONENT` `LANGUAGE` `BROWSER` `DEVICE_TYPE`; values are free text |
| `resultAnalysis` | map | no, **omitted when empty** | What the tester wrote about each group of verdicts. Keys are `PASSED` `FAILED` `BLOCKED` `UNTESTED`; values are free text |
| `executionStartedAt` | date | no, defaults to the epoch | When Start Execution was first pressed. Kept: a run resumed next week still started when it started |
| `executionEndedAt` | date | no, defaults to the epoch | When execution last stopped |

```json
{
  "createdBy" : "Sara Al-Otaibi",
  "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
  "modifiedBy" : "Sara Al-Otaibi",
  "modifiedAt" : "Monday 14-09-2026 At 10:22:05 [Asia/Riyadh]",
  "status" : "IN_PROGRESS",
  "configuration" : { "TEST_TYPE" : "Regression", "PLATFORM" : "Web", "BROWSER" : "Chrome" },
  "executionStartedAt" : "Monday 14-09-2026 At 10:00:12 [Asia/Riyadh]",
  "executionEndedAt" : "Thursday 01-01-1970 At 00:00:00 [Z]"
}
```

---

## A test case — `<uuid>.tc`

One file per test case, inside its test set. **Any `.tc` file directly inside
a test set is a test case** (Rule-INTERNAL-011), so `login.tc` written by hand
is read as one and a copy of it is given an id of its own; a `.tc` sitting in
`Test Cases` rather than in a set is not a case at all. The file holds JSON; the
name says what the JSON is, so nothing has to look inside to find out.

Testin writes the file as `<id>.tc`, and a hand-named one is filed under its
id the next time anything writes it, the hand-named file going once that write
has landed (Rule-INTERNAL-084). The two names answer different questions:
the **file name** is what the tree shows (Rule-INTERNAL-012 — a `name` field
inside the file would not decide it), and the **`id` field** is what a test run's
recorded result points at.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `id` | UUID string | yes in practice | Identity. A fresh random UUID when absent, which makes the case a new one |
| `order` | string | no, defaults `""` | The rank that places the case in its set — see below |
| `description` | string | no | What the case is. The card title |
| `expectedResult` | string | no | |
| `steps` | array of strings | no | One step per element. A blank element is skipped when drawn and keeps its place in the file |
| `status` | enum | no, defaults `PENDING` | `REVIEWED` `PENDING` `DISABLED` `TO_BE_UPDATED` |
| `priority` | enum | no, defaults `LOW` | `HIGH` `MEDIUM` `LOW` |
| `group` | array of strings | no | The groups the case is in, each as the tester typed it. Empty for none |
| `reference` | string | no | A ticket, a requirement — an identifier, never formatted for display |
| `module` | string | no | |
| `testData` | string | no | Used rather than read: never reformatted, on any surface |
| `preConditions` | string | no | |
| `createdBy` / `updatedBy` | string | no | |
| `createdAt` / `updatedAt` | date | no | |

**`order` is a rank, not a number.** `"zo"` is a valid order and sorts as text.
A case carries where it sits, not who its neighbors are; the earlier design gave
each case a `previous` and a `next`, so every insertion, deletion and reorder
rewrote a file the tester had not touched — which is what made two people
working in parallel conflict on a third person's case, and what let one lost
pointer leave a whole set unordered.

**An empty `order` means "not placed yet"** — imported, copied in, or arrived
from a merge. Those sort after the placed cases, oldest first, and are given a
rank the next time anything writes them. It is not an error and must not be
repaired on read.

There is **no separate sequence file.** `TestCaseSequenceStore` is an in-memory
index the plugin builds while scanning; the order on disk is the `order` field
on each case and nothing else.

```json
{
  "order" : "zo",
  "id" : "4fd2a19b-59c7-44df-8cc4-ec5d293b18e9",
  "description" : "Log in with a valid user",
  "expectedResult" : "The dashboard opens",
  "status" : "REVIEWED",
  "steps" : [ "Open the login page", "Enter a valid user", "Submit" ],
  "priority" : "HIGH",
  "reference" : "",
  "group" : [ "SMOKE" ],
  "createdBy" : "Sara Al-Otaibi",
  "updatedBy" : "Sara Al-Otaibi",
  "createdAt" : "Wednesday 02-09-2026 At 23:29:28 [Asia/Riyadh]",
  "updatedAt" : "Monday 07-09-2026 At 06:49:39 [Asia/Riyadh]",
  "module" : "",
  "testData" : "",
  "preConditions" : ""
}
```

---

## A test run — `run.json`

One file per test run, beside its `.tr`. It records what was executed, not what
exists: a case removed from the test set keeps its result here. What the run
itself is - how it was configured, what the tester wrote about the verdicts, when
it was executed - is in its `.tr`, with its status.

The screenshots its failures name sit beside it, one PNG each, named by five
random lowercase letters and digits that no result of the run already holds -
`k3f9a.png`. Testin writes a screenshot before the result that names it, and
moves one that no result names to the recycle bin after the next write of this
file. Any PNG in the folder named that way is taken for a screenshot, so one put
there by hand under such a name goes too.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `results` | array | no | One entry per case the run covers, below. The run's own facts are not here: they are in its `.tr`, above |

Each entry in `results`:

| Field | Type | Meaning |
|---|---|---|
| `id` | UUID string | The test case this result is about. When absent, the result names no test case and reads as one whose case was removed |
| `status` | enum | `PASSED` `FAILED` `BLOCKED` as a tester or the automation judged it; `PENDING` until then; `UNTESTED` for a case still pending when the run completed or closed. A case whose test case was deleted since the run keeps its status here and is shown as Removed; `REMOVED` is no longer written, and a file written by 2.11.0-alpha or earlier that holds it is read as removed |
| `duration` | number, seconds | Nanosecond precision, written as a decimal |
| `executedBy` | string | |
| `executedAt` | date | |
| `actualResult` | string | Empty unless the case failed |
| `stacktrace` | string | Empty unless the case failed. Text only: a pasted screenshot is never in it |
| `screenshots` | array of strings | The file names of the screenshots pasted with the failure, beside this file, in the order they were pasted. Left out when there are none; cleared by a pass and by an automated failure, and their files go with the next write |
| `bugSeverity` | enum | `EMPTY` `BLOCKER` `MAJOR` `MINOR` `ENHANCEMENT` |
| `bugPriority` | enum | `EMPTY` `HIGH` `MEDIUM` `LOW` |
| `bugIssueUrl` | string | The GitHub issue the failure was reported as, written by [Report Bug](viewPanel/reportBug.md). Empty until then; cleared by a pass, kept by an automated failure |

`EMPTY` is a real constant, not a missing value. A passed case carries
`"bugSeverity" : "EMPTY"`, and nothing reading it has to test for absence.

---

## `testin.yml` — the automation repository's own file

The one file that lives in the code repository rather than under the Testin
folder, and the only one that is **committed**. It is the team's: Testin reads
it when it is there, and **writes it only when the tester presses Save to
testin.yml** (Rule-TREE-PANEL-112). A code project without one works fully,
except that its automation code stays off until the file names the open test
project (Rule-INTERNAL-089, Rule-CODEGEN-082, Decision-013). What it adds is agreement - a
colleague who clones the repository lands on the same test project with no
setup. `testin.yaml` is read too.

| Key | Type | Required | Meaning |
|---|---|---|---|
| `location` | `local` / `remote` | no, defaults `local` | Whether the test data is on this machine or cloned from Git, the only way a test project is shared |
| `RepoUrl` | string | only when remote | Validated as a repository address. Any account and token are dropped as it is read: on an `https://` or `http://` address every account goes, because there it is the secret; on an `ssh://` address only `account:secret` goes, so the conventional `git@` survives (Rule-SHARE-004) |
| `testinProject` | string | no | Which test project, for everyone who opens the code project. Without it each tester chooses one, kept on their own machine; with it, a tester's own choice still wins until the file names a different project (Rule-TREE-PANEL-106) |
| `bugRepoUrl` | string | only to report bugs | The GitHub repository [Report Bug](viewPanel/reportBug.md) files issues in, as its address: `https://github.com/owner/repo`, `https://host/owner/repo.git`, `ssh://git@host:2222/owner/repo` or `git@host:owner/repo`. Any account and token in it are dropped as it is read, by the same rule as `RepoUrl`. An address that does not name exactly a host, an owner and a repository - one ending in `/issues`, a file or a local path - is kept, and Report Bug refuses to send with the reason |

**No machine and no person appears here.** The Testin folder, the tester's
name and the log level are application settings, and the test
project a tester chose is kept on their machine — see `CLAUDE.md`, and
Decision-011 on [the decisions page](decisions.md). An unknown key is logged
and skipped, never fatal - which is what happens to `connection` and the
`sftp` keys a file written before Decision-012 still carries.

```yaml
location: local
testinProject: NAFATH
```

---

## Versioning

**A reader ignores what it does not know.** Every JSON type carries
`@JsonIgnoreProperties(ignoreUnknown = true)` and the YAML reader logs an
unknown key and skips it. Adding a field is therefore always safe: an older
build reading a newer file drops the field, and writes the file back without it.
That is the trade — forward compatibility is a *read*, not a round trip.

**A writer never removes or renames a field without a conversion.** When a name
has to change, the old one stays readable through `@JsonAlias` — `modifiedBy`
and `modifiedAt` are read from `updatedBy` and `updatedAt` for exactly this
reason, and those aliases are permanent.

**A file whose shape changed structurally is not read at all.** A test run
written before 3 September 2026 kept its results in `<folder>.json`, which is
why renaming a run lost them. Runs are `run.json` now, and the old file is not
read: such a run shows no results, and its old file stays on disk as litter.
That is a decision, not an oversight, and
`RunResultsSurviveRenameTest.aRunWrittenByAnOlderBuildIsNotRead` asserts it so
it cannot be mistaken for one.

**An unknown enum constant fails that file's read.** It is not defaulted,
because a status quietly becoming `PENDING` loses a tester's decision without
saying so. This is the one incompatibility that needs a conversion story before
it ships — [#91 and #92](https://github.com/mtb550/test-in/issues/91) exist
because a format changed and the rule for old data had to be decided after the
fact, which is the thing this page is here to prevent.

**Old data gets wiped, not migrated.** When a change cannot be made compatible,
the answer is to delete the old data and start again rather than to ship a
converter — the trees are small, and a converter is a second reader of a format
nobody writes any more.
