[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-047

# UC-EDITOR-PANEL-047: See which test cases are automated

> There is no key for this. It is drawn on every card.

**As a** tester, **I want** to see which of my test cases have automation behind
them, **so that** I can tell what a run will actually cover without opening the
code.

The icon that jumps to the generated method now also says whether there is any
automation to jump to. The filter beside it is how a tester sees a whole test
set at once.

## Rules

- **Rule-EDITOR-PANEL-195** — A test case is in one of three states. Its
  generated method has something in it, or it names a method and has none, or it
  is not automated. A method Testin wrote and nobody has filled in is not
  automation: every generated method starts as an empty one.
- **Rule-EDITOR-PANEL-196** — The state is read away from the screen. A test set
  opens at the speed it opened before, and the cards say what they know as the
  answers arrive. A test case nobody has read yet is never reported as not
  automated.
- **Rule-EDITOR-PANEL-197** — Nothing about this is saved. The state is read
  from the code every time the list is drawn, so a method written or deleted
  shows the next time the tester looks.
- **Rule-EDITOR-PANEL-198** — The filter offers the three states and behaves
  like the four filters beside it. Choosing none of them shows everything. It is
  offered in both editors, because a test run holds test cases too.

## What the tester sees

```
┌──────────────────────────────────────────────────────────────┐
│  1  Sign in with a correct username and password   [C] [>]   │
│     High   Regression                                        │
└──────────────────────────────────────────────────────────────┘
```

The two icons appear while the pointer is over the card, as they always have.
The first is the one that jumps to the code, and its shape says what is there:

| | |
|---|---|
| a class icon | the generated method has something in it |
| a class icon with an error mark | the test case names a method and there is none — the automation was written and is gone |
| a hollow class icon | not automated. Either there is no method, or Testin wrote one and nobody has filled it in |

The same icon and the same three shapes are used in the view panel's details.

## The filter

**Automation** joins **Priority**, **Group**, **Module** and **Status** in the
filter popup, with the same three icons beside the three states. Picking one
narrows the list to it, picking two shows both, and picking none shows
everything. **Reset Filters** clears it with the rest.

This is how a tester reads a whole test set at once. The icon answers for the
card under the pointer; the filter answers for all of them.

It is offered in the test set editor and in the test run editor. A test run is
where the question matters most: a run whose test cases have no methods will not
execute much, and the filter is how a tester sees that before starting it.

## Main flow

1. The tester opens a test set.
2. The cards are drawn at once.
3. Testin reads the generated code for every test case in the set, away from
   the screen. That is one class read once, whether the set holds fifty test
   cases or a thousand.
4. Hovering a card shows its state on the navigate icon.
5. To see the whole set at once, the tester opens the filter and picks a state.

## What Testin refuses

**If the IDE has no Java plugin** — the icon is drawn as it always was and no
test case is reported as un-automated. Testin cannot read Java there, and a
tester who cannot generate automation has not failed to write it. The filter is
not offered.

**If a method is there but empty** — the test case is not automated. Testin
writes every method as a name and a TODO comment, so a method on its own means
somebody created the test case, not that somebody wrote the automation.

**If the IDE is still indexing** — the reading waits until it has finished.
Asking sooner answers wrongly.

**If the code cannot be read** — the icon is drawn as it always was and the
reason goes to the log. Nothing on screen claims a test case has no automation
because the reading failed.

---

[Documentation](../README.md) › [The editor panel](main.md)
