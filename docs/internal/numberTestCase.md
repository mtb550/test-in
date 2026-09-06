[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-004

# UC-INTERNAL-004: Give a test case its number

**As a** tester, **I want** every test case to carry the same number wherever I
read it, **so that** the number I quote in a bug report means one test case and
not a row in whatever list I happened to be looking at.

There is no key for this. Dragging a test case into place is on the editor
panel.

## Rules

- **Rule 24** — A test case's number is its position in its test set, counting
  from one.
- **Rule 25** — The number is worked out when it is drawn, and stored nowhere.
- **Rule 26** — One thing works the number out, so the card, the row and the
  generated test method always agree.
- **Rule 27** — A filter never renumbers. Hiding rows does not change the number
  on the rows that remain.
- **Rule 28** — Each test case carries its own place in the order. Testin keeps
  it inside that test case's own file.
- **Rule 29** — A test case with no place yet sorts after every test case that
  has one. Among those, the oldest comes first.
- **Rule 30** — Moving one test case writes only the test cases that actually
  moved. Dropping one into a test set of 200 writes one file.
- **Rule 31** — A place between any two places always exists, so a test case can
  always be dropped between two others.
- **Rule 32** — A save that would leave the file exactly as it is writes
  nothing. Testin does not record the tester as having edited it.
- **Rule 33** — A test case is known by its file name. Testin decides whether a
  save is a new test case or a change by whether it already knows that name.
- **Rule 34** — An import and an undo write the file without stamping it. An
  import keeps the audit the file brought. An undo puts back the audit the test
  case had before.

## The screens

The test set before the tester drags anything.

```
┌──────────────────────────────────────────────────────────────┐
│  Payment                                                     │
│                                                              │
│   1   Card is declined                                       │
│   2   Card has expired          the tester drags this to top │
│   3   Card is accepted                                       │
└──────────────────────────────────────────────────────────────┘
```

The same test set afterwards.

```
┌──────────────────────────────────────────────────────────────┐
│  Payment                                                     │
│                                                              │
│   1   Card has expired            only this file was written │
│   2   Card is declined                                       │
│   3   Card is accepted                                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The numbers** — every one of them changed. None of them was saved.
2. **The file that was written** — one, the test case that moved.

## Main flow

1. The tester drags a test case to a new place in its test set.
2. Testin works out a place for it, between the two test cases it landed
   between.
3. Testin writes that one test case's file.
4. Testin lists the test set again. Test cases with a place come first, in that
   order. Test cases without one come last, oldest first.
5. Every number on screen is worked out again from the new list.
6. The card, the grid row and the generated test method all read the same
   number.

## What Testin refuses

**If the tester opens a field, reads it and presses `Enter`** — nothing is
written. Testin compares the file it would write against the file already on
disk. They match, so it stops. The tester is not recorded as having edited the
test case, and the test set is not recorded as having changed.

**If a test case has no place in the order** — it sorts last, with the other
test cases that have none, oldest first. Something that has just arrived lands
at the end, where a tester looks for it.

**If a test case's place is lost** — the same. Nothing becomes unsorted. What is
in the folder is on the screen, in the order the places give.

**If a tester copies a test case file by hand under a new name** — it becomes a
second, separate test case. The file name is the identity, so both are read and
both get a number.

**If a test case file is not named the way Testin names them** — Testin keeps
the name written inside it instead. That is a file Testin did not write, and
inventing an identity for it would be worse than believing what it says.

**If two testers add a test case at the same time** — both are kept. There is no
shared counter to contend on, and adding never rewrites a neighbor. If the two
land in the same place, the older one is drawn first.

## Why it works this way

Three separate places used to work the number out. A filter renumbered the cards
from one, so a test set of 40 test cases read as a test set of 12. There is now
one owner, and rule 26 is the whole reason it exists.

---

[Documentation](../README.md) › [Inside Testin](main.md)
