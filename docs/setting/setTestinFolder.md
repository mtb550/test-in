[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-002

# UC-SETTING-002: Set the Testin folder

**As a** tester, **I want** to tell Testin which folder on this machine holds my
test projects, **so that** the tree has something to show.

Testin shows nothing at all until this folder is set. It is the first thing to
do on a new machine.

There is no key for this. It is the first row of the page.

## Rules

- **Rule-SETTING-001** — One page for the whole IDE. Every code project open in
  it reads the same values.
- **Rule-SETTING-002** — Nothing on this page is checked. A folder that does not
  exist is stored exactly as typed.
- **Rule-SETTING-003** — Nothing on this page raises a message when it is saved.
- **Rule-SETTING-004** — Only a changed Testin folder makes Testin read the disk
  again. Every other setting is read where it is used, when it is used.
- **Rule-SETTING-005** — A password is never on this page. It is asked for when
  it is needed and kept in the IDE's password store.
- **Rule-SETTING-006** — Nothing on this page has a key of its own.
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

## The screen

The row is the first one on the page.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Testin source root:  [ Example -> C:\Users\...\Testin ] [...] [Open]    │
└──────────────────────────────────────────────────────────────────────────┘
```

1. **The box** — the one folder that holds every test project.
2. **The gray example** — shown only while the box is empty. It reads
   *Example -> C:\Users\{username}\Documents\Testin*.
3. **The browse button** — opens a folder chooser.
4. **Open** — opens the folder in the file manager. It is
   [UC-SETTING-003](openTestinFolder.md).

The folder chooser is titled **Select Root Folder**. The line under that title
reads *Choose the directory where your test projects are stored*.

The whole page is drawn on [the settings page](main.md#the-page).

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

**Nothing is refused here.** Three kinds of path are all stored exactly as
typed: a path that does not exist, a path that is a file, and a path of nothing
but spaces. That is difference 1 on
[the settings page](main.md#where-the-plugin-breaks-its-own-rules).

## What happens when no folder is set

**When a code project opens** — a message titled **Testin Setup Required** reads
*Please configure the Root Testin Folder to enable test management features.*
It carries a link reading **Open Settings**.

**In the tree panel** — the empty state is shown, with a link reading
**Configure Testin settings**.

**On the toolbar** — **Select Test Project** is gray.

**On disk** — nothing is read at all.

**If the path has spaces around it** — they are removed when it is stored, so
the value in the file is the value Testin uses. It used to keep them and trim
them at the next project open, which changed a stored setting nobody had edited.

---

[Documentation](../README.md) › [The settings page](main.md)
