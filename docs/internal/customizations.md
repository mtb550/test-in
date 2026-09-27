# What Testin draws itself

Every place the plugin builds its own color, icon, border, painting, popup or
size instead of taking the platform's. Read it when deciding whether to add one.

Measured on `af6b14a3`, over `src/main`. `testin-java` and `testin-testng` draw
nothing.

The rows are grouped by the only question that matters: **does the platform
already ship this?** Only the first group is a candidate for removal.

---

## The platform ships this

| What                                                                         | Where                                                                       | The platform's                                                                                                                                     | Cost to drop                                                                                         |
|------------------------------------------------------------------------------|-----------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| The grid's selection color, a hand-mixed pair `(214,230,250)` / `(37,55,76)` | `editor/EditorColors.java:29-31`                                            | `UIUtil.getListSelectionBackground(boolean)`, or the scheme key `EditorColors.SELECTION_BACKGROUND`                                                | One constant. The pair was mixed before the theme keys were used anywhere                            |
| The bulk editor's diff tint, `(228,250,228)` / `(43,61,44)`                  | `testcase/update/bulk/BulkJsonEditors.java:266`                             | The diff scheme already answers this: `DiffColors`/`EditorColors` carry an added-lines color the tester has themed                                 | One line, and it starts obeying the tester's diff colors                                             |
| A text field background fallback, `JBColor(Gray._245, Gray._50)`             | `testcase/update/bulk/BulkJsonEditors.java:107`                             | `UIUtil.getTextFieldBackground()`                                                                                                                  | One line                                                                                             |
| The editor row stripe, `Gray._245/_60` and `Gray._230/_45`                   | `ui/framework/RowStripe.java:29-30`                                         | `UIUtil.getDecoratedRowColor()` is the platform's striped-row color                                                                                | One constant, three readers (`BaseCard`, `GridPanelBuilder` ×2, `lightmode/FailureForm`)             |
| The whole table UI delegate replaced                                         | `editor/grid/GridPanelBuilder.java:401` — `table.setUI(new BasicTableUI())` | `JBTable` ships a UI that honors the theme; `BasicTableUI` is Swing's generic one, so the grid opts out of every IntelliJ table behavior at once | Unknown until tried. This is the single largest customization in the plugin and the least documented |
| A letter drawn into an icon                                                  | `util/Icons.java:56-95` (`fieldLetter`, `LetterIcon`)                       | `com.intellij.ui.TextIcon` — which `ui/dialogs/DialogStyle.java:133` already uses for the keycap note                                              | Medium: `TextIcon` sizes itself from the font, the hand-drawn one centres in a fixed box             |
| A toolbar button painting its own background                                 | `editor/AbstractIconButton.java:142`                                        | `ActionButton` and `InplaceButton` paint hover and pressed states from the theme                                                                   | Large: the button also owns its own hover and selection state                                        |

## The platform does not ship this

| What                                            | Where                                           | Why nothing fits                                                                                                                 |
|-------------------------------------------------|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| The dialog card: a rounded fill plus a hairline | `ui/dialogs/SectionFill.java:40`                | `RoundedLineBorderWithBackground` is exactly this and is `@ApiStatus.Internal`. Verified in the SDK, 2026-09-27                  |
| The unfocused field frame                       | `ui/dialogs/FieldFrame.java:31-33`              | It **extends** `DarculaTextBorder` and only adds the unfocused stroke, which is the intended way to specialise a platform border |
| A chosen row as a rounded band                  | `ui/framework/TextFieldWithSelections.java:270` | A list selection is a rectangle everywhere in Swing. Rule-INTERNAL-106                                                           |
| Badge pills                                     | `ui/Badges.java:180`                            | No platform pill. The shape carries severity and priority together                                                               |
| The verdict donut                               | `view/marker/VerdictDonut.java:160,182`         | No platform chart of any kind                                                                                                    |
| The grid's selected-cell edge                   | `editor/grid/SelectionCellBorder.java:45`       | A table cell border is the caller's in Swing                                                                                     |
| Light mode's key button                         | `lightmode/KeyBtn.java:84`                      | Related: `ui/framework/Keycap` draws the same idea for dialogs, so **these two are each other's duplicate**, not the platform's  |
| Analysis category colors parsed from `#rrggbb`  | `model/ResultAnalysis.java:125`                 | The colors are the tester's own data, not the IDE's                                                                              |

## Decided, not drift

These are recorded decisions. They are listed so nobody "cleans" them.

| What                                                          | Where                                   | The decision                                                                            |
|---------------------------------------------------------------|-----------------------------------------|-----------------------------------------------------------------------------------------|
| `GRAY`, `RED`, `GREEN` are a `JBColor` whose halves are equal | `util/Icons.java:45-47`                 | Testin's icons are one color in both themes, on purpose - the pair says so              |
| `@SuppressWarnings("UnstableApiUsage")`                       | `editor/TestinTabColorProvider.java:39` | The tab title color is the experimental half of the tab color interface. Open in #324   |
| `//noinspection QualifiedClassName`                           | `logger/LogWriter.java:46`              | `Logger` collides with the plugin's own; CLAUDE.md names this as the one exception      |
| Run status badge colors                                       | `model/RunStatus.java:41,47,53`         | Passed, failed and running read as green, red and amber to a tester regardless of theme |
| The dialog surfaces                                           | `ui/dialogs/DialogStyle.java:61`        | Two named theme colors, chosen per theme by luminance. Rule-INTERNAL-099                |

## Not customizations at all

Counted during the sweep and dismissed, so the next sweep does not re-raise them.

| What                         | Count | Why it is not                                                                                                                                                                                                                         |
|------------------------------|-------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Cell renderers               | 5     | Every one extends a platform renderer (`ColoredListCellRenderer`, `ColoredTreeCellRenderer`, `TableCellRenderer`). That is the platform's own extension point                                                                         |
| Popups                       | 6     | `DialogStyle.createPopupBuilder` is the framework's one owner; `FilterPopupBtn` uses the stock `createActionGroupPopup`; the other four hand-build because they attach under a component, which `AbstractFrameworkDialog` does not do |
| Components in `ui.framework` | 35    | The thing that makes every dialog agree                                                                                                                                                                                               |
| Font roles in `util.Fonts`   | 22    | One owner for every font a tester reads. Rule-INTERNAL-095                                                                                                                                                                            |
| Rules in `tools/inspect.ps1` | 12    | They exist because no IntelliJ inspection covers them                                                                                                                                                                                 |
| Fixed sizes                  | 5     | `StatusBar:188`, `AbstractToolbarPanel:92`, `LightModeWindow:665`, `TestCaseForm:74`, `StatusBarBase:46` — each pins one dimension so a row cannot grow with its content. Worth re-reading, but they are layout, not drawing          |

---

## The two questions to ask before adding one

1. **Has the platform got it?** `AllIcons` for icons, `JBUI.CurrentTheme` and the
   theme's named keys for colors, `JBUI.Borders` for borders,
   `ColoredListCellRenderer` and friends for renderers, `JBPopupFactory` for
   popups. Search before concluding it has not: a collapse triangle was
   hand-drawn here on 27 September 2026 because `AllIcons.General.ArrowDown` was
   assumed to be a chevron. It is a `<polygon>`, and the platform ships 106
   filled-triangle icons.
2. **Does it survive every bundled theme?** Six ship: New UI Dark and Light,
   Darcula, IntelliJ Light, Island Dark and Island Light. A color that is right
   in one and invisible in another is a defect, not a customization.
