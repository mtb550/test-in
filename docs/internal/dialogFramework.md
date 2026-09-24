[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-007

# UC-INTERNAL-007: Answer any Testin dialog

> There is no key for this. It is what happens in every dialog Testin opens.

**As a** tester, **I want** every Testin dialog to work the same way, **so that**
I learn it once and never wonder what a new one will do.

Testin opens a lot of dialogs — creating a node, renaming one, ordering it,
choosing what to export, answering a conflict, writing a failure. They are one
dialog with different contents. A dialog says what it is called, what it holds
and which keys it answers, and the shell builds the rest.

## Rules

- **Rule-INTERNAL-053** — Every Testin dialog has the same three parts: a title
  at the top, its fields stacked down the middle, and a strip along the bottom
  naming the keys that work right now.
- **Rule-INTERNAL-054** — The strip and the keys come from one declaration. A
  key named on the strip works, and a key that works is named on the strip.
  Neither can exist without the other.
- **Rule-INTERNAL-055** — A key on the strip works wherever the cursor is inside
  the dialog, not only in the field that has it.
- **Rule-INTERNAL-056** — One key does one thing. A dialog that gave two
  meanings to one key does not open at all.
- **Rule-INTERNAL-057** — The first field that can take the cursor has it when
  the dialog opens, so the tester can type straight away.
- **Rule-INTERNAL-058** — `Tab` moves to the next field in the order the fields
  are drawn, and `Shift+Tab` back to the one before.
- **Rule-INTERNAL-059** — `Escape` closes the dialog at once and saves nothing.
  Whether a dialog asks first is decided for that dialog: the failure form never
  asks (Rule-EDITOR-PANEL-144), and no other dialog asks yet.
- **Rule-INTERNAL-060** — A field's own way of saying yes does what the dialog's
  confirming key does. Clicking a row in a list, or pressing the dialog's
  button, is the same as pressing `Enter`.
- **Rule-INTERNAL-061** — A dialog opens in the middle of the window at the size
  its contents need. Two things make one movable and resizable: asking for a size
  in pixels, or saying it is resizable and keeping the size its contents give. The
  second is for a dialog whose rows decide how tall it is — a details popup grows
  with what it lists, and a fixed height would squeeze its chart out below the
  rows.
- **Rule-INTERNAL-067** — A dialog says why it will not take what was typed and
  stays open with the value still in the field. An empty field is marked as the
  one holding the dialog open; a value the dialog refuses for any other reason
  is refused in one sentence naming the value.
- **Rule-INTERNAL-075** — A dialog of a kind already on screen is brought
  forward rather than opened again. Testin dialogs do not close when they lose
  the focus, so nothing else would have stopped a second one. A dialog that holds
  nothing the tester typed - a confirmation, a node's details, a screenshot - is
  replaced by the newer one instead, closed unanswered. So an older question can
  never be answered by the key meant for a newer one.
- **Rule-INTERNAL-076** — A dialog says whether clicking away closes it. Almost
  none do - one holding what the tester typed must not lose it to a stray click,
  and Escape is what cancels. The search does, because it holds a question
  rather than an answer.
- **Rule-INTERNAL-077** — Every icon a framework surface draws is gray. A stock
  platform icon may ship colored, and one colored glyph among gray ones is the
  loudest thing on the row. A color that means something - an error, a verdict -
  is not an icon that names a thing and is drawn as it is.
- **Rule-INTERNAL-078** — The shortcut strip is one row. A dialog narrower than
  its own hints shortens the strip rather than folding it onto a second line and
  growing a line taller to hold it.
- **Rule-INTERNAL-079** — A surface has one shortcut strip. It cannot be built
  as half of a pair, so no dialog is two tinted rows tall to say six words.
- **Rule-INTERNAL-080** — A button a dialog will not act on yet is drawn
  disabled, and hovering over it says why.
- **Rule-INTERNAL-085** — In a box that offers a list and can also be typed
  into, Enter picks the value under it while the list is open. While the list is
  closed, Enter is the dialog's own key, as it is in every other field.
- **Rule-INTERNAL-087** — A caption sits on its own line above the field it
  names, in the caption font: JetBrains Mono, two points below the dialog's
  label font, in capitals, in the muted caption gray. There is no caption
  column to line up.
- **Rule-INTERNAL-095** — Every font a tester reads comes from one owner,
  `org.testin.util.Fonts`, which names each role - title, strong, body, label,
  badge, code, caption, message, field, placeholder, value, choice, small,
  hint, keycap, figure and icon letter - and derives them all from the same two
  sizes: the editor's, which the panels zoom with, and the IDE's label font,
  which the dialogs follow. A surface asks for the role it is showing and never
  derives a font of its own, so a title is the same size in every panel and a
  placeholder the same in every dialog. The documents Testin writes are in it
  too: `Fonts.Report` holds the point sizes a PDF and a Word file are set in,
  the pixel sizes an HTML report uses, and the families all three are written
  in, so a size changes in one place or it disagrees with itself in three.
- **Rule-INTERNAL-096** — Every typing surface in a dialog is drawn in the same
  frame, whether it holds one line or many: a text area sits in the frame a text
  field has, not in a borderless well. A value the dialog shows read-only is set
  in the size a field would show it in, so a test case's description above the
  fields is not smaller than the answer being typed under it.
- **Rule-INTERNAL-097** — A box that holds many lines grows as lines are added,
  and the dialog grows with it rather than scrolling inside a fixed box. Every
  such box is the same box: the same frame, the same font, Tab leaves it, Enter
  is the dialog's key and Ctrl+Enter adds a line.

These rules are about the shell every dialog is built on. What each dialog
holds, and what its keys mean, is on the page for that dialog.

## What the tester sees

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Node                                       (1)  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [set]  set name...                                     (2)  │
│                                                              │
│  [set]  Test Set          Holds test cases              (3)  │
│  [pkg]  Test Set Package  Groups test sets                   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    ↑ ↓ Select    Escape Cancel           (4)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — what this dialog is for, in a few words.
2. **The first field** — it has the cursor the moment the dialog opens.
3. **The rest of the fields**, stacked below it in the order they were
   declared. One of them takes any space left over; the others keep the height
   they need.
4. **The strip** — every key that works right now, and what each one does. Some
   entries are only a hint: `↑ ↓ Select` names keys the list answers itself,
   and the strip shows them so the tester knows they are there.

## Main flow

1. The tester opens a dialog, from a key or a menu.
2. It opens in the middle of the window, with the cursor in the first field.
3. The tester types, and moves between fields with `Tab`.
4. The tester presses a key from the strip, or clicks a row.
5. The dialog does that one thing and closes.

## What Testin refuses

**If the tester presses `Escape`** — the dialog closes at once and nothing is
saved. No dialog asks first today, even when something was typed, and what was
typed is gone. That is decided for the failure form (Rule-EDITOR-PANEL-144) and
for Report Bug (Rule-VIEW-PANEL-070). For every other dialog it is only how the
platform closes a popup: it acts on `Escape` before the dialog sees the key.

**If a dialog is written with two meanings for one key** — it does not open, and
the plugin says which key. A key that silently replaced another is the failure
this prevents, and it cannot be seen from the screen.

**If a dialog is written with no fields at all** — it does not open. There would
be nothing to put the cursor in.

## Where the plugin breaks its own rules

**Three surfaces are not built on the shell**, against thirty-four that are:
light mode's zoom indicator, the button that picks which details a view shows,
and the shortcut menu. Rule-INTERNAL-053 to Rule-INTERNAL-061 do not reach them.
They behave the same way by hand, which is the problem: each is a copy that can
drift. That is difference 6 on
[the Inside Testin page](main.md#where-the-plugin-breaks-its-own-rules), and
[#69](https://github.com/mtb550/test-in/issues/69).

**The test case create and update dialogs are built on the shell**, like every
other. The shell owns the popup, the title, the strip, the sizing and the
one-at-a-time rule. Each dialog owns its own sections and the keys that reach
past the editors inside them.

**The shortcut menu is the exception that keeps the promise.** It is still
hand-built — a menu is rows and nothing else, and each row carries and prints
its own letter, so the shell would give it nothing. What it does not do anymore
is answer a key it never mentions: its `Enter`, `Escape` and arrows come from
one declaration that both binds the keys and draws the strip, the same two
halves the shell uses. Rule-INTERNAL-067.

---

[Documentation](../README.md) › [Inside Testin](main.md)
