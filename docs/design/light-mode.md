# Light mode

A separate always-on-top window that shows **one test case at a time**, so a
tester can exercise the application under test with IntelliJ minimized and still
record a verdict without switching windows.

Built and shipped — [#13](https://github.com/mtb550/test-in/issues/13), merged in
`4730de57`, cleaned in `206c9744`. This document describes the window as built and
says where it differs from what was originally drawn.

Package: `org.testin.lightmode`, 10 classes.

---

## Why it exists

A tester running a case by hand is not looking at the IDE. They are in a browser,
or an app, or on a phone, doing what the case says. The test case is behind the
window they are working in, and every verdict costs them a window switch: find
IntelliJ, find the run, find the row, click, come back.

Light mode removes that. The case sits above whatever they are testing and the
three verdicts are one keystroke away.

**It is a real top-level frame, not a tool window.** That is the whole
requirement: a tool window belongs to the IDE frame and is hidden with it, and
staying visible while IntelliJ is minimized is the reason this exists at all.

---

## Where it opens from

One button on the run editor toolbar, beside the execution controls it belongs
with. Its icon is a sun, `AllIcons.MeetNewUi.LightTheme` — it names the mode
rather than the gesture.

The design asked for a window holding a smaller window in its corner and no stock
icon draws that. `General.OpenInToolWindow` points its arrow *into* the frame
rather than out of it, and `General.ExpandComponent` ships at half opacity, so it
would have looked permanently disabled beside the buttons it sits with. Borrowed
rather than drawn, which is what
[#123](https://github.com/mtb550/test-in/issues/123) tracks.

It is a **toggle**: press to open light mode, press again to close it and carry on
in the editor, and it stays pressed for as long as the window is there. It is
enabled only while the run is open and greys out the moment the run reaches
Completed or Closed — the same question that disables Start, and the same one that
closes the window if it is open.

---

## The window

Shown at its real size. Content is the sample project's Login case, mid-way
through Cycle 2. 420px wide by default.

### Before Start — nothing to judge yet

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [ >]  [pin]  [view]            Cycle-2                      6 cases       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│            Press the play button to start test execution                   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

No test case at all: the run name, how many cases it holds, and the prompt. No
description, no verdict buttons, no clocks.

A window that showed the case and offered the three verdicts while telling the
tester to press Start would be contradicting itself, and the tester would be right
to ignore the prompt and start judging. That is **stricter than the run editor**,
which lets a verdict be recorded whether or not execution is running — and
deliberately so: a case judged before Start carries a zero duration, and here that
cannot happen, because there is nothing to judge until the run is going.

### Executing — the case alone

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [||]  [pin]  [view]            Cycle-2                        3 / 6       │
├────────────────────────────────────────────────────────────────────────────┤
│  LOGIN                                                                     │
│  Sign in with a correct username and password                              │
│  The dashboard opens and the account name is shown in the header.          │
├────────────────────────────────────────────────────────────────────────────┤
│      P Passed              F Failed              B Blocked                 │
│  00:41                                                    00:12:41         │
├────────────────────────────────────────────────────────────────────────────┤
│  Ctrl+D Details  Ctrl+H Hide  Esc Close  P Passed  F Failed  B Blocked     │
└────────────────────────────────────────────────────────────────────────────┘
```

### Executing — details open

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [||]  [pin]  [view]            Cycle-2                        3 / 6       │
├────────────────────────────────────────────────────────────────────────────┤
│  LOGIN                                                                     │
│  Sign in with a correct username and password                              │
│  The dashboard opens and the account name is shown in the header.          │
│                                                                            │
│  STEPS           1. Open the sign-in page.                                 │
│                  2. Type the username.                                     │
│                  3. Type the password.                                     │
│                  4. Press Sign in.                                         │
│  TEST DATA       sample.user@example.com / correct-horse                   │
│  PRE CONDITIONS  The account exists and is not locked.                     │
│  TAGS            [ HIGH ]  [ Accounts ]                                    │
├────────────────────────────────────────────────────────────────────────────┤
│      P Passed              F Failed              B Blocked                 │
│  00:41                                                    00:12:41         │
├────────────────────────────────────────────────────────────────────────────┤
│  Ctrl+D Details  Ctrl+H Hide  Esc Close  P Passed  F Failed  B Blocked     │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## What each part is

Numbered left to right along the title bar, then down the window.

### 1. Pin, pressed when it is holding

On by default, because that is the point of the window.

This design drew one glyph filling in — outline when the window may fall behind
other applications, solid when it is staying on top. The build draws the
platform's own pressed toolbar toggle instead: `AllIcons.General.Pin_tab`
unchanged, on a rounded pill of `ActionButton.pressedBackground()`. Same idea, the
platform's shape rather than ours, which is why it is not two glyphs that swap.

The window is a real top-level frame, not a tool window, so it survives IntelliJ
being minimized.

### 2. Start, then Stop

One control that swaps, and it draws the run editor's toolbar icons rather than
pictures of its own — `Toolbar.START_MANUAL_EXECUTION_ICON` and
`Toolbar.STOP_EXECUTION_ICON`, read from the same two constants `StopExecutionBtn`
reads, so the two surfaces cannot drift. A tester who presses Start in the toolbar
and Stop in this window is pressing the same two buttons and should be looking at
them.

It had drawn `AllIcons.Actions.Execute`, and the javadoc on the toolbar's own
constant records that as the icon the toolbar had already moved away from: it is
the platform's run-*code* arrow, and manual execution runs no code. Stop is a
pause glyph rather than a square, because that is what `Debugger.ThreadFrozen`
draws.

### 3. Details selection

Four checkboxes deciding what the window shows — the test set name, the durations,
the verdict buttons and the status bar. It carries the toolbar's own Details icon,
`AllIcons.Actions.Selectall`, a checkbox with a tick: the same picture in the IDE
and in the window, and a picture of exactly what it opens.

### 4. Run and counter

Which run is being executed, and where in it. `3 / 6` counts the cases the editor
is showing, which is the filtered view when a filter is on — the window reads
`getCurrentTestCases()`, the same list the grid walks, so the two can never
disagree about which case is next.

### 5. The description, large

The one thing that cannot be turned off, at a size readable from across a desk.

### 6. The test set, in small caps above it

On by default, because a description alone is ambiguous — "Sign in with a correct
username and password" could sit under Login or under a Checkout set testing guest
sign-in.

Hideable, because a tester working through a single set reads the same word on
every case, and a word that never changes is noise. A run spanning several sets
wants it; a run inside one does not.

### 7. Two clocks, deliberately quiet, and in two units

The case reads `00:41` and the run reads `00:12:41`. Bottom strip, 10px, one at
each end and no labels: the test case duration on the left, the test run duration
on the right.

They were two large tiles in the body with the live one in the accent color, and
that is a stopwatch — a tester watching a number climb hurries, and a hurried
tester is the thing this window exists to prevent.

**Unlabelled**, because the right-hand figure is always the larger of the two,
which tells a tester which is which faster than a word would, and because a label
that never changes is noise. Both come from the execution timer
[#27](https://github.com/mtb550/test-in/issues/27) already built and are recorded
whether or not they are shown, so nothing is lost by showing them softly.

**The run carries its hours and the case does not, and that is what makes them
tellable apart.** An attempt at hours-and-minutes for the run broke exactly that
rule: at five minutes it read `00:05` beside a case at four and a half reading
`04:30`, and the smaller number was the longer time. Keeping the seconds and
always carrying the hours makes the run clock the wider and the larger of the pair
whatever either of them holds.

### 8. The expected result, under the description

Not behind the details toggle, because it is not a detail: the description says
what to do and this says what should happen, and a tester who cannot see the
second has no way to judge the first.

They are one thought, so they read as one, and **neither is labelled**: the action
is large, dark and medium weight; the outcome is smaller, lighter and gray directly
beneath it. A tester reading two lines in that order does not need to be told which
is which, and the word *Expected* would cost a line of a window whose height is its
content.

It cannot be turned off. It left the view menu during the build, on the same
argument that kept the description off the list from the start.

### 9. Details open and close on the keyboard

`Ctrl+D` shows the four remaining fields — steps, test data, pre-conditions and
tags — and `Ctrl+H` hides them. There is no button: a control that sits on screen
permanently to be pressed twice a session is furniture, and this window is built to
have none.

**Two keys rather than one toggle, and that is the better half of the idea.** A
toggle key does whatever the current state is not, so a tester has to know what the
window is showing before they can predict what pressing it will do. Show and hide
always do what they say, which matters most when the tester is looking at the
application under test rather than at this window. The window remembers which it
was.

### 10. The status bar — every key the window answers to

`Ctrl+D` Details, `Ctrl+H` Hide, `Escape` Close, `P` Passed, `F` Failed, `B`
Blocked.

**This is where `Ctrl+D` and `Ctrl+H` get taught.** Removing the details button
left two shortcuts nothing on screen mentioned, which was the one real cost of that
change; this row pays it back. It is not a new idea either —
`ui.framework.StatusBarShortcut` already puts exactly this on every dialog in the
plugin, which is where the wording and the spacing come from.

**It sits on the title bar's ground, not the body's.** The window has two tones and
they mean something: the working area — the case, the verdict buttons, the clocks —
is the pale one, and the chrome that frames it, the title bar and this row, is the
gray one. So the verdict component and the status bar look like the two separate
things they are, rather than one band divided by a hairline.

**It can be turned off, twice over.** It is a checkbox in the view menu, so a
tester who has learned the keys can reclaim the row in this window. It is also a
setting — *Settings → Testin → "Show keyboard shortcuts in dialogs"* — which turns
the strip off in every Testin dialog at once, because knowing the keys is a fact
about the tester rather than about twenty-eight dialogs. Both questions are
answered in `StatusBarBase`, the only class in the plugin that draws such a strip,
so no dialog had to be touched to obey either.

The one exception is the failure form: the commit buttons are gone, so this is the
only statement of `Enter` and `Escape`, and the window forces the row back for that
one state.

**One line, always.** It never wraps and never scrolls: whatever fits is shown and
the rest is simply not there. So the order is a decision rather than a list — the
three keys with no other affordance come first, and the three that fall off a
narrow window are exactly the three with buttons sitting above them. Widening the
window brings them back, which gives the width handle a second job.

### 11. The verdict bar

Three buttons, each printing its own key, all in one color. `P`, `F` and `B` are
not new bindings: `TestStatus` already carries them, which is why a fourth status —
Out Of Scope, once [#10](https://github.com/mtb550/test-in/issues/10) lands — will
appear here without this window changing. The words tell them apart; the color
would only say "button".

---

## Choosing what shows

Four checkboxes on the title bar, remembered per machine rather than per project.

- **Test set name**
- **Duration** — both clocks, one entry
- **Verdict buttons**
- **Status bar**

**Four toggles, not a settings screen.** The window has one job, and every one of
these is a thing on it a tester can already see — so the list cannot grow past what
the window holds. This is why it is a menu on the title bar rather than a page in
Settings.

**Duration is one entry rather than two**, because the two clocks are one line:
hiding one and keeping the other would leave a lopsided row and a toggle nobody
would reach for twice.

**The verdict buttons and the durations are one component with two switchable
halves.** Turn the buttons off and the clocks stay where they are; turn the
durations off and the buttons close up over them; turn both off and the component
is gone, leaving the body sitting directly on the status bar. Nothing is left
behind as an empty band, because the window's height is its content.

**Hiding the verdict buttons costs nothing, because the status bar still names the
keys.** That is what makes the smallest window usable rather than merely small: the
buttons go, the row that says `P` Passed is still there, and nobody hides them and
thinks the window stopped working. It is also the one case where the tail of that
row matters, since there is no longer a button teaching those three.

**And the status bar itself stays on the list.** A tester who has learned the keys
does not need a strip repeating them, and in a window this small a row they never
read is worth reclaiming. Turning off all four is theirs to choose. The one state
where it is not is the failure form — with no buttons on it, that row is the only
place `Enter` and `Escape` are written down — so the window forces it back for that
state rather than taking the choice away everywhere.

**Neither the description nor the expected result turns off.** The description says
what to do and the expected result says what should happen; they are one thought,
and a tester who can see only the first has no way to judge it. A window showing
half a test case is not a smaller window, it is a broken one. The expected result
was the second checkbox on this list until the build; taking it off is the same
argument that kept the description off from the start.

**Remembered with the position.** Per machine, not per project — the same tester on
the same screen wants the same window, whichever project they open.

**Failure capture overrides two of the four.** Pressing `F` shows the actual
result, severity, priority and error capture whatever is hidden — a verdict that
asks for detail cannot be trimmed to the point where the detail is unreachable. The
verdicts go while it is open, because the case is already judged and the form is the
only thing left to do; the status bar comes back, because it is what says how the
form is finished.

---

## Pressing F

Passed and Blocked are one keystroke — recorded, and on to the next case. Failed is
the one verdict that asks for something back, because a failure nobody described is
a failure nobody can act on.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [||]  [pin]  [view]            Cycle-2                        3 / 6       │
├────────────────────────────────────────────────────────────────────────────┤
│  LOGIN · Failed                                                            │
│  Sign in with a correct username and password                              │
│  The dashboard opens and the account name is shown in the header.          │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ set actual result..                                                  │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Bug Severity    ( ) Blocker   ( ) Major   ( ) Minor   (•) Enhancement     │
│  Bug Priority    ( ) High   ( ) Medium   (•) Low                           │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ paste error or exception or screenshot..                             │  │
│  │                                                                      │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  00:41                                                    00:12:41         │
├────────────────────────────────────────────────────────────────────────────┤
│  Enter Save & next                                      Esc Cancel         │
└────────────────────────────────────────────────────────────────────────────┘
```

### A. The case stays above them

Description and expected result do not move or shrink: writing down what actually
happened is a comparison against what should have, and hiding either half while the
tester types is the one thing this form must not do. `Ctrl+D` and `Ctrl+H` do not
apply here — a form cannot be collapsed while it is waiting to be filled in.

**And the form zooms with it.** The wheel exists so the window can be read from
where the tester is sitting; a form they then had to lean in to type into would have
moved the problem rather than solved it. The fields are framework components with
fonts of their own, so the size each part was built at is taken once and every zoom
multiplies that — measured from what is on screen instead, a second wheel click
would compound the first.

### B. The same four fields the IDE asks for

Actual result, bug severity, bug priority and the error capture — exactly what
`FailedResultDialog` collects today, with the same defaults and the same wording. A
failure recorded here and one recorded in the run editor are the same record, not
two shapes of one.

### C. Inside the window, and inside a component it already has

There is one dialog. The details component — the box `Ctrl+D` fills with steps,
test data, pre-conditions and tags — holds these four fields instead while a failure
is being written. Nothing is added to the window and nothing is taken away; one
component swaps what it is showing, and so does the verdict component below it.

That is also the decision the `F` key forces. `FailedResultDialog` is modal and
owned by the IDE frame, so opening it would raise IntelliJ and put the tester back
exactly where this window exists to keep them out of — the feature defeating itself
on its most common verdict.

### D. Radio buttons, from the framework

This design drew chips, on the argument that seven radios and their captions would
not fit 400px. The build uses `RadioSelection` unchanged, because the fields are
declared once in `FailureFields` and shared with the run editor's own dialog — a
second look for the same four fields is the divergence that rule exists to prevent,
and the row does fit. The choices still come from `BugSeverity.CHOICES` and
`BugPriority.CHOICES`, so a constant added to either appears here on its own.

### E. Ctrl+V belongs here and nowhere else

And the field says so rather than the strip — the placeholder reads *"paste error
or exception or screenshot.."*, which is the same sentence in the place the tester
is already looking.

It removes a way to lose work: pasting evidence onto a case at rest and then
pressing `P` would have destroyed it, because passing a case clears its actual
result, stacktrace and bug fields. The run editor confirms before doing that; a
window built on single keystrokes cannot, so evidence only ever exists inside a form
that `Enter` saves and `Escape` discards.

### F. The verdicts leave and nothing replaces them

They were replaced by a Cancel and a Save & next, and those are gone: `Enter` and
`Escape` already do both, and two buttons restating them cost a row in a window
whose whole argument is that it is small. The durations keep running — the tester is
still on this case while they write the failure up — and the status bar names the
two keys this state answers to instead of the ones it does not.

That is the one thing the removal had to pay for. With no buttons, the status bar is
the only place `Enter` and `Escape` are written down — so while a form is open that
row is shown whatever the view menu says.

### G. Escape unsets the verdict

A tester who hits `F` by mistake gets the case back unjudged, not a Failed with an
empty actual result. Only Save records anything.

---

## Zoom

The scroll wheel over the window, **with no modifier**. It changes the size of this
window and nothing else — not the grid behind it, not the details panel, not the
IDE's editors.

**The case zooms. The window does not.** Only the test set, the description, the
expected result and the detail fields change size; the title bar, the verdict bar
and the clocks are identical at every zoom. Zoom exists so the case can be read from
where the tester is actually sitting — making the icons and the clocks bigger would
only cost them the screen space they were trying to free.

**It is this window's own size, and nothing else's.** That is a deliberate departure
from how the rest of the plugin zooms, and the reason is the whole point of the
feature: the tester is away from the IDE, reading a small window on the far side of
a screen while working in another application. How large that needs to be has
nothing to do with how large a grid should be on the monitor they are not looking
at.

**So it must not reuse `FontSync.attachWheelZoom`**, which is the obvious thing to
reach for and the wrong one. That helper ends in `applyGlobally`, which by its own
javadoc "puts a font size on the global scheme and on every open editor" — exactly
what this window must not do. Written here because a later reader will see two
wheel-zoom implementations and try to merge them.

**It opens at the size you are used to, then goes its own way.** Every time, not
just the first: the sizes are read from `FontSync.getBaseFontSize()` as the window
opens, so a tester who changes the IDE font gets a window that agrees with it. The
zoom on top of that is theirs and is remembered per machine, alongside the position,
the width and the four view toggles — it used to reset to 1× on every open, which
meant setting it again every time.

Read as methods rather than frozen into constants, for the reason the plugin already
had written down beside `TestStatus`'s lazy colors: the editor font is a setting that
changes while the IDE runs, and a constant freezes whichever size was in force when
the class was first loaded.

**Everything in the body is that number plus or minus a little.** Description
`base + 3` and bold, the expected result and every detail value `base`, labels and
the test set `base − 2`, floored at 8pt as the rest of the plugin floors it. One
number moves and the body moves together, so the layout cannot come apart at a size
nobody tested. The expected result and the detail values share a size deliberately:
they are the same kind of thing being read, and setting them apart said they were
not.

**No modifier, in the end.** This design argued for `Ctrl` and the wheel, on
convention: that is what the IDE means by zoom and what every other Testin surface
listens for. It went the other way, because the case for convention was the weaker
half — the window has nothing else a wheel could mean, its height is fixed to its
content so there is nothing to scroll, and a scroll pane inside it takes the event
first anyway, since an event goes to the deepest component that handles it. So it is
the one window in the plugin where zoom needs no modifier.

---

## Behaviour

| Gesture | What happens |
|---|---|
| **Start** | Sets the run to In Progress, begins timing and shows the first case that has no verdict. The same call the editor's toolbar makes. Opening the window does not start it — opening a window should not change a run's status. |
| **Stop** | Ends the execution flow. The run keeps every verdict already recorded; only the clock stops. |
| **`P` / `B`** | Records the verdict on the current case and advances to the next one that has not been judged. |
| **`F`** | Opens the failure capture in place. `Enter` saves and advances; `Escape` returns with the case still unjudged. |
| **`Ctrl+V`** | Pastes an image or text into Error capture, on the failure form. It does nothing at rest, so evidence cannot be attached to a case that is about to be passed and wiped. |
| **`Escape`** | Closes the window. The run is untouched; reopening returns to the first unjudged case. |
| **`Ctrl+D`** | Shows the detail fields. Does nothing if they are already shown. |
| **`Ctrl+H`** | Hides them. Does nothing if they are already hidden. |
| **Wheel** | Zooms the test case and the failure form inside this window only, with no modifier. |
| **Drag an edge** | Width only. The left and right edges resize; the top and bottom do not. Height is whatever the content needs. |
| **Show details, or a view toggle** | The window grows or shrinks to fit. That is the only way its height changes, and it changes immediately rather than leaving a gap or a scrollbar. |
| **Drag the bar** | Moves the window. Its position, width, zoom level and the four view toggles are remembered per machine, not per project. |
| **Light Mode pressed again** | The window closes and the editor carries on from wherever the run got to. Nothing is saved or discarded on the way out, because every verdict was written as it was recorded. |
| **Window closed any other way** | Escape, the project closing, the run editor's tab closing: the button un-presses. It reflects whether the window is open rather than remembering that it was opened. |
| **Verdict set in either view** | The other follows it. Both read the same run and neither keeps a copy, so a case judged in light mode is already judged in the grid behind it, and one judged in the grid moves the window on. |
| **Last case judged** | The run completes itself. `RunEditor.finishIfEverythingIsJudged` applies Completed the moment nothing is left pending, so this is not something the window decides — and the window is then gone, so there is no bar left to announce it. |
| **Wrong verdict recorded** | Press Light Mode to return to the editor, correct the case there, press it again to carry on. The window advances only forward; the grid is where a run is edited. |
| **Run reaches Completed or Closed** | The window closes, and the Light Mode button goes grey. Both ask the same question Start already asks, so a signed-off run cannot be reopened in light mode and cannot be started. |
| **Project or IDE closes** | The window is disposed with the project. No leaked frames, no leaked listeners. |
| **A clock is read** | Two formats across the whole plugin, not four. A run total is `HH:MM:SS` with the hours always in front — here and in the run editor's own status bar — because a field that appears at 01:00:00 is a number that jumps. A case duration is `MM:SS`, here, in the grid and in an exported sheet. Milliseconds are measured, stored and never drawn, so a fast automated case reads `00:00` in the grid where it used to read `84ms`; the figure is still in the file. |
| **A case nobody timed** | Blank, in the grid and the sheet. A verdict that came from the context menu or a bulk apply was never timed, and `00:00` would claim a measurement nobody took. A case in front of a tester at zero is the opposite — it has just been arrived at — so light mode's clock shows `00:00` rather than blinking out for its first second. |

---

## Decisions this design takes

Each is a choice, not a default.

### It is a window in Testin, not a second plugin

One distribution, one licence, and it reads the run straight from the indexer's
cache. A separate plugin would need its own way to find a run on disk, which is the
one thing the file-access rule exists to prevent. It can still be split out later,
once its boundary is visible rather than guessed.

### It needs a run open

The window executes a run that exists. Exploratory testing that creates cases as it
goes is a different feature with a different shape, and folding it in here would
make both worse.

### Light mode is a mode, and the toolbar says which one you are in

The button is a toggle rather than an opener, and it is pressed for exactly as long
as the window exists. Pressed *asks* whether the window is open; it does not
remember that it was opened — so Escape, the run being signed off, the editor's tab
closing and the project closing all un-press it without anyone writing four
handlers. It is the rule the execution controls already follow:
`RunEditor.onExecutionStateChanged` reads the run rather than a flag of its own, for
the reason its javadoc gives, and the second copy is always the one that drifts.

### The window never holds its own execution flag

Which control shows — Start or Stop — is asked of the run, not remembered by the
window. `RunEditor.onExecutionStateChanged` already owns that question and says why
in its own javadoc: a second copy is a second thing to keep in step, and the one
that drifted would leave a Stop button on a finished run. Light mode is a second
view of one execution, so it asks the same question and can never disagree with the
editor about whether the run is going.

### It only goes forward

A verdict advances to the next unjudged case and there is no way back to the last
one. That is the window's whole shape: one case, one decision, on to the next — and
the run editor's own context menu offers both directions precisely because it is the
place where a run is edited rather than executed.

Correcting a verdict is an editing gesture, so it happens where editing lives. The
toolbar toggle is what makes that cheap: press Light Mode to drop back into the
grid, fix the case, press it again and carry on. A Back button here would be a
second, smaller editor inside a window built to have none of one.

### Closing it abandons nothing

Every verdict is written as it is recorded, so there is no session to resume —
reopening simply returns to the first case with no verdict. This is why `Escape`
needs no confirmation.

### There is no finished state, because the window is gone

A run that has judged every case completes itself — `finishIfEverythingIsJudged`
applies Completed as soon as nothing is pending. Completed is terminal, and three
things already ask whether a run is terminal: Start refuses, the Light Mode button
greys out, and the window closes. One question, three answers, no fourth state to
draw and no "well done" screen to write. A window that outlived its run would be a
window offering verdicts nothing could accept.

### The view menu chooses parts, not fields

[#13](https://github.com/mtb550/test-in/issues/13) asks for a control over which
test case fields appear. The menu chooses between the things the window itself holds
instead, and the four fields behind Details stay fixed — steps, test data,
pre-conditions, tags. Four toggles over parts of a window cannot grow; eighteen
toggles over test case attributes is a settings screen inside a window built to have
none. If the wrong four are behind Details, changing those four is the fix.

### Details are a keystroke, and that has a price

Removing the button removes a permanent row from a window whose whole argument is
that it holds one case and nothing else. Two keys also beat one toggle: `Ctrl+D` and
`Ctrl+H` always do what they say, where a single toggle key does the opposite of a
state the tester would have to check first.

They are taught by the status bar, which is the row that answered this. Before it,
removing the button left two shortcuts nothing on screen mentioned and a tester who
did not read the documentation would never have found them.

**Note for whoever binds them:** `Ctrl+D` is already
`Shortcuts.CreateTestCaseDescription` in the create-test-case dialog. Different
window, different component, so no handler competes — but the two now mean different
things, and the keyboard reference should say so rather than leave a tester to
discover it.

### The tester sets the width. The content sets the height

Only the side edges resize. Height is never dragged — it is exactly what the window
is currently holding, and it changes when the tester opens details or turns a part
off, which are the gestures that actually change how much there is to show.

That removes two failure states rather than one. A window taller than its content
has a band of empty grey under the verdict buttons and looks broken; a window
shorter than its content needs a scrollbar, and a scrollbar in a window showing one
test case means the tester has to scroll to read the thing they came for. Neither
can happen if height is not theirs to set.

**There is no screen guard, and this document used to promise one.** It said the
window would stop at the screen edge and let the body scroll. Nothing does that:
`fitHeight` sets the frame to its preferred height and consults no bounds, so a very
long description at a large zoom makes a window taller than the display. Clamping
alone would silently cut the case off, which is worse than a tall window, and
scrolling means a scroll pane in the one window built so nothing has to be scrolled
— so it is recorded rather than built, as finding 54 on
[#66](https://github.com/mtb550/test-in/issues/66).

### This window keeps its own zoom

Everywhere else in Testin, zoom is one number: the IDE's editor font size, pushed to
the global scheme so the grid, the details panel and every editor move together.
This window does not join that.

The departure is the feature. A tester using light mode is not looking at the IDE —
that is why the window exists. The size that makes a case readable across a desk,
over the top of a browser, has nothing to do with the size that makes a grid
readable on a monitor nobody is facing, and moving both together would mean every
zoom here quietly rearranging work waiting behind it.

### The clocks never compete with the case

Elapsed time is recorded for the report, not shown to hurry anybody. It stays in the
bottom strip at the size of a hint, in the same gray, and it never takes the accent
color — which is reserved for the case being worked on. A window that puts a
climbing number in front of a tester is asking them to go faster, and a tester going
faster is the failure this window was built to avoid.

### A verdict color means a verdict was given

Green, red and amber belong to a case that has been judged — that is what they mean
everywhere else in the plugin, in the grid, the tree and every report. The three
buttons at the bottom of this window are offers, not outcomes, and painting them
permanently in all three colors would make color mean "button" instead. A tester
scanning for color would learn nothing from it, and might read the strip as the
case's status.

So the verdict bar is one color and the words tell the three apart. Color returns on
the failure form, where **Failed** has actually been chosen — and the window never
shows a case that already carries a verdict, so there is nothing else for it to say.

---

## What had to change underneath

Things the code could not do when this was designed, and the ones the window forced
into shared ownership on its way through. Kept here because each was a change to
something older and wider than this window, and the next reader of those classes
should know why they are shaped the way they are.

| Before | Now |
|---|---|
| Every status method on `RunStatusService` took a `RunEditor` and a `JBList`, and `applyStatus` opened by refusing anything that was not the editor. | A verdict path that takes a run, a case and a status. The editor is one caller of it rather than its shape. |
| A shortcut was drawn as a filled keycap, spelled out wherever one appeared; and whether the hint strip showed was each dialog's own business. | `ui.framework.Keycap` draws a key for all **28 files** that declare one, and `StatusBarBase` answers "is this strip shown" once — so one setting reaches every Testin dialog without one being touched. |
| The five settings that turn a text area into a paragraph a tester reads were written out in the card, the details rows, the steps, the title and here. | `ui.framework.Prose` owns them. Gathering the five found a tab stop on three paragraphs nobody can type into — Tab landed on them and nothing happened — and fixed it everywhere at once. |
| The two grays a list alternates between were constants in the grid and the same two colors written inline again in the card. | `ui.framework.RowStripe`. The failure form's two fields borrow its odd row rather than introducing a fifth gray, which is why they read as wells on a window painted as frame decoration. |
| Four duration formats: milliseconds under a second, a millisecond tail above it, `hh:mm:ss` for a run and the same for a case. | Two. A run total is `HH:MM:SS` with the hours always in front; a case is `MM:SS`. Milliseconds are still measured and stored and never drawn. |
| Nothing in the plugin had ever opened a top-level window — no `JFrame`, no `setAlwaysOnTop`. | One disposal pattern, written once: owned by the project, closed with it, correct on multi-monitor and high-DPI. |
| Every zoom in the plugin was the IDE's editor font size. `FontSync.attachWheelZoom` ends in `applyGlobally`, which puts a size on the global scheme and on every open editor. | A font scale this window owns, so the wheel here changes nothing outside it. |
| The only message-bus topic was for the automated runner; nothing was published when a tester recorded a verdict by hand. | **Still not built.** A verdict is still a direct call from `RunEditor.onExecutionStateChanged`, and this window is refreshed by it like the panels before it. Listed as what the window would have wanted, not as what it got. |

---

## Where the build differs from the original design

| Drawn | Built |
|---|---|
| The failure form's status bar offered `Ctrl+V` Paste evidence | Not shown. The error capture's own placeholder already says it, and `Ctrl+V` still works — it is the text area's own paste. |
| Chips for severity and priority | Real radio buttons, from `RadioSelection`, shared with the run editor's dialog. |
| A pin glyph that fills in | The platform's pressed toolbar toggle. |
| `Ctrl` and the wheel | The wheel alone. |
| The counter counts every case in the run | It counts the editor's filtered view, which is the same list the grid walks. |
| The title bar had a close button | Removed. Three routes out already exist and every one is announced. |
| Every state change animated | Everything snaps. **Declined as not required** — [#178](https://github.com/mtb550/test-in/issues/178), closed after watching for it in use. |
| The window stops at the screen edge and the body scrolls | No such guard exists. Open as finding 54 on [#66](https://github.com/mtb550/test-in/issues/66). |

---

## Testing

Almost nothing in this window is reachable from a unit test: it is an undecorated
top-level frame with hand-bound keys, a hand-installed resize listener and a layout
that measures itself. It has been exercised by hand instead, in a sandbox IDE via
`./gradlew runIde`.

**The first pass found six defects**, none of which a test could have caught — among
them the case being written in the UI font rather than the editor's, Start and Stop
drawing the platform's run-code arrow, and the window staying open after a run was
signed off.

**A later class-by-class read found two more by inspection rather than by running**:
a right click on a verdict button recorded that verdict, and with one run split
across two editors the toggle updated the wrong button.

---

*Colors are the plugin's own: `008000`, `FF0000` and `FFA500` from `TestStatus`,
accent `2E5496` from the report generators.*
