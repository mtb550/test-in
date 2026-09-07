[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-002

# UC-SETTING-002: Set the Testin folder

**As a** tester, **I want** to tell Testin which folder on this machine holds my
test projects, **so that** the tree has something to show.

There is no key for this. It is the first row of the page.

## Rules

- **Rule-SETTING-010** — Testin reads exactly one folder. Every test project
  lives directly inside it.
- **Rule-SETTING-011** — An empty box means not set. A box of nothing but spaces
  means the same.
- **Rule-SETTING-012** — Changing the folder makes every code project that has
  opened the Testin panel read the disk again.
- **Rule-SETTING-013** — A folder given as a partial path is read against the
  code project's own folder.
- **Rule-SETTING-014** — With no folder set, Testin reads nothing and shows
  nothing. It does not fail.

Rule-SETTING-001 to Rule-SETTING-006 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

## The screen

The row is at the top of the page, drawn on
[the settings page](main.md#the-page).

The gray example in the empty box reads:

> Example -> C:\Users\{username}\Documents\Testin

The browse button opens a folder chooser titled **Select Root Folder**, whose
line underneath reads *Choose the directory where your test projects are
stored*.

## Main flow

1. The tester opens the settings page.
2. The tester presses the browse button on the first row.
3. The folder chooser opens.
4. The tester picks the folder holding their test projects and confirms.
5. The whole path appears in the box.
6. The tester presses **Apply**.
7. Every code project with a Testin panel open reads the folder again.
8. The tree fills with the test project that code project is bound to.

## What Testin refuses

**Nothing is refused here.** A path that does not exist, a path that is a file
and a path of nothing but spaces are all stored exactly as typed. That is
difference 1 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

## What happens when no folder is set

**When a code project opens** — a message titled **Testin Setup Required** reads
*Please configure the Root Testin Folder to enable test management features.*
It carries a link reading **Open Settings**.

**In the tree panel** — the empty state is shown, with a link reading
**Configure Testin settings**.

**On the toolbar** — **Select Test Project** is gray.

**On disk** — nothing is read at all.

## Where the plugin breaks its own rules

**The stored value changes on its own.** A folder saved with spaces around it is
stored with them. The next time a code project opens, Testin trims them and
writes the trimmed value back. That is difference 3 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The settings page](main.md)
