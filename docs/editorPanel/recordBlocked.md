[Documentation](../README.md) › [The editor panel](main.md) › UC-EDITOR-PANEL-033

# UC-EDITOR-PANEL-033: Record that a test case is blocked

**As a** tester, **I want** to say a test case could not be tried at all,
**so that** it is not counted as a failure of the product.

`B`.

## Rules

- **Rule-EDITOR-PANEL-137** — **Blocked** means the test case was attempted and
  could not finish, usually because of the environment or the data.
- **Rule-EDITOR-PANEL-138** — Recording blocked clears nothing. Anything already
  written about the test case stays.

Rule-EDITOR-PANEL-001 to Rule-EDITOR-PANEL-009 hold everywhere in the panel.
They are on
[the editor panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The walk has selected a test case and is timing it.
2. The tester finds the environment will not let them try it.
3. The tester presses `B`.
4. Testin records **Blocked**, the tester's name, the time and the duration.
5. A message reads *Blocked*.
6. The walk moves to the next test case.

## What Testin refuses

**If nothing is selected** — nothing happens.

**If the test case was deleted from its test set** — a message reads *The test
case was removed - the run keeps what it recorded.*

**If a grid cell is open for editing** — the key belongs to the cell.

## Blocked against failed

**Blocked** is the environment's fault. **Failed** is the product's. A report
counts them apart, and its blocked section says the test cases *could not
complete, typically because of an environment or data dependency*.

Blocked asks for no explanation. If the tester wants to write down why, they can
type into the **Actual Result** column afterwards, which is
[UC-EDITOR-PANEL-041](typeActualResult.md).

---

[Documentation](../README.md) › [The editor panel](main.md)
