[Documentation](README.md) › Every shortcut

# Every shortcut

Every key Testin answers to, what it does, and where. One row per key per place,
so a key that means different things in two places appears twice - which is the
honest way to read this, because that is how a tester meets them.

`Ctrl` here is `Cmd` on a Mac for the keys the platform decides: copy, cut,
paste, undo, redo, find, and select every value. Everything else is `Ctrl` on
every machine, because it is Testin's own key rather than the operating
system's.

Each module's own page carries the same keys in its **Every key, in one place**
table; this is all of them together.

## The tree panel

| Key | What it does |
|---|---|
| `Enter` | Opens the selected test set or test run |
| `Context Menu` | Opens the menu on the selected node, without the mouse |
| `Ctrl+M` | Creates a node under the selected one |
| `Shift+F6` | Renames the selected node |
| `Delete` | Removes the selected nodes |
| `Ctrl+X` | Cuts the selected nodes, to move them |
| `Ctrl+C` | Copies the selected nodes |
| `Ctrl+V` | Pastes into the selected node |
| `Escape` | Takes the gray off nodes the tester cut |
| `Ctrl+Z` | Undoes the last tree change |
| `Ctrl+Y` | Redoes it |
| `1` `2` `3` | Inside the status popup: Assigned, Completed, Closed |
| `Enter` | Inside the status popup, takes the highlighted one |
| `Shift+Enter` | **Review Changes**, on the message about uncommitted work |
| `Ctrl+P` | Generates a report on the selected test run |

## The editor panel: both editors

| Key | What it does |
|---|---|
| `Ctrl+F` | Puts the cursor in the search box |
| `Ctrl+Right` | The next page |
| `Ctrl+Left` | The previous page |
| `Enter` | Opens the details panel, or a grid cell |
| `Escape` | Steps back one step |
| `Context Menu` | Opens the menu on the selection |
| `Ctrl` and the wheel | Changes the text size |
| `Delete` | Removes the selected test cases |
| `Ctrl+Z` | Takes back the last change |
| `Ctrl+Y` | Puts it back |
| `Ctrl+Enter` | A line break inside a grid cell or a long field |
| `Ctrl+C` | On cards, copies the details as text. In the grid, copies the cells |
| `Ctrl+X` | In the grid, copies the cells and empties the ones that can be typed into |
| `Ctrl+V` | In the grid, pastes text into the cells |
| `Ctrl+Shift+C` | Copies the test cases themselves |
| `Ctrl+Shift+X` | Cuts the test cases |
| `Ctrl+Shift+V` | Pastes test cases into this test set |
| `F5` | Runs the selected test cases, or stops them |
| `Shift+F5` | Goes to the automation code |
| `Ctrl+F12` | **Automate Test Case**, which is not built |
| `Ctrl+P` | Generates a report on this test run |

## The editor panel: writing test cases

On the cards of a test set editor.

| Key | What it does |
|---|---|
| `Ctrl+M` | Creates a test case |
| `F2` | Opens the menu of fields to change |
| `D` `E` `M` `T` `B` `S` `P` `G` `O` | Opens that one field straight away |
| `Enter` | Inside the F2 menu, takes the highlighted field |

## The editor panel: executing a test run

On the cards of a test run editor.

| Key | What it does |
|---|---|
| `P` | Records **Passed** |
| `F` | Records **Failed**, and asks why |
| `B` | Records **Blocked** |
| `F2` | Changes the failure details without changing the verdict |

`P` and `B` mean a field in the test set editor and a verdict in the test run
editor. They are different keys on different screens, not one key with two jobs.

## Light mode

The always on top window, which has its own keys.

| Key | What it does |
|---|---|
| `P` `F` `B` | The three verdicts, as on the cards |
| `Enter` | Saves the failure detail and moves on |
| `Escape` | Leaves the failure form, or closes the window |
| `Ctrl+D` | Shows the details |
| `Ctrl+H` | Hides them |
| The wheel alone | Changes the text size - no `Ctrl` here, deliberately |

## The view panel

