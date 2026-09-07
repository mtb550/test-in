[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-005

# UC-EDITOR-PANEL-005: Create a test case

**As a** tester, **I want** to add a test case to the test set I am looking at,
**so that** something I have just thought of is written down before I forget it.

`Ctrl+M`.

## Rules

- **Rule-EDITOR-PANEL-027** — The dialog opens showing the description alone.
  Every other field appears when its key is pressed.
- **Rule-EDITOR-PANEL-028** — A field the tester never opened writes nothing.
- **Rule-EDITOR-PANEL-029** — A new test case has no place in the order yet, so
  it sorts last.
- **Rule-EDITOR-PANEL-030** — A new test case starts at the lowest priority.
- **Rule-EDITOR-PANEL-031** — The description, the expected result, the module,
  the pre-conditions, the test data and each step have their spaces trimmed.
- **Rule-EDITOR-PANEL-032** — A blank step is dropped.
- **Rule-EDITOR-PANEL-033** — The dialog does not close when the tester clicks
  outside it, or when the IDE loses the focus.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

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

**If the description cannot name a Java method** — nothing is saved, and a
message titled **That description cannot name a test method** says what the
method would have been called. A description has to begin with a letter, and
cannot be a single word Java keeps for itself.

**If a step is left blank** — it is dropped, and the steps after it keep their
own numbers.

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
