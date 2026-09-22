[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-008

# UC-INTERNAL-008: Convert a test project to this build's file format

**As a** tester upgrading Testin, **I want** my test projects brought to the new
file format by themselves, **so that** opening the IDE is all I have to do about
it.

The format is [formats.md](../formats.md): a test case is `<id>.tc`, a run's
results are one `<test case id>.ri` each, the run's own facts are in its `.tr`,
and every folder's marker carries an `id`. A project written before that carries
`<id>.json` cases and one `run.json` per run, and this is what turns the first
into the second — **once, per project, with no prompt**.

**2.13.0-alpha converts. 2.14.0-alpha deletes the conversion code.** From then on
a project without the format number is not read at all, and the tester is told to
install 2.13.0-alpha once first. JetBrains cannot make one plugin version require
an earlier one, so the number in the file is what enforces the order.

## Rules

- **Rule-INTERNAL-091** — A test project says which format its files are in, in
  its `.tp`. This build reads format 2 and converts anything older to it, once,
  before the project is read. A project written by a newer Testin is not read at
  all.
- **Rule-INTERNAL-092** — A conversion says what it did, once, in a notification
  that stays in the log: every project converted, how many test cases it kept,
  how many test runs it lost, and any file left to repair.

## What the tester sees

Nothing, until it is over. Opening a project with test data in the old format
converts it while the tree is being read, and a notification titled **Test Data
Converted** stays in the log afterward: one line per project, saying how many
test cases were converted and how many test runs were removed.

**The IDE stays usable throughout.** The conversion runs in the background,
beside the reading rather than in front of it, and nothing waits for it. There
is no progress bar and no cancel button, because the next open has to finish a
conversion stopped half way anyway.

## Main flow

1. Testin reads the project's `.tp`. Format 2 or newer, and there is nothing to
   do - no file is opened and nothing is reported.
2. Every `.json` directly inside a folder under `Test Cases` is **moved** to
   `<its id>.tc`, content untouched: marked folders and unmarked ones, active
   projects and inactive ones. A file that will not parse keeps its own base name
    - `login.json` becomes `login.tc` - so the scan goes on reporting it.
3. A second file claiming an id another already took keeps its content and gets
   an id derived from the one it claimed and its own place in the project. The
   place is written the same way on Windows as on Linux, so two machines
   converting the same commit give the file the same id.
4. Every folder Testin marked as a test run, or a run package, that holds a
   `run.json` is removed with its results and its screenshots. **The runs go**;
   nothing else under `Test Runs` is touched.
5. Every marker that parses and has no `id` is given one, derived from its place
   in the project and when the folder was created. The place is written the same
   way on every machine, so a shared project converted twice does not conflict
   on every marker. The audit block is not touched.
6. `"format": 2` goes into the `.tp`, last, and only when every step above
   succeeded.
7. One notification says what happened, per project.

At every start, and every time the Testin folder changes, **every** test project
in that folder is converted this way - not only the one the open repository is
about. A project left unconverted would be refused by the release that deletes
the converter, with nothing left able to convert it.

## What Testin refuses

**If the project's `.tp` will not parse** — nothing in the project is touched,
and the file is named in the notification as one to repair. Writing a format
number into a file the tester still has to fix would say the conversion
succeeded.

**If a marker cannot be parsed** — it keeps no id, and it is named in the same
list. The id is stamped the next time that marker is written, once the tester has
repaired it (Rule-INTERNAL-083).

**If a step fails** — the format number is not written, so the next open tries
again. The tree shows the project with *Written by an older Testin. Install
2.13.0-alpha once to convert it, then update.* where its contents would be, and
nothing in it is read or written.

**If the same failure happens again** — it is reported once, not once per try.
A conversion that could not finish is tried again on every scan and on every
change to the Testin folder. A tester who has already been told about a file
only they can repair does not need telling again. A later try that got further
has different numbers in it, and that one is said (Rule-INTERNAL-092).

**If the project was written by a newer Testin** — it is not read, and the tree
says *Written by a newer Testin. Update the plugin to read it.* A format this
build does not know is refused rather than guessed at: reading it as format 2
would delete what this build cannot see.

## Where the plugin breaks its own rules

Nothing yet.
