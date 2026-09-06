[Documentation](../README.md) › [The project panel](main.md) › UC-003

# UC-003: Choose which test project this code project uses

> **No key.** Press **Select Test Project** at the top of the panel.

**As a** tester, **I want** to point this code project at a different test
project in the same Testin folder, **so that** one machine can serve several
products.

## Rules

- **Rule 19** — The choice is written into the code project. A colleague who
  copies that project down gets the same test project, with no setup.
- **Rule 20** — If the choice cannot be written, Testin says so. It never
  reports the choice as saved.

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The tester presses the **Select Test Project** button in the panel header.
2. The **Select Test Project** dialog lists every test project in the Testin
   folder, with its status. The current one is selected.
3. The tester selects one and presses `Enter`.
4. Testin writes the choice into this code project.
5. The tree reloads on that test project.
6. Testin shows *Bound*, with the test project's name.

## What Testin refuses

**If no test project folder exists in the Testin folder** — no dialog opens, and
the message *No Test Projects*, with the line *Create one under the Testin root
first*, is shown in red.

**If the code project's configuration file cannot be written** — an error titled
*Not Bound* says the choice will not be remembered, and the dialog stays open.

---

[Documentation](../README.md) › [The project panel](main.md)