| Key | What it does |
|---|---|
| `Enter` | Opens the panel on the selected test cases |
| `F2` | Opens the menu that changes one field |
| `Ctrl+Right` | Moves to the next test case |
| `Ctrl+Left` | Moves to the previous test case |
| `F5` | Runs the test case on display |
| `Shift+F5` | Opens its generated test method |
| `Escape` | Closes the panel, pressed in the editor |
| `Ctrl` and the wheel | Changes the text size |

## Automation code

| Key | What it does |
|---|---|
| `F5` | Runs the selected test cases, or stops them |
| `Shift+F5` | Goes to the generated method |
| `Ctrl+F12` | **Automate Test Case**, which is not built |

## Reports

| Key | What it does |
|---|---|
| `Ctrl+P` | Generates a report on the selected test run |

## Sharing work with the team

| Key | What it does |
|---|---|
| `Enter` | Confirms the group picker, the Git identity, a merge answer, and a removal |
| `Escape` | Cancels the dialog, except the merge question below |
| `Escape` | On a merge question, skips that test case and goes on to the next |
| `Ctrl+Click` | Adds a group in the group picker |
| `Right click` | Puts one change back, in the review |

## Inside Testin

| Key | What it does |
|---|---|
| `Ctrl+Alt+F` | Opens search, from anywhere in the IDE. `Cmd+Alt+F` on a Mac |
| `Enter` | In the search dialog, goes to the result |
| `Up` `Down` | Moves through the results |
| `Escape` | Closes the search |

## The settings page

The settings page has no keys of its own. Changing the text size is `Ctrl` and
the mouse wheel over the editor panel or the view panel, and the wheel alone in
light mode. The tree does not zoom.

## Inside the test case dialogs

While the create or update dialog is open.

| Key | What it does |
|---|---|
| `Ctrl+D` | Opens the description field |
| `Ctrl+E` | Opens the expected result |
| `Ctrl+M` | Opens the module |
| `Ctrl+S` | Adds a step |
| `Ctrl+W` | Removes a step |
| `Ctrl+T` | Opens the test data |
| `Ctrl+B` | Opens the pre conditions |
| `Ctrl+G` | Opens the group picker |
| `Ctrl+P` | Opens the priority |
| `Alt+Enter` | Offers the corrections for what was typed |
| `Ctrl+Space` | Offers what has been typed before |
| `Tab` `Shift+Tab` | Moves between the choices in a picker |
| `Up` `Down` | Moves through a list of choices |
| `Space` | Selects the group under the cursor |
| `Enter` | Saves |
| `Escape` | Cancels, and asks first when something was typed |

## Inside the bulk editors

| Key | What it does |
|---|---|
| `Ctrl+Enter` | Adds an item to a list being edited |
| `Shift+Delete` | Removes one |
| `Ctrl+Shift+A` | Puts a caret on every value at once |
| `Ctrl+Click` | Puts a caret where clicked |
| `Tab` `Shift+Tab` `Up` `Down` | Move between the values |
| `Enter` | Saves |
| `Escape` | Cancels |

## In any Testin text field

| Key | What it does |
|---|---|
| `Ctrl+C` `Ctrl+X` `Ctrl+V` | Copy, cut and paste the text |
| `Ctrl+A` | Selects all of it |

## Every key on this page is bound

There is no list of declared-and-unbound keys any more, and there should never
be one again.

`H` `M` `L` were drawn beside the priorities and `E` beside the empty bug
priority, and nothing answered to any of them. They were removed rather than
bound: the priorities render `P1` `P2` `P3`, so the letters matched nothing a
tester could see, and `E` named a choice the failure dialog does not offer at
all. Choosing a priority is the mouse or the arrow keys, which is what the hint
beside it now says. [#283](https://github.com/mtb550/test-in/issues/283).

**A key is drawn only where pressing it does that thing.** Both directions of
that matter: a capability shown without its key is
[Rule-PRODUCT-016](product.md), and a key shown for a capability that has none
is this.

---

[Documentation](README.md)
