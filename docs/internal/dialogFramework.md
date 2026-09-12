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
- **Rule-INTERNAL-059** — `Escape` closes the dialog and saves nothing. When
  something typed would be lost, it asks first, and says what is about to go.
- **Rule-INTERNAL-060** — A field's own way of saying yes does what the dialog's
  confirming key does. Clicking a row in a list, or pressing the dialog's
  button, is the same as pressing `Enter`.
- **Rule-INTERNAL-061** — A dialog opens in the middle of the window at the size
  its contents need. A dialog that asks for a size instead can be moved and
  resized.
- **Rule-INTERNAL-067** — A dialog says why it will not take what was typed and
  stays open with the value still in the field. An empty field is marked as the
  one holding the dialog open; a value the dialog refuses for any other reason
  is refused in one sentence naming the value.
- **Rule-INTERNAL-075** — A dialog of a kind already on screen is brought
  forward rather than opened again. Testin dialogs do not close when they lose
  the focus, so nothing else would have stopped a second one.
- **Rule-INTERNAL-076** — A dialog says whether clicking away closes it. Almost
  none do - one holding what the tester typed must not lose it to a stray click,
  and Escape is what cancels. The search does, because it holds a question
  rather than an answer.
- **Rule-INTERNAL-077** — Every icon a framework surface draws is gray. A stock
  platform icon may ship colored, and one colored glyph among gray ones is the
  loudest thing on the row. A color that means something - an error, a verdict -
  is not an icon that names a thing and is drawn as it is.

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

**If the tester presses `Escape` with nothing typed** — the dialog closes at
once and nothing is saved.

**If the tester presses `Escape` after typing something** — Testin asks first,
in a dialog headed **Discard what you typed?**, and says what is about to go.
Answering **Discard** closes it; answering nothing leaves it open.

**If a dialog is written with two meanings for one key** — it does not open, and
the plugin says which key. A key that silently replaced another is the failure
this prevents, and it cannot be seen from the screen.

**If a dialog is written with no fields at all** — it does not open. There would
be nothing to put the cursor in.

## Where the plugin breaks its own rules

**Five surfaces are not built on the shell**, against twenty-four that are: the
test case create and update dialogs, light mode's zoom indicator, the details
popup button and the shortcut menu. Rule-INTERNAL-053 to Rule-INTERNAL-061 do
not reach them. They behave the same way by hand, which is the problem: each is
a copy that can drift. That is difference 6 on
[the Inside Testin page](main.md#where-the-plugin-breaks-its-own-rules), and
[#69](https://github.com/mtb550/test-in/issues/69).

**The shortcut menu is the exception that keeps the promise.** It is still
hand-built — a menu is rows and nothing else, and each row carries and prints
its own letter, so the shell would give it nothing. What it does not do any more
is answer a key it never mentions: its `Enter`, `Escape` and arrows come from
one declaration that both binds the keys and draws the strip, the same two
halves the shell uses. Rule-INTERNAL-067.

---

[Documentation](../README.md) › [Inside Testin](main.md)
