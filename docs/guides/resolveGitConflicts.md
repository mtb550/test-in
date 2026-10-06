[Documentation](../README.md) › Task guides

# How to resolve Git conflicts

> A conflict means you and a colleague changed the same thing. Testin merges
> test cases one field at a time, and asks only about a field you both rewrote.

## Before you start

- The test project is shared over Git. If not, read
  [How to share test projects over Git](shareOverGit.md).

## Steps

1. A sync or a push stops, and a message titled **Git Conflicts** names the
   files. ([Resolve the conflicts a pull stopped on](../share/resolveConflicts.md))
2. Choose to carry on. Testin merges each test case, one field at a time.
3. For a field both sides rewrote, a window shows both values. Pick the one that
   wins for each row, and press `Enter`.
   ([Answer which side wins for a field both changed](../share/answerMergeQuestions.md))
4. The pull finishes, and the sync ends with *Synced*.
5. To undo the pull and keep what is here instead, choose to abort at step 2.

## If something goes wrong

| What you see                    | What to do                                                   |
|---------------------------------|--------------------------------------------------------------|
| The conflict message comes back | A file is still in conflict. Carry on again.                 |
| A step failed                   | Press **Show Git log** on its message to read what Git said. |

[Documentation](../README.md) › Task guides
