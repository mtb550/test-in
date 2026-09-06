[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-011

# UC-EDITOR-PANEL-011: Remove test cases

**As a** tester, **I want** to delete test cases that are no longer wanted,
**so that** the test set is what somebody would actually run.

`Delete` on the selection.

## Rules

- **Rule 59** — The confirmation names the test case, or counts them, and says
  which test set they are in.
- **Rule 60** — A removal can be taken back with `Ctrl+Z`.
- **Rule 61** — Nothing is renumbered. The removed test case simply leaves a gap
  in the order, and the numbers on screen close up on their own.
- **Rule 62** — A test case that is waiting to be pasted is removed without
  asking again, because the move was already agreed to.

Rules 1 to 9 hold everywhere in the panel. They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Confirm Removing                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Remove these 4 test cases?                                  │
│                                                              │
│  From    C:\Users\mtb\Downloads\Testin\Demo\Test Cases\Login │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Remove       Escape Cancel                       │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — names the one test case, or counts several.
2. **From** — the test set they are in, as a path on disk.

## Main flow

1. The tester selects four cards and presses `Delete`.
2. The **Confirm Removing** dialog opens.
3. The tester presses `Enter`.
4. The four test cases leave the editor, the test project and the automation
   code.
5. A message reads *Removed 4*.

## What Testin refuses

**If nothing is selected** — **Delete** is gray and the key does nothing.

**If the tester presses `Escape`** — nothing is removed and nothing is said.

## What goes with them

The test case's file, and its generated test method. Anything the tester wrote
inside that method goes with it, and the confirmation does not mention the code.

Undoing the removal brings the test cases back and writes their methods again,
empty. What was in the method bodies does not come back.

---

[Documentation](../README.md) › [The editor panel](main.md)
