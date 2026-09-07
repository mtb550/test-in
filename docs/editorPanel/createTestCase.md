[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-005

# UC-EDITOR-PANEL-005: Create a test case

**As a** tester, **I want** to add a test case to the test set I am looking at,
**so that** an idea is written down before I forget it.

This is how every test case in Testin begins.

`Ctrl+M`.

## Rules

- **Rule-EDITOR-PANEL-001** — A test set opens in one editor and a test run in
  another. Both are the same shape: a toolbar on top, the rows in the middle, a
  status bar at the bottom.
- **Rule-EDITOR-PANEL-002** — Both editors open showing cards. The grid is built
  the first time the tester asks for it.
- **Rule-EDITOR-PANEL-003** — A card is drawn to the width of the list. A title
  too long for that width wraps onto more lines, and the cards never scroll
  sideways.
- **Rule-EDITOR-PANEL-004** — A page holds 50 test cases until the tester says
  otherwise. The most a page can hold is 1000.
- **Rule-EDITOR-PANEL-005** — What the tester types is stored exactly. Testin
  may draw it differently, and never saves the drawn form.
- **Rule-EDITOR-PANEL-006** — A save that would leave the file as it is writes
  nothing, and says nothing.
- **Rule-EDITOR-PANEL-007** — Each editor keeps an undo history of its own, and
  the tree keeps another.
- **Rule-EDITOR-PANEL-008** — Every change confirms itself with one message in
  the past tense. A change to several test cases gets one message with a count.
- **Rule-EDITOR-PANEL-009** — Moving the view says nothing. Paging, filtering,
  searching and opening the details panel are all silent. It changes nothing
  either: a test case the filter is hiding is reported, never brought into view
  by throwing the filter away.
- **Rule-EDITOR-PANEL-010** — While a grid cell is open for editing, every key
  that would act on the row is refused.
- **Rule-EDITOR-PANEL-028** — The dialog opens showing the description alone.
  Every other field appears when its key is pressed.
- **Rule-EDITOR-PANEL-029** — A field the tester never opened writes nothing.
- **Rule-EDITOR-PANEL-030** — A new test case has no place in the order yet, so
  it sorts last.
- **Rule-EDITOR-PANEL-031** — A new test case starts at the lowest priority.
- **Rule-EDITOR-PANEL-032** — The description, the expected result, the module,
  the pre-conditions, the test data and each step have their spaces trimmed.
- **Rule-EDITOR-PANEL-033** — A blank step is dropped.
- **Rule-EDITOR-PANEL-034** — The dialog does not close when the tester clicks
  outside it, or when the IDE loses the focus.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Case                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [/]  set description                                        │
│                                                              │
│  (a field appears here when its key is pressed)              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k] Enter Save    Escape Cancel    Ctrl+D Description       │
│      Ctrl+E Expected Result    Ctrl+S Steps    Ctrl+P Prio.. │
└──────────────────────────────────────────────────────────────┘
```

1. **The description** — the only field there when the dialog opens.
2. **The strip at the bottom** — the keys that work right now. It changes as the
   tester moves between fields.

## The fields, and the keys that open them

| Field | Key | The gray hint in the empty box |
|---|---|---|
| Description | `Ctrl+D` | *set description* |
| Expected Result | `Ctrl+E` | *set expected result* |
| Module | `Ctrl+M` | *set module* |
| Steps | `Ctrl+S` | *set step*, then the number |
| Priority | `Ctrl+P` | none, it is a list |
| Group | `Ctrl+G` | none, they are tick boxes |
| Test Data | **none** | *set test data* |
| Pre Conditions | **none** | *set pre conditions* |

## Main flow

1. The tester presses `Ctrl+M` in the editor.
2. The **Create Test Case** dialog opens with the cursor in the description.
3. The tester types a description.
4. The tester presses `Ctrl+E` and types the expected result.
5. The tester presses `Ctrl+S` and types the steps, one to a line.
6. The tester presses `Enter`.
7. Testin creates the test case at the end of the test set.
8. A message reads *Created*.
9. Testin writes the test method for it.
10. The new test case is selected.

## What Testin refuses

**If the description is empty** — the dialog stays open, the description turns
red, and the cursor goes back to it. No message is raised.

**If the description cannot name a Java method** — nothing is saved. A message
titled **That description cannot name a test method** says what the method would
have been called. A description must begin with a letter. It also cannot be a
single word that Java keeps for itself.

**If a step is left blank** — it is dropped. The steps after it keep their own
numbers.

## Where the plugin breaks its own rules

**Test Data and Pre Conditions cannot be filled in here.** Both are drawn in the
dialog and neither has a key that opens it, so neither can be reached. They can
be set afterwards with `T` and `B`. That is difference 2 on
[the editor panel page](main.md#where-the-plugin-breaks-its-own-rules-writing-test-cases).

**The group boxes read in capitals.** They read **REGRESSION** where the badge
beside them reads **Regression**. That is difference 3.

**`Ctrl+M` is not `Cmd+M` on a Mac.** Two other keys on the same screen are
turned into Mac keys and this one is not. That is difference 9.

---

[Documentation](../README.md) › [The editor panel](main.md)
