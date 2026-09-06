[Documentation](../README.md) › [Design](design.md) › Light mode

# Light mode

Light mode is a separate window that stays above every other window. It shows
**one test case at a time**. The tester can work in the application under test
with IntelliJ minimized, and still record a verdict without switching windows.

| | |
|---|---|
| **Area** | [Design](design.md) |
| **Module** | `RE` — Test run editor. Light mode is one of its windows |
| **Read with** | Nothing yet. The test run editor's business and system requirements are not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Answers** | Why this window is shaped the way it is, and what every part of it does |
| **State** | **Written.** Built and shipped — [#13](https://github.com/mtb550/test-in/issues/13), closed |
| **Checked against** | `main` at `206c9744`, 6 September 2026 — read class by class against the built code |
| **Cites** | No `BR` or `SR` yet; the business requirements do not describe this window ([#72](https://github.com/mtb550/test-in/issues/72)) |

This document describes the window as it was built. It also says where the build
differs from what was first drawn. The package is `org.testin.lightmode`, and it
holds 10 classes.

---

## What it is for

A tester running a test case by hand is not looking at the IDE. They are in a
browser, or an app, or on a phone, doing what the test case says. The test case
is behind the window they are working in. Every verdict costs them a window
switch. They find IntelliJ, find the test run, find the row, click, and come
back.

Light mode removes that switch. The window stays above the application under
test. The three verdicts are one keystroke away.

**It is a real top-level frame, not a tool window.** That is the whole
requirement. A tool window belongs to the IDE frame, and is hidden with it.
Staying visible while IntelliJ is minimized is the reason this window exists.

---

## Where it opens from

One button on the run editor toolbar. It sits beside the execution controls it
belongs with. Its icon is a sun, `AllIcons.MeetNewUi.LightTheme`. The icon names
the mode, not the gesture.

The design asked for a window holding a smaller window in its corner. No stock
icon draws that. `General.OpenInToolWindow` points its arrow *into* the frame,
not out of it. `General.ExpandComponent` ships at half opacity, so it would have
looked permanently disabled beside the buttons it sits with. So the icon is
borrowed rather than drawn. That is what
[#123](https://github.com/mtb550/test-in/issues/123) tracks.

The button is a **toggle**. The tester presses it to open light mode. They press
it again to close the window and carry on in the editor. It stays pressed for as
long as the window is there. It is enabled only while the test run is open. It
grays out the moment the test run reaches Completed or Closed. That is the same
question that disables Start, and the same one that closes the window if it is
open.

---

## The window

The sketches below are at the real size. The content is the sample project's
Login test case, mid-way through Cycle 2. The window is 420px wide by default.

### Before Start: nothing to judge yet

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [ >]  [pin]  [view]            Cycle-2                      6 cases       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│            Press the play button to start test execution                   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

There is no test case at all. The window shows three things: the test run name,
how many test cases it holds, and the prompt. There is no description, there are
no verdict buttons, and there are no clocks.

A window that showed the test case, and offered the three verdicts, while
telling the tester to press Start would contradict itself. The tester would be
right to ignore the prompt and start judging. This is **stricter than the run
editor**. The run editor records a verdict whether or not execution is running.
The difference is deliberate. A test case judged before Start carries a zero
duration. Here that cannot happen, because there is nothing to judge until the
test run is going.

### Executing: the test case alone

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

### Executing: details open

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

### 1. The pin, pressed while it holds

It is on by default, because that is the point of the window.

This design drew one glyph that fills in. It was an outline when the window may
fall behind other applications, and solid when it stays on top. The build draws
the platform's own pressed toolbar toggle instead. It is
`AllIcons.General.Pin_tab`, unchanged, on a rounded pill of
`ActionButton.pressedBackground()`. The idea is the same. The shape is the
platform's rather than ours. That is why it is not two glyphs that swap.

The window is a real top-level frame, not a tool window. So it survives IntelliJ
being minimized.

### 2. Start, then Stop

One control that swaps. It draws the run editor's toolbar icons rather than
pictures of its own. They are `Toolbar.START_MANUAL_EXECUTION_ICON` and
`Toolbar.STOP_EXECUTION_ICON`. They are read from the same two constants
`StopExecutionBtn` reads, so the two surfaces cannot drift. A tester who presses
Start in the toolbar and Stop in this window is pressing the same two buttons.
They should be looking at them.

It had drawn `AllIcons.Actions.Execute`. The javadoc on the toolbar's own
constant records that as the icon the toolbar had already moved away from. It is
the platform's run-*code* arrow, and manual execution runs no code. Stop is a
pause glyph rather than a square, because that is what `Debugger.ThreadFrozen`
draws.

### 3. Details selection

Four checkboxes decide what the window shows:

- the test set name
- the durations
- the verdict buttons
- the status bar

It carries the toolbar's own Details icon, `AllIcons.Actions.Selectall`. That
icon is a checkbox with a tick. It is the same picture in the IDE and in the
window, and it is a picture of exactly what it opens.

### 4. Run and counter

This says which test run is being executed, and where in it. `3 / 6` counts the
test cases the editor is showing. With a filter on, that is the filtered view.
The window reads `getCurrentTestCases()`, the same list the grid walks. So the
two can never disagree about which test case is next.

### 5. The description, large

This is the one thing that cannot be turned off. It is drawn at a size the
tester can read from across a desk.

### 6. The test set, in small caps above it

It is on by default, because a description alone can be ambiguous. "Sign in with
a correct username and password" could sit under a Login test set. It could also
sit under a Checkout test set testing guest sign-in.

It can be hidden. A tester working through a single test set reads the same word
on every test case, and a word that never changes is noise. A test run spanning
several test sets wants it. A test run inside one does not.

### 7. Two clocks, quiet, in two units

The test case reads `00:41`. The test run reads `00:12:41`. They sit in the
bottom strip, at 10px, one at each end, with no labels. The test case duration
is on the left. The test run duration is on the right.

They were two large tiles in the body, with the live one in the accent color.
That is a stopwatch. A tester watching a number climb hurries, and a hurried
tester is what this window exists to prevent.

**They carry no labels.** The right-hand figure is always the larger of the two.
That tells the tester which is which faster than a word would. A label that
never changes is noise. Both figures come from the execution timer
[#27](https://github.com/mtb550/test-in/issues/27) already built. Both are
recorded whether or not they are shown, so nothing is lost by showing them
softly.

**The test run clock carries its hours. The test case clock does not.** That is
what tells them apart. An attempt at hours and minutes for the test run broke
that rule. At five minutes it read `00:05`, beside a test case at four and a
half minutes reading `04:30`. The smaller number was the longer time. Keeping
the seconds, and always carrying the hours, makes the test run clock the wider
and the larger of the pair. That holds whatever either of them shows.

### 8. The expected result, under the description

It is not behind the details toggle, because it is not a detail. The description
says what to do. The expected result says what should happen. A tester who
cannot see the second has no way to judge the first.

They are one thought, so they read as one. **Neither carries a label.** The
action is large, dark and medium weight. The outcome is smaller, lighter and
gray, directly beneath it. A tester reading two lines in that order does not
need to be told which is which. The word *Expected* would cost a line, in a
window whose height is its content.

It cannot be turned off. It left the view menu during the build, on the same
argument that kept the description off the list from the start.

### 9. Details open and close on the keyboard

`Ctrl+D` shows the four remaining fields: steps, test data, pre-conditions and
tags. `Ctrl+H` hides them. There is no button. A control that sits on screen
permanently, to be pressed twice a session, is furniture. This window is built
to have none.

**Two keys rather than one toggle.** A toggle key does whatever the current
state is not. So the tester has to know what the window is showing before they
can predict what pressing it will do. Show and hide always do what they say.
That matters most when the tester is looking at the application under test
rather than at this window. The window remembers which it was showing.

### 10. The status bar, which names every key

`Ctrl+D` Details, `Ctrl+H` Hide, `Escape` Close, `P` Passed, `F` Failed, `B`
Blocked.

**This is where `Ctrl+D` and `Ctrl+H` are taught.** Removing the details button
left two shortcuts that nothing on screen mentioned. That was the one real cost
of the change, and this row pays it back. The row is not a new idea.
`ui.framework.StatusBarShortcut` already puts exactly this on every dialog in
the plugin. That is where the wording and the spacing come from.

**It sits on the title bar's ground, not the body's.** The window has two tones,
and they mean something. The working area is the pale one. That is the test
case, the verdict buttons and the clocks. The frame around it is the gray one.
That is the title bar and this row. So the verdict component and the status bar
look like the two separate things they are, rather than one band divided by a
hairline.

**It can be turned off in two places.** It is a checkbox in the view menu, so a
tester who has learned the keys can reclaim the row in this window. It is also a
setting: *Settings → Testin → "Show keyboard shortcuts in dialogs"*. That
setting turns the strip off in every Testin dialog at once. Knowing the keys is
a fact about the tester, not about 28 dialogs. Both questions are answered in
`StatusBarBase`. It is the only class in the plugin that draws such a strip, so
no dialog had to be touched to obey either.

The failure form is the one exception. Its commit buttons are gone, so this row
is the only place `Enter` and `Escape` are written down. The window forces the
row back for that one state.

**One line, always.** It never wraps and never scrolls. Whatever fits is shown,
and the rest is simply not there. So the order is a decision, not a list. The
three keys with nothing else on screen to teach them come first. The three that
fall off a narrow window are exactly the three with buttons sitting above them.
Widening the window brings them back, which gives the width handle a second job.

### 11. The verdict bar

Three buttons, each printing its own key, all in one color. `P`, `F` and `B` are
not new bindings. `TestStatus` already carries them. That is why a fourth status
will appear here without this window changing. The fourth is Out Of Scope, once
[#10](https://github.com/mtb550/test-in/issues/10) lands. The words tell the
three apart. The color would only say "button".

---

## Choosing what shows

Four checkboxes on the title bar, remembered per machine rather than per
project.

- **Test set name**
- **Duration** — both clocks, one entry
- **Verdict buttons**
- **Status bar**

**Four toggles, not a settings screen.** The window has one job. Every one of
these is a thing on the window that a tester can already see. So the list cannot
grow past what the window holds. That is why it is a menu on the title bar
rather than a page in Settings.

**Duration is one entry, not two**, because the two clocks are one line. Hiding
one and keeping the other would leave a lopsided row. It would also leave a
toggle nobody would reach for twice.

**The verdict buttons and the durations are one component, with two halves that
switch.** Turn the buttons off, and the clocks stay where they are. Turn the
durations off, and the buttons close up over them. Turn both off, and the
component is gone, leaving the body sitting directly on the status bar. Nothing
is left behind as an empty band, because the window's height is its content.

**Hiding the verdict buttons costs nothing, because the status bar still names
the keys.** That is what makes the smallest window usable, rather than merely
small. The buttons go. The row that says `P` Passed is still there. Nobody hides
the buttons and thinks the window stopped working. This is also the one case
where the tail of that row matters, because no button is left to teach those
three keys.

**The status bar itself stays on the list.** A tester who has learned the keys
does not need a strip repeating them. In a window this small, a row they never
read is worth reclaiming. Turning off all four is theirs to choose. The failure
form is the one state where it is not. With no buttons on it, that row is the
only place `Enter` and `Escape` are written down. So the window forces the row
back for that state, rather than taking the choice away everywhere.

**Neither the description nor the expected result turns off.** The description
says what to do. The expected result says what should happen. They are one
thought, and a tester who can see only the first has no way to judge it. A
window showing half a test case is not a smaller window. It is a broken one. The
expected result was the second checkbox on this list until the build. Taking it
off is the same argument that kept the description off from the start.

**Remembered with the position.** They are remembered per machine, not per
project. The same tester on the same screen wants the same window, whichever
project they open.

**Failure capture overrides two of the four.** Pressing `F` shows the actual
result, the severity, the priority and the error capture, whatever is hidden. A
verdict that asks for detail cannot be trimmed until the detail is unreachable.
The verdict buttons go while the form is open. The test case is already judged,
and the form is the only thing left to do. The status bar comes back,
because it is what says how the form is finished.

---

## Pressing F

Passed and Blocked are one keystroke. The verdict is recorded, and the window
moves to the next test case. Failed is the one verdict that asks for something
back. A failure nobody described is a failure nobody can act on.

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

### A. The test case stays above the form

The description and the expected result do not move or shrink. Writing down what
actually happened is a comparison against what should have happened. Hiding
either half while the tester types is the one thing this form must not do.
`Ctrl+D` and `Ctrl+H` do not apply here. A form cannot be collapsed while it is
waiting to be filled in.

**The form zooms with it.** The wheel exists so the window can be read from
where the tester is sitting. A form they then had to lean in to type into would
have moved the problem, not solved it. The fields are framework components with
fonts of their own. So the size each part was built at is taken once, and every
zoom multiplies that size. Measured from what is on screen instead, a second
wheel click would compound the first.

### B. The same four fields the IDE asks for

The four fields are the actual result, the bug severity, the bug priority and
the error capture. They are exactly what `FailedResultDialog` collects today,
with the same defaults and the same wording. A failure recorded here and one
recorded in the run editor are the same record. They are not two shapes of one.

### C. Inside the window, in a component it already has

There is no second dialog. `Ctrl+D` fills the details component with steps, test
data, pre-conditions and tags. While a failure is being written, that same
component holds these four fields instead. Nothing is added to the window, and
nothing is taken away. One component swaps what it is showing, and so does the
verdict component below it.

That is also the decision the `F` key forces. `FailedResultDialog` is modal, and
owned by the IDE frame. Opening it would raise IntelliJ. That would put the
tester back where this window exists to keep them from. The feature would defeat
itself on its most common verdict.

### D. Radio buttons, from the framework

This design drew chips. The argument was that seven radio buttons and their
captions would not fit in 400px. The build uses `RadioSelection` unchanged. The
fields are declared once in `FailureFields`, and shared with the run editor's
own dialog. A second look for the same four fields is the divergence that rule
exists to prevent. The row does fit. The choices still come from
`BugSeverity.CHOICES` and `BugPriority.CHOICES`. So a constant added to either
one appears here on its own.

### E. Ctrl+V belongs here and nowhere else

The field says so, rather than the strip. The placeholder reads *"paste error or
exception or screenshot.."*. That is the same sentence, in the place the tester
is already looking.

This removes a way to lose work. Pasting evidence onto a test case at rest, and
then pressing `P`, would have destroyed it. Passing a test case clears its
actual result, its stacktrace and its bug fields. The run editor confirms before
doing that. A window built on single keystrokes cannot. So evidence only ever
exists inside a form that `Enter` saves and `Escape` discards.

### F. The verdicts leave and nothing replaces them

They were replaced by a Cancel button and a Save & next button. Those are gone.
`Enter` and `Escape` already do both. Two buttons restating them cost a row, in
a window whose whole argument is that it is small. The durations keep running,
because the tester is still on this test case while they write the failure up.
The status bar names the two keys this state answers to, instead of the ones it
does not.

That is the one thing the removal had to pay for. With no buttons, the status
bar is the only place `Enter` and `Escape` are written down. So while a form is
open, that row is shown whatever the view menu says.

### G. Escape unsets the verdict

A tester who presses `F` by mistake gets the test case back unjudged. They do
not get a Failed with an empty actual result. Only Save records anything.

---

## Zoom

The scroll wheel over the window zooms it, **with no modifier**. It changes the
size of this window and nothing else. Not the grid behind it. Not the details
panel. Not the IDE's editors.

**The test case zooms. The window does not.** Four things change size: the test
set, the description, the expected result and the detail fields. The title bar,
the verdict bar and the clocks are identical at every zoom. Zoom exists so the
test case can be read from where the tester is sitting. Making the icons and the
clocks bigger would only cost the screen space they were trying to free.

**It is this window's own size, and nothing else's.** That is a deliberate
departure from how the rest of the plugin zooms. The reason is the whole point
of the feature. The tester is away from the IDE. They are reading a small window
on the far side of a screen, while working in another application. How large
that window needs to be has nothing to do with how large a grid should be on a
monitor they are not looking at.

**So it must not reuse `FontSync.attachWheelZoom`.** That helper is the obvious
thing to reach for, and the wrong one. It ends in `applyGlobally`, which by its
own javadoc "puts a font size on the global scheme and on every open editor".
That is exactly what this window must not do. This is written down because a
later reader will see two wheel-zoom implementations and try to merge them.

**It opens at the size the tester is used to, then goes its own way.** That
happens every time, not just the first. The sizes are read from
`FontSync.getBaseFontSize()` as the window opens. So a tester who changes the
IDE font gets a window that agrees with it. The zoom on top of that is the
tester's own. It is remembered per machine, alongside the position, the width
and the four view toggles. It used to reset to 1× on every open, which meant
setting it again every time.

The sizes are read as methods, not frozen into constants. The plugin already had
the reason written down beside `TestStatus`'s lazy colors. The editor font is a
setting that changes while the IDE runs. A constant freezes whichever size was
in force when the class was first loaded.

**Everything in the body is that number, plus or minus a little.**

- the description is `base + 3`, and bold
- the expected result and every detail value are `base`
- labels and the test set are `base − 2`

Every size is floored at 8pt, as the rest of the plugin floors it. One number
moves, and the body moves together. So the layout cannot come apart at a size
nobody tested. The expected result and the detail values share a size on
purpose. They are the same kind of thing being read, and setting them apart said
they were not.

**No modifier, in the end.** This design argued for `Ctrl` and the wheel, on
convention. That is what the IDE means by zoom, and what every other Testin
surface listens for. The build went the other way, for three reasons. The window
has nothing else a wheel could mean. Its height is fixed to its content, so
there is nothing to scroll. A scroll pane inside it takes the event first
anyway, because an event goes to the deepest component that handles it. So this
is the one window in the plugin where zoom needs no modifier.

---

## What each gesture does

| Gesture | What happens |
|---|---|
| **Start** | Sets the test run to In Progress. Begins timing. Shows the first test case that has no verdict. It is the same call the editor's toolbar makes. Opening the window does not start the test run, because opening a window should not change a test run's status. |
| **Stop** | Ends the execution flow. The test run keeps every verdict already recorded. Only the clock stops. |
| **`P` / `B`** | Records the verdict on the current test case. Advances to the next test case that has not been judged. |
| **`F`** | Opens the failure capture in place. `Enter` saves and advances. `Escape` returns, with the test case still unjudged. |
| **`Ctrl+V`** | Pastes an image or text into Error capture, on the failure form. It does nothing at rest. So evidence cannot be attached to a test case that is about to be passed and wiped. |
| **`Escape`** | Closes the window. The test run is untouched. Reopening returns to the first unjudged test case. |
| **`Ctrl+D`** | Shows the detail fields. It does nothing if they are already shown. |
| **`Ctrl+H`** | Hides them. It does nothing if they are already hidden. |
| **Wheel** | Zooms the test case and the failure form, inside this window only, with no modifier. |
| **Drag an edge** | Width only. The left and right edges resize. The top and bottom do not. Height is whatever the content needs. |
| **Show details, or a view toggle** | The window grows or shrinks to fit. That is the only way its height changes. It changes immediately, rather than leaving a gap or a scrollbar. |
| **Drag the bar** | Moves the window. Its position, width, zoom level and four view toggles are remembered per machine, not per project. |
| **Light Mode pressed again** | The window closes. The editor carries on from wherever the test run got to. Nothing is saved or discarded on the way out, because every verdict was written as it was recorded. |
| **Window closed any other way** | The button un-presses. That covers `Escape`, the project closing, and the run editor's tab closing. The button reflects whether the window is open, rather than remembering that it was opened. |
| **Verdict set in either view** | The other view follows it. Both read the same test run, and neither keeps a copy. So a test case judged in light mode is already judged in the grid behind it, and one judged in the grid moves the window on. |
| **Last test case judged** | The test run completes itself. `RunEditor.finishIfEverythingIsJudged` applies Completed the moment nothing is left pending. So this is not something the window decides. The window is gone by then, so there is no bar left to announce it. |
| **Wrong verdict recorded** | Press Light Mode to return to the editor. Correct the test case there. Press it again to carry on. The window advances only forward. The grid is where a test run is edited. |
| **Test run reaches Completed or Closed** | The window closes, and the Light Mode button goes gray. Both ask the same question Start already asks. So a signed-off test run cannot be reopened in light mode, and cannot be started. |
| **Project or IDE closes** | The window is disposed with the project. No leaked frames, no leaked listeners. |
| **A clock is read** | There are two formats across the whole plugin, not four. A test run total is `HH:MM:SS`, with the hours always in front. That is true here and in the run editor's own status bar, because a field that appears at 01:00:00 is a number that jumps. A test case duration is `MM:SS`, here, in the grid and in an exported sheet. Milliseconds are measured, stored and never drawn. So a fast automated test case reads `00:00` in the grid where it used to read `84ms`. The figure is still in the file. |
| **A test case nobody timed** | Blank, in the grid and the sheet. A verdict that came from the context menu or a bulk apply was never timed, and `00:00` would claim a measurement nobody took. A test case in front of a tester at zero is the opposite. It has just been arrived at. So light mode's clock shows `00:00`, rather than blinking out for its first second. |

---

## Decisions this design takes

Each is a choice, not a default.

### It is a window in Testin, not a second plugin

One distribution, one license. The window reads the test run straight from the
indexer's cache. A separate plugin would need its own way to find a test run on
disk. That is the one thing the file-access rule exists to prevent. It can still
be split out later, once its boundary is visible rather than guessed.

### It needs a test run open

The window executes a test run that already exists. Exploratory testing creates
test cases as it goes. That is a different feature, with a different shape.
Folding it in here would make both worse.

### Light mode is a mode, and the toolbar says which one the tester is in

The button is a toggle, not an opener. It is pressed for exactly as long as the
window exists. Pressed *asks* whether the window is open. It does not remember
that it was opened. So four things un-press it without anyone writing four
handlers: `Escape`, the test run being signed off, the editor's tab closing, and
the project closing. It is the rule the execution controls already follow.
`RunEditor.onExecutionStateChanged` reads the test run rather than a flag of its
own, for the reason its javadoc gives. The second copy is always the one that
drifts.

### The window never holds its own execution flag

Which control shows, Start or Stop, is asked of the test run. The window does
not remember it. `RunEditor.onExecutionStateChanged` already owns that question,
and says why in its own javadoc. A second copy is a second thing to keep in
step. The copy that drifted would leave a Stop button on a finished test run.
Light mode is a second view of one execution. So it asks the same question, and
can never disagree with the editor about whether the test run is going.

### It only goes forward

A verdict advances to the next unjudged test case. There is no way back to the
last one. That is the window's whole shape: one test case, one decision, on to
the next. The run editor's own context menu offers both directions, because it
is the place where a test run is edited rather than executed.

Correcting a verdict is an editing gesture, so it happens where editing lives.
The toolbar toggle makes that cheap. The tester presses Light Mode to drop back
into the grid, fixes the test case, and presses it again to carry on. A Back
button here would be a second, smaller editor, inside a window built to hold
none.

### Closing it abandons nothing

Every verdict is written as it is recorded. So there is no session to resume.
Reopening returns to the first test case with no verdict. That is why `Escape`
needs no confirmation.

### There is no finished state, because the window is gone

A test run that has judged every test case completes itself.
`finishIfEverythingIsJudged` applies Completed as soon as nothing is pending.
Completed is terminal, and three things already ask whether a test run is
terminal:

- Start refuses
- the Light Mode button grays out
- the window closes

One question, three answers. There is no fourth state to draw, and no "well
done" screen to write. A window that outlived its test run would offer verdicts
that nothing could accept.

### The view menu chooses parts, not fields

[#13](https://github.com/mtb550/test-in/issues/13) asks for a control over which
test case fields appear. The menu chooses between the things the window itself
holds instead. The four fields behind Details stay fixed: steps, test data,
pre-conditions and tags. Four toggles over parts of a window cannot grow. A list
of 18 toggles over test case attributes is a settings screen, inside a window
built to hold none. If the wrong four are behind Details, changing those four is
the fix.

### Details are a keystroke, and that has a price

Removing the button removes a permanent row. The window's whole argument is that
it holds one test case and nothing else. Two keys also beat one toggle. `Ctrl+D`
and `Ctrl+H` always do what they say. A single toggle key does the opposite of a
state the tester would have to check first.

The status bar teaches them. It is the row that answered this. Before it,
removing the button left two shortcuts that nothing on screen mentioned. A
tester who did not read the documentation would never have found them.

**Note for whoever binds them:** `Ctrl+D` is already
`Shortcuts.CreateTestCaseDescription` in the create-test-case dialog. It is a
different window and a different component, so no handler competes. But the two
now mean different things. The keyboard reference should say so, rather than
leave a tester to discover it.

### The tester sets the width. The content sets the height

Only the side edges resize. Height is never dragged. It is exactly what the
window is holding. It changes when the tester opens details, or turns a part
off. Those are the gestures that actually change how much there is to show.

That removes two failure states, not one. A window taller than its content has a
band of empty gray under the verdict buttons, and looks broken. A window shorter
than its content needs a scrollbar. A scrollbar in a window showing one test
case means the tester has to scroll to read the thing they came for. Neither can
happen if height is not theirs to set.

**There is no screen guard, and this document used to promise one.** It said the
window would stop at the screen edge and let the body scroll. Nothing does that.
`fitHeight` sets the frame to its preferred height, and consults no bounds. So a
very long description at a large zoom makes a window taller than the display.
Clamping alone would silently cut the test case off, which is worse than a tall
window. Scrolling means a scroll pane, in the one window built so nothing has to
be scrolled. So it is recorded rather than built, as finding 54 on
[#66](https://github.com/mtb550/test-in/issues/66).

### This window keeps its own zoom

Everywhere else in Testin, zoom is one number. It is the IDE's editor font size,
pushed to the global scheme. So the grid, the details panel and every editor
move together. This window does not join them.

The departure is the feature. A tester using light mode is not looking at the
IDE. That is why the window exists. One size makes a test case readable across a
desk, over the top of a browser. Another makes a grid readable on a monitor
nobody is facing. The two have nothing to do with each other. Moving both
together would mean every zoom here quietly rearranged the work waiting behind
it.

### The clocks never compete with the test case

Elapsed time is recorded for the report. It is not shown to hurry anybody. It
stays in the bottom strip, at the size of a hint, in the same gray. It never
takes the accent color, which is reserved for the test case being worked on. A
window that puts a climbing number in front of a tester is asking them to go
faster. A tester going faster is the failure this window was built to avoid.

### A verdict color means a verdict was given

Green, red and amber belong to a test case that has been judged. That is what
they mean everywhere else in the plugin: in the grid, in the tree and in every
report. The three buttons at the bottom of this window are offers, not outcomes.
Painting them permanently in all three colors would make color mean "button"
instead. A tester scanning for color would learn nothing from it. They might
read the strip as the test case's status.

So the verdict bar is one color, and the words tell the three apart. Color
returns on the failure form, where **Failed** has actually been chosen. The
window never shows a test case that already carries a verdict, so there is
nothing else for color to say.

---

## What had to change underneath

These are things the code could not do when this window was designed. They also
include what the window forced into shared ownership on its way through. Each
one was a change to something older and wider than this window. The next reader
of those classes should know why they are shaped the way they are.

| Before | Now |
|---|---|
| Every status method on `RunStatusService` took a `RunEditor` and a `JBList`. `applyStatus` opened by refusing anything that was not the editor. | A verdict path that takes a test run, a test case and a status. The editor is one caller of it, rather than its shape. |
| A shortcut was drawn as a filled keycap, spelled out wherever one appeared. Whether the hint strip showed was each dialog's own business. | `ui.framework.Keycap` draws a key for all **28 files** that declare one. `StatusBarBase` answers "is this strip shown" once. So one setting reaches every Testin dialog, without one being touched. |
| The five settings that turn a text area into a paragraph a tester reads were written out five times. They were in the card, the details rows, the steps, the title and here. | `ui.framework.Prose` owns them. Gathering the five found a tab stop on three paragraphs nobody can type into. Tab landed on them and nothing happened. It was fixed everywhere at once. |
| The two grays a list alternates between were constants in the grid. The same two colors were written inline again in the card. | `ui.framework.RowStripe` owns them. The failure form's two fields borrow its odd row, rather than introducing a fifth gray. That is why they read as wells, on a window painted as frame decoration. |
| Four duration formats: milliseconds under a second, a millisecond tail above it, `hh:mm:ss` for a run and the same for a case. | Two formats. A test run total is `HH:MM:SS`, with the hours always in front. A test case is `MM:SS`. Milliseconds are still measured and stored, and never drawn. |
| Nothing in the plugin had ever opened a top-level window. There was no `JFrame`, and no `setAlwaysOnTop`. | One disposal pattern, written once. It is owned by the project and closed with it. It is correct on multi-monitor and high-DPI. |
| Every zoom in the plugin was the IDE's editor font size. `FontSync.attachWheelZoom` ends in `applyGlobally`, which puts a size on the global scheme and on every open editor. | A font scale this window owns, so the wheel here changes nothing outside it. |
| The only message-bus topic was for the automated runner. Nothing was published when a tester recorded a verdict by hand. | **Still not built.** A verdict is still a direct call from `RunEditor.onExecutionStateChanged`. This window is refreshed by it, like the panels before it. This row is what the window would have wanted, not what it got. |

---

## Where the build differs from the original design

| Drawn | Built |
|---|---|
| The failure form's status bar offered `Ctrl+V` Paste evidence | Not shown. The error capture's own placeholder already says it. `Ctrl+V` still works, because it is the text area's own paste. |
| Chips for severity and priority | Real radio buttons, from `RadioSelection`, shared with the run editor's dialog. |
| A pin glyph that fills in | The platform's pressed toolbar toggle. |
| `Ctrl` and the wheel | The wheel alone. |
| The counter counts every test case in the test run | It counts the editor's filtered view, which is the same list the grid walks. |
| The title bar had a close button | Removed. Three routes out already exist, and every one is announced. |
| Every state change animated | Everything snaps. **Declined as not required**: [#178](https://github.com/mtb550/test-in/issues/178), closed after watching for it in use. |
| The window stops at the screen edge and the body scrolls | No such guard exists. Open as finding 54 on [#66](https://github.com/mtb550/test-in/issues/66). |

---

## How it was tested

Almost nothing in this window is reachable from a unit test. It is an
undecorated top-level frame, with hand-bound keys, a hand-installed resize
listener and a layout that measures itself. It has been exercised by hand
instead, in a sandbox IDE, with `./gradlew runIde`.

**The first pass found six defects.** No test could have caught any of them.
Three of the six:

- the test case was written in the UI font rather than the editor's
- Start and Stop drew the platform's run-code arrow
- the window stayed open after a test run was signed off

**A later class-by-class read found two more, by inspection rather than by
running.** A right click on a verdict button recorded that verdict. With one
test run split across two editors, the toggle updated the wrong button.

---

*Colors are the plugin's own: `008000`, `FF0000` and `FFA500` from `TestStatus`,
accent `2E5496` from the report generators.*

---

[Documentation](../README.md) › [Design](design.md) › **Light mode** — the test run editor's business and system requirements are not written yet: [#181](https://github.com/mtb550/test-in/issues/181)
