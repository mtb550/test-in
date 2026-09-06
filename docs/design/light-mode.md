[Documentation](../README.md) › [Design](design.md) › Light mode

# Light mode

Light mode is a separate window that stays above every other window. It shows
**one test case at a time**. The tester can work in the application under test
with IntelliJ minimized, and still record a verdict without switching windows.

| | |
|---|---|
| **Area** | [Design](design.md) |
| **Part of Testin** | The test run editor. Light mode is one of its windows |
| **Read with** | Nothing yet. The test run editor's business and system requirements are not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Answers** | Why this window is shaped the way it is, and what every part of it does |
| **State** | **Written.** Built and shipped — [#13](https://github.com/mtb550/test-in/issues/13), closed |
| **Checked against** | `main` at `206c9744`, 6 September 2026 — read class by class against the built code |
| **Points at** | No rules yet. The business requirements do not describe this window ([#72](https://github.com/mtb550/test-in/issues/72)) |

This document describes the window as it was built. It also says where the
build differs from what was first drawn.

---

## What it is for

A tester running a test case by hand is not looking at the IDE. They are in a
browser, or an app, or on a phone, doing what the test case says. The test case
is behind the window they are working in. Every verdict costs them a window
switch. They find IntelliJ, find the test run, find the row, click, and come
back.

Light mode removes that switch. The window stays above the application under
test. The three verdicts are one keystroke away.

**It is a window of its own, not a panel inside the IDE.** That is the whole
requirement. A panel disappears when the IDE is minimized. This window does
not, and staying visible is the reason it exists.

---

## Where it opens from

One button on the run editor toolbar. It sits beside the Start and Stop
buttons it belongs with. Its icon is a sun, borrowed from the IDE's own set of
icons. The icon names the mode, not the action.

The design asked for a picture of a window holding a smaller window in its
corner. The IDE has no ready-made icon for that. The two that came closest were
both wrong: one points its arrow the wrong way, and the other is drawn so
faintly it would have looked permanently disabled. So the icon is borrowed
rather than drawn, which is what
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
Login test case, mid-way through Cycle 2. The window is about 420 pixels wide to start with.

### Before the tester presses Start

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

The window would contradict itself if it showed a test case and offered the
three verdicts while telling the tester to press Start. The tester would be
right to ignore the prompt and start judging. This is **stricter than the run
editor**. The run editor records a verdict whether or not execution is running.
The difference is deliberate. A test case judged before Start carries a zero
duration. Here that cannot happen, because there is nothing to judge until the
test run is going.

### While tests are running, showing the test case alone

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

### While tests are running, with the details open

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

### 1. The pin button, pressed while the window stays on top

It is on by default, because that is the point of the window.

This design drew one icon that fills in. It was an outline when the window may
fall behind other applications, and solid when it stays on top. The build uses
the IDE's own pin icon instead, on the rounded gray patch the IDE draws behind
any pressed toolbar button. The meaning is the same. The shape is the IDE's.
That is why it is not two icons that swap.

The window is a real top-level frame, not a tool window. So it survives IntelliJ
being minimized.

### 2. Start, then Stop

One button that swaps between Start and Stop. It uses the run editor toolbar's
own two icons, read from one place, so the toolbar and this window can never
end up different. A tester who presses Start in the toolbar and Stop in this
window is pressing the same two buttons, and should be looking at them.

It had drawn the IDE's arrow for running *code*. The toolbar had already moved
away from that icon, because a tester running a test case by hand runs no code.
Stop is a pause symbol rather than a square, because a paused test run is what
Stop leaves behind.

### 3. The menu that chooses what the window shows

Four checkboxes decide what the window shows:

- the test set name
- the durations
- the verdict buttons
- the status bar

It carries the toolbar's own Details icon, a tick box with a tick in it. It is the same picture in the IDE and in the
window, and it is a picture of exactly what it opens.

### 4. Run and counter

This says which test run is being executed, and how far through it the tester
is. `3 / 6` counts the test cases the editor is showing. With a filter on, it
counts only what the filter left. The window reads the same list of test cases
the grid shows, so the two can never disagree about which test case is next.

### 5. The description, large

This is the one thing that cannot be turned off. It is drawn at a size the
tester can read from across a desk.

### 6. The test set name, in small capitals above the description

It is on by default, because a description alone can be ambiguous. "Sign in with
a correct username and password" could sit under a Login test set. It could also
sit under a Checkout test set testing guest sign-in.

It can be hidden. A tester working through a single test set reads the same
word on every test case, and a word that never changes is noise. It helps when
a test run covers several test sets. It does not when the test run covers one.

### 7. The two clocks

The test case reads `00:41`. The test run reads `00:12:41`. They sit in the
bottom strip, small, one at each end, with no labels. The test case duration
is on the left. The test run duration is on the right.

They were first drawn as two large boxes, with the running one in the highlight
color. That turns them into a stopwatch. A tester watching a number climb
hurries, and a hurried tester is what this window exists to prevent.

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
description is large and dark. The expected result is smaller, lighter and
gray, directly beneath it. A tester reading two lines in that order does not
need to be told which is which. The word *Expected* would cost a line, in a
window that is only as tall as what it shows.

It cannot be turned off. It was taken off the view menu while the window was
being built, for the same reason the description was never on it.

### 9. Details open and close on the keyboard

`Ctrl+D` shows the four remaining fields: steps, test data, pre-conditions and
tags. `Ctrl+H` hides them. There is no button. A control that sits on screen
permanently, to be pressed twice a session, is wasted space. This window is
built to have none.

**Two keys rather than one.** A single key that both shows and hides depends on
what the window is doing now. So the tester has to look before they can know
what pressing it will do. Show and hide always do what they say.
That matters most when the tester is looking at the application under test
rather than at this window. The window remembers which it was showing.

### 10. The status bar, which names every key

`Ctrl+D` Details, `Ctrl+H` Hide, `Escape` Close, `P` Passed, `F` Failed, `B`
Blocked.

**This is where `Ctrl+D` and `Ctrl+H` are taught.** Removing the details button
left two shortcuts that nothing on screen mentioned. That was the one real cost
of the change, and this row pays it back. The row is not a new idea.
Every Testin dialog already carries a row like this one. That is where the
wording and the spacing come from.

**Its background is the title bar's, not the body's.** The window uses two
shades, and they mean something. The working area is the pale one. That is the
test case, the verdict buttons and the clocks. The frame around it is the gray
one. That is the title bar and this row. So the verdict row and the status bar
look like the two separate things they are, rather than one band split by a
hairline.

**It can be turned off in two places.** It is a checkbox in the view menu, so a
tester who has learned the keys can reclaim the row in this window. It is also a
setting: *Settings → Testin → "Show keyboard shortcuts in dialogs"*. That
setting turns the strip off in every Testin dialog at once. Knowing the keys is
a fact about the tester, not about 28 separate dialogs. One piece of code draws
this row everywhere, so the setting reaches every dialog at once.

The failure form is the one exception. Its **Save** and **Cancel** buttons are
gone, so this row
is the only place `Enter` and `Escape` are written down. The window forces the
row back for that one state.

**One line, always.** It never wraps and never scrolls. Whatever fits is shown,
and the rest is simply not there. So the order was chosen, not left to chance.
The three keys with no button to teach them come first. The three that
fall off a narrow window are exactly the three with buttons sitting above them.
Making the window wider brings them back, which gives the window's edge a
second job.

### 11. The verdict bar

Three buttons, each printing its own key, all in one color. `P`, `F` and `B` are
not new keys. The three verdicts already answer to them everywhere else in
Testin. That is why a fourth verdict will appear here without this window
changing. The fourth is Out Of Scope, once
[#10](https://github.com/mtb550/test-in/issues/10) is done. The words tell the
three apart. Color would only say "button".

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

**The verdict buttons and the clocks are one strip, with two halves that switch
on and off.** Turn the buttons off, and the clocks stay where they are. Turn
the clocks off, and the buttons close up over them. Turn both off, and the
whole strip is gone, leaving the body sitting directly on the status bar.
Nothing is left behind as an empty band, because the window's height is its
content.

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

**The failure form overrides two of the four.** Pressing `F` shows the actual
result, the severity, the priority and the error box, whatever is hidden. If a
verdict needs detail, the window must show that detail, whatever is switched
off.
The verdict buttons go while the form is open. The test case is already judged,
and the form is the only thing left to do. The status bar comes back,
because it is what says how the form is finished.

---

## What happens when a test case fails

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

### 12. The test case stays above the form

The description and the expected result do not move or shrink. Writing down what
actually happened is a comparison against what should have happened. Hiding
either half while the tester types is the one thing this form must not do.
`Ctrl+D` and `Ctrl+H` do not apply here. A form cannot be collapsed while it is
waiting to be filled in.

**The form zooms with it.** The wheel exists so the window can be read from
where the tester is sitting. A form they then had to lean in to type into would
have moved the problem, not solved it. Each part remembers the size it was
built at, and zoom multiplies that starting size. If it multiplied the size on
screen instead, every turn of the wheel would grow on top of the last.

### 13. The same four fields as the run editor's failure dialog

The four fields are the actual result, the bug severity, the bug priority and
the error box. They are exactly the fields the run editor's own failure dialog
asks for, with the same starting values and the same wording. A failure
recorded here and one recorded in the run editor are the same record.

### 14. Inside the window, in a box it already has

There is no second dialog. `Ctrl+D` fills the details box with steps, test data,
pre-conditions and tags. While a failure is being written, that same box holds
these four fields instead. Nothing is added to the window, and nothing is taken
away. One box swaps what it is showing, and so does the verdict strip below it.

That is also the decision the `F` key forces. The run editor's own failure
dialog belongs to the IDE window, and blocks it until it is closed. Opening it
would bring IntelliJ to the front. That is exactly what this window exists to
avoid. The feature would defeat itself on its most common verdict.

### 15. Real radio buttons, the same ones the run editor uses

This design drew small rounded buttons instead. The argument was that seven
radio buttons and their labels would not fit across 400 pixels. They do fit.
The build uses real radio buttons, and the four fields are written down once
and shared with the run editor's own dialog. Two looks for the same four fields
is exactly what that sharing exists to prevent. The choices come from one list
each, so a new severity or priority appears here on its own.

### 16. Ctrl+V belongs here and nowhere else

The field says so, rather than the status bar. Its gray hint text reads *"paste
error or exception or screenshot.."*. That is the same sentence, in the place
the tester is already looking.

This removes a way to lose work. Passing a test case clears everything recorded
about a failure on it. So pasting evidence onto a test case, and then pressing
`P`, would have destroyed the evidence. The run editor asks first. A window
where one keystroke records a verdict cannot ask. So evidence only ever exists
inside a form that `Enter` saves and `Escape` throws away.

### 17. The verdict buttons disappear while the form is open

They were first replaced by a **Cancel** button and a **Save & next** button.
Those are gone too. `Enter` and `Escape` already do the same, and two buttons
repeating them cost a row, in a window whose whole argument is that it is
small. The clocks keep running, because the tester is still on this test case
while they write the failure up. The status bar names the two keys that work
here.

That is the one thing the removal had to pay for. With no buttons, the status
bar is the only place `Enter` and `Escape` are written down. So while a form is
open, that row is shown whatever the view menu says.

### 18. Escape puts the test case back to no verdict

A tester who presses `F` by mistake gets the test case back unjudged. They do
not get a Failed with an empty actual result. Only Save records anything.

---

## Zoom

The scroll wheel over the window makes the text bigger and smaller, **without
holding `Ctrl` or any other key**. It changes this window and nothing else. Not
the grid behind it. Not the details panel. Not the IDE's editors.

**The text changes size. The window's furniture does not.** Four things change
size: the test set name, the description, the expected result and the detail
fields. The title bar, the verdict buttons and the clocks stay the same at
every zoom. Zoom exists so the
test case can be read from where the tester is sitting. Making the icons and the
clocks bigger would only cost the screen space they were trying to free.

**It is this window's own size, and nothing else's.** That is a deliberate
departure from how the rest of the plugin zooms. The reason is the whole point
of the feature. The tester is away from the IDE. They are reading a small window
on the far side of a screen, while working in another application. How large
that window needs to be has nothing to do with how large a grid should be on a
monitor they are not looking at.

**So it must not reuse the plugin's shared zoom.** That is the obvious thing to
reach for, and the wrong one, because it puts a font size on every open editor
in the IDE. This is written down because a later reader will find two pieces of
wheel-zoom code and try to merge them.

**It opens at the size the tester is used to, then goes its own way.** That
happens every time, not just the first. The size is read from the IDE's editor font as the window opens. So a tester who changes the
IDE font gets a window that agrees with it. The zoom on top of that is the
tester's own. It is remembered per machine, alongside the position, the width
and the four view toggles. It used to go back to the normal size every time the
window opened, which meant setting it again each time.

The size is asked for each time, not fixed once. The editor font is a setting,
and a tester can change it while the IDE is running. A fixed size would freeze
whichever size happened to be set when the window first opened.

**Everything in the window is that size, plus or minus a little.** Call it the
base size.

- the description is three points larger, and bold
- the expected result and every detail are the base size
- labels and the test set name are two points smaller

No text ever goes below 8 points, which is where the rest of the plugin stops
too. One number moves, and everything moves together. So the layout cannot come apart at a size
nobody tested. The expected result and the detail values share a size on
purpose. They are the same kind of thing being read, and setting them apart said
they were not.

**No modifier, in the end.** This design argued for `Ctrl` and the wheel, on
convention. That is what the IDE means by zoom, and what every other Testin
screen listens for. The build went the other way, for three reasons. The window
has nothing else a wheel could mean. Its height is fixed to what it shows, so
there is nothing to scroll. Anything inside it that does scroll would take the
wheel first anyway. So this is the one window in the plugin where zoom needs no
other key held down.

---

## What each key and button does

| Gesture | What happens |
|---|---|
| **Start** | Sets the test run to In Progress. Begins timing. Shows the first test case that has no verdict. It does exactly what the toolbar's Start button does. Opening the window does not start the test run, because opening a window should not change a test run's status. |
| **Stop** | Ends the execution flow. The test run keeps every verdict already recorded. Only the clock stops. |
| **`P` / `B`** | Records the verdict on the current test case. Advances to the next test case that has not been judged. |
| **`F`** | Opens the failure capture in place. `Enter` saves and advances. `Escape` returns, with the test case still unjudged. |
| **`Ctrl+V`** | Pastes an image or text into the error box, on the failure form. It does nothing when no failure form is open, so evidence cannot be attached to a test case that is about to be passed and cleared. |
| **`Escape`** | Closes the window. The test run is untouched. Reopening returns to the first unjudged test case. |
| **`Ctrl+D`** | Shows the detail fields. It does nothing if they are already shown. |
| **`Ctrl+H`** | Hides them. It does nothing if they are already hidden. |
| **Wheel** | Zooms the test case and the failure form, inside this window only, with no modifier. |
| **Drag an edge** | Width only. The left and right edges resize. The top and bottom do not. Height is whatever the content needs. |
| **Show details, or a view toggle** | The window grows or shrinks to fit. That is the only way its height changes. It changes immediately, rather than leaving a gap or a scrollbar. |
| **Drag the bar** | Moves the window. Its position, width, zoom level and four view toggles are remembered per machine, not per project. |
| **Light Mode pressed again** | The window closes. The editor carries on from wherever the test run got to. Nothing is saved or discarded on the way out, because every verdict was written as it was recorded. |
| **Window closed any other way** | The button pops back out. That covers `Escape`, the project closing, and the run editor's tab closing. The button shows whether the window is open, rather than remembering that someone opened it. |
| **Verdict set in either view** | The other view follows it. Both read the same test run, and neither keeps a copy. So a test case judged in light mode is already judged in the grid behind it, and one judged in the grid moves the window on. |
| **Last test case judged** | The test run finishes itself. Testin marks it **Completed** the moment nothing is left waiting. So this is not something the window decides. The window has already closed, so nothing is left to announce it. |
| **Wrong verdict recorded** | Press Light Mode to return to the editor. Correct the test case there. Press it again to carry on. The window advances only forward. The grid is where a test run is edited. |
| **Test run reaches Completed or Closed** | The window closes, and the Light Mode button goes gray. Both ask the same question Start already asks. So a signed-off test run cannot be reopened in light mode, and cannot be started. |
| **Project or IDE closes** | The window closes with the project. Nothing is left running behind it. |
| **A clock is read** | The whole plugin uses two formats, not four. A test run always shows hours, minutes and seconds, even at zero, so the number never suddenly gets wider. A test case shows minutes and seconds, here, in the grid and in an exported sheet. Milliseconds are measured and saved, and never shown. So a fast automated test case reads `00:00` in the grid where it used to read `84ms`. The real figure is still in the file. |
| **A test case nobody timed** | Blank, in the grid and the sheet. A verdict set from the menu, or set on many test cases at once, was never timed, and `00:00` would claim a measurement nobody took. A test case sitting in front of a tester at zero is the opposite: it has only just started. So light mode's clock shows `00:00`, rather than going blank for its first second. |

---

## Why the window is built this way

Each is a choice, not a default.

### It is a window in Testin, not a second plugin

One download, one license. The window reads the test run through the same code
the rest of Testin uses. A separate plugin would need its own way to find a test
run on disk, and having two ways is exactly what Testin's file rule exists to
prevent. It can still be split out later, once its edges are clear.

### It needs a test run open

The window executes a test run that already exists. Exploratory testing creates
test cases as it goes. That is a different feature, with a different shape.
Folding it in here would make both worse.

### The toolbar button stays pressed while the window is open

The button is a toggle, not an opener. It is pressed for exactly as long as the
window is there. It checks whether the window is open, rather than remembering
that someone opened it. So it pops back out on its own however the window
closes: by `Escape`, by the test run being signed off, by the editor's tab
closing, or by the project closing. Nobody had to write four separate pieces of
code for that. Start and Stop already work the same way, and the second copy of
an answer is always the one that ends up wrong.

### The window asks the test run whether it is running

Whether Start or Stop shows is asked of the test run each time. The window never
remembers it. A remembered answer is a second thing to keep in step, and the one
that fell out of step would leave a Stop button on a finished test run. Light
mode is a second view of one test run, so it asks the same question the editor
asks, and the two can never disagree.

### It only goes forward

A verdict moves to the next test case with no verdict. There is no way back to
the last one. That is the window's whole shape: one test case, one decision, on
to the next. The run editor's own menu goes both ways, because that is where a
test run is edited rather than run.

Correcting a verdict is an editing gesture, so it happens where editing lives.
The toolbar toggle makes that cheap. The tester presses Light Mode to drop back
into the grid, fixes the test case, and presses it again to carry on. A Back
button here would turn this into a second, smaller editor, inside a window
built to be no such thing.

### Closing it abandons nothing

Every verdict is written as it is recorded. So there is no session to resume.
Reopening returns to the first test case with no verdict. That is why `Escape`
needs no confirmation.

### There is no finished state, because the window is gone

A test run that has judged every test case finishes itself. Testin marks it
**Completed** as soon as nothing is left waiting. Completed is the end, and
three things already ask whether a test run has reached it:

- Start refuses
- the Light Mode button grays out
- the window closes

One question, three answers. There is no fourth state to draw, and no "well
done" screen to write. A window that outlived its test run would offer verdicts
that nothing could take.

### The view menu chooses parts, not fields

[#13](https://github.com/mtb550/test-in/issues/13) asks for a control over which
test case fields appear. The menu chooses between the things the window itself
holds instead. The four fields behind Details stay fixed: steps, test data,
preconditions and tags. Four switches over parts of a window cannot grow into
more. A list of 18 switches over test case fields is a settings screen, inside a
window built to have none. If the wrong four are behind Details, changing those four is
the fix.

### Details open with a key, and that costs something

Removing the button removes a permanent row. The window's whole argument is that
it holds one test case and nothing else. Two keys also beat one toggle. `Ctrl+D`
and `Ctrl+H` always do what they say. A single toggle key does the opposite of a
state the tester would have to check first.

The status bar teaches them. It is the row that answered this. Before it,
removing the button left two shortcuts that nothing on screen mentioned. A
tester who did not read the documentation would never have found them.

**One warning.** `Ctrl+D` already means something else in the create-test-case
dialog. It is a different window, so the two never clash, but they do mean
different things. The keyboard reference should say so, rather than leave a
tester to find out.

### The tester sets the width. The content sets the height

Only the side edges resize. Height is never dragged. It is exactly what the
window is holding. It changes when the tester opens details, or turns a part
off. Those are the gestures that actually change how much there is to show.

That removes two problems, not one. A window taller than what it shows has a
strip of empty gray under the verdict buttons, and looks broken. A window
shorter than what it shows needs a scrollbar, and a scrollbar in a window
showing one test case means scrolling to read the thing you came for. Neither
happens, because the tester cannot set the height.

**Nothing stops the window growing past the screen, and this document used to
promise it would.** It said the window would stop at the edge of the screen and
let the test case scroll. Nothing does that. The window simply grows to fit what
it holds, so a very long description at a large size makes a window taller than
the screen. Stopping it would cut the test case off, which is worse. Letting it
scroll puts a scrollbar in the one window built so nothing has to be scrolled.
So it is written down rather than fixed, as finding 54 in
[#66](https://github.com/mtb550/test-in/issues/66).

### This window keeps its own zoom

Everywhere else in Testin, zoom is one number: the IDE's editor font size. It
changes the font everywhere in the IDE, so the grid, the details panel and every
editor move together. This window does not join them.

The departure is the feature. A tester using light mode is not looking at the
IDE. That is why the window exists. One size makes a test case readable across a
desk, over the top of a browser. Another makes a grid readable on a monitor
nobody is facing. The two have nothing to do with each other. Moving both
together would mean every zoom here quietly rearranged the work waiting behind
it.

### The clocks never compete with the test case

The time is recorded for the report. It is not shown to hurry anybody. It stays
small and gray at the bottom, and never takes the highlight color, which is
kept for the test case being worked on. A
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

## What this window changed in the rest of Testin

> **This one section is for whoever changes the code.** A tester can skip it.
> Everything above is about what the tester sees. This is about what building
> the window forced other parts of Testin to share, so the next person to open
> those parts knows why they are shaped the way they are.

| Before | Now |
|---|---|
| Setting a verdict only worked from the run editor, and refused anything else. | Setting a verdict takes a test run, a test case and a status. The editor is one caller among others. |
| Every dialog drew its own key caps, and decided for itself whether to show the row of shortcuts. | One piece of code draws a key cap for all **28 files** that name one, and one place answers "is this row shown". So one setting reaches every Testin dialog. |
| The five settings that turn a text box into a paragraph a tester reads were written out five times: in the card, the details rows, the steps, the title and here. | One place owns them. Gathering the five turned up a bug: `Tab` stopped on three paragraphs nobody can type into, and did nothing. It was fixed everywhere at once. |
| The two grays a list alternates between were written once in the grid, and written again in the card. | One place owns them. The failure form's two boxes borrow the darker of the two, rather than adding a fifth gray. That is why they look sunken against the gray frame. |
| Four ways of writing a duration: milliseconds under a second, milliseconds after the seconds above it, and two different clock formats. | Two. A test run shows hours, minutes and seconds. A test case shows minutes and seconds. Milliseconds are still measured and saved, and never shown. |
| Nothing in the plugin had ever opened a window of its own, so nothing knew how to close one properly. | One way of closing a window, written once. It belongs to the project and closes with it, and it is correct on several monitors and on high-resolution screens. |
| Every zoom in the plugin changed the IDE's editor font, which changes every open editor. | A text size this window owns, so the wheel here changes nothing outside it. |
| Nothing was announced inside the plugin when a tester recorded a verdict by hand. | **Still not built.** A verdict is still a direct call from the run editor, and this window is refreshed by it. This row is what the window would have wanted, not what it got. |

---

## Where the plugin does not match this design

| Drawn | Built |
|---|---|
| The failure form's status bar offered `Ctrl+V` Paste evidence | Not shown. The error box's own hint text already says it, and `Ctrl+V` still works, because pasting into a text box works anyway. |
| Small rounded buttons for severity and priority | Real radio buttons, shared with the run editor's dialog. |
| A pin icon that fills in | The IDE's own pressed-button look. |
| `Ctrl` and the wheel | The wheel alone. |
| The counter counts every test case in the test run | It counts what the filter left, which is the same list the grid shows. |
| The title bar had a close button | Removed. Three routes out already exist, and every one is announced. |
| Every change animated | Everything changes instantly. **Decided against**: [#178](https://github.com/mtb550/test-in/issues/178), closed after watching real use. |
| The window stops at the edge of the screen, and the test case scrolls | Nothing does that. Open as finding 54 in [#66](https://github.com/mtb550/test-in/issues/66). |

---

## How it was tested

Almost nothing in this window can be tested automatically. It is a window of
its own, with keys, resizing and a layout all wired by hand. It was tested by
hand instead, in a trial copy of the IDE.

**The first pass found six defects.** No test could have caught any of them.
Three of the six:

- the test case was drawn in the IDE's menu font, not the editor's font
- Start and Stop drew the IDE's arrow for running code
- the window stayed open after a test run was signed off

**Reading the code later found two more.** A right click on a verdict button
recorded that verdict. With one test run open in two editors, the toolbar button
updated the wrong one.

---

*The colors are Testin's own green, red and amber, the same three the verdicts
use everywhere else, and the same blue the reports use.*

---

[Documentation](../README.md) › [Design](design.md) › **Light mode** — the test run editor's business and system requirements are not written yet: [#181](https://github.com/mtb550/test-in/issues/181)
