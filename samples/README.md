# The sample test project

A small, complete Testin project, committed so that `./gradlew runIde` opens onto
data instead of onto nothing, and so that every file format has one example a
person can read (#107).

It is **documentation as much as data**. Everything in it is invented — a
fictional `Demo` project, a `Testin Sample` author, one fixed date. Nothing here
came from a real test project, and nothing here should.

## What is in it

    automation/          the project the sandbox IDE opens
      testin.yml         says which test project this repository drives
      pom.xml            a real Java project, so generated methods have a home
      src/test/java/     where Generate Code writes

    testin-root/         what the rootTestinPath setting points at
      Demo/                                  .tp   the test project
        Test Cases/                          .tcd
          Login/                             .ts   3 cases
          Checkout/                          .tsp  a test set package
            Payment/                         .ts   3 cases
        Test Runs/                           .trd
          Cycle-1/                           .tr   completed, six results
          Regression/                        .trp  a test run package
            Cycle-2/                         .tr   in progress, two results

Seven directories, seven marker formats. `.tsp` and `.trp` had no committed
example anywhere before this — the format documentation (#100) takes its
examples from here.

## Running the sandbox on it

    ./gradlew runIde

On a **fresh** sandbox the task points it at `samples/testin-root` and opens
`samples/automation`. On a sandbox that already has Testin settings it does
neither: it opens whatever that sandbox had, pointed wherever its owner pointed
it.

Both halves move together on purpose. Opening the sample against somebody else's
Testin root does not resolve `testinProject: Demo`, so Testin rebinds the project
and writes that root's project name into `testin.yml` - a committed file. That
happened twice before `SampleProjectTest` started asserting the binding.

To get the sample back, delete `.sandbox` and run again, or set the Testin root
to this folder in **Settings → Testin**.

## Changing it

`SampleProjectTest` reads every file here through the same model the plugin
reads it with, and asserts that all seven markers are present, that each case's
file name is its id, that each case carries a rank, and that every result in a
run names a case that exists.

So this sample cannot rot quietly. If a format changes, that test fails and names
the file — which is the entire reason it is worth committing sample data rather
than writing it by hand each time.

Keep it small. It is meant to be read in one sitting; a set with forty cases in
it would demonstrate nothing the six here do not.
