<!--
  The subject of the commit says what changed for a tester. This says why the
  fix is this one - a reader six months from now has the diff and needs the
  reasoning.
-->

## What was wrong

The defect or the gap, in a sentence or two. Not the files that changed.

Closes #

## What it is now

What a tester sees differently, and why this fix rather than another. If you
considered a bigger change and did not build it, say so here rather than leaving
the next reader to wonder.

## What was measured

Numbers, where there are any: how many call sites moved, what a count was before
and after, what a gate reported. "It works" is not a measurement.

## Checks

- [ ] `./gradlew compileJava test` is green
- [ ] `./gradlew inspect` run, if this touched nullability, annotations, or many
      files at once
- [ ] Run in a sandbox (`./gradlew runIde`), if a tester can see it — **a green
      build is not evidence of a working plugin**
- [ ] The documents changed in this same commit, if what a tester sees changed
- [ ] Every rule cited in a code marker is one a document writes

## Side findings

Anything noticed on the way and deliberately not fixed here. A finding must
never be lost because the task ended, and never be silently fixed inside an
unrelated change.
