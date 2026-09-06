[Documentation](../README.md) › [The settings page](main.md) › UC-SETTING-004

# UC-SETTING-004: Give my name

**As a** tester, **I want** my name recorded on the work I do,
**so that** a colleague reading a test case or a verdict can see who last
touched it.

There is no key for this. It is the **Tester name** row.

## Rules

- **Rule 18** — The name is read at the moment it is stamped, not remembered
  from when the IDE started. Changing it takes effect at once.
- **Rule 19** — An empty name is allowed. Testin then stamps nothing, which
  means the file did not say rather than that nobody chose.

Rules 1 to 6 hold everywhere on the page. They are on
[the settings page](main.md#rules-that-hold-everywhere-on-the-page).

## Where the name is stamped

| What the tester does | What carries the name |
|---|---|
| Creates any node in the tree | Who created it |
| Renames a node | Who last changed it |
| Changes a test project's, a test set's or a package's status | Who last changed it |
| Saves a test case | Who created it, or who last changed it |
| Records a verdict in a test run | Who ran it |
| Starts a sync with the team's server | Who is syncing, so others can see |

The name appears on the Details popup of any node, and on the **Created By** and
**Updated By** rows of the view panel.

## Main flow

1. The tester types their name into the **Tester name** row.
2. The tester presses **Apply**.
3. From that moment, everything this machine writes carries the name.

## What Testin refuses

Nothing. An empty name is accepted, and every stamp is then left empty.

## What it does not do

The name is not sent anywhere and is not the same as the name Git records on a
commit. Git is told who the tester is separately, and that is
[UC-SHARE-008](../share/setGitIdentity.md).

---

[Documentation](../README.md) › [The settings page](main.md)
