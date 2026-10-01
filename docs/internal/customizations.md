# What Testin draws itself

Every place the plugin builds its own color, icon, border, painting, popup or
size instead of taking the platform's. Read it when deciding whether to add one.

Measured on `af6b14a3`, over `src/main`. `testin-java` and `testin-testng` draw
nothing.

The rows are grouped by the only question that matters: **does the platform
already ship this?** Only the first group is a candidate for removal.

---

## The platform ships this

| What                                                                         | Where                                                                                | The platform's                                                                                                                                   | Cost to drop                                                                                         | Rule in docs/                                                                                                                                                         |
|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The grid's selection color, a hand-mixed pair `(214,230,250)` / `(37,55,76)` | `editor/EditorColors.java` `SELECTION_BACKGROUND`                                    | `UIUtil.getListSelectionBackground(boolean)`, or the scheme key `EditorColors.SELECTION_BACKGROUND`                                              | One constant. The pair was mixed before the theme keys were used anywhere                            | Rule-EDITOR-PANEL-250 asks for a selection color, not this pair                                                                                                       |
| The bulk editor's diff tint, `(228,250,228)` / `(43,61,44)`                  | `testcase/update/bulk/BulkJsonEditors.java` `refreshDiffHighlights`                  | The diff scheme already answers this: `DiffColors`/`EditorColors` carry an added-lines color the tester has themed                               | One line, and it starts obeying the tester's diff colors                                             | None                                                                                                                                                                  |
| A text field background fallback, `JBColor(Gray._245, Gray._50)`             | `testcase/update/bulk/BulkJsonEditors.java` `BulkJsonEditors(Project)`               | `UIUtil.getTextFieldBackground()`                                                                                                                | One line                                                                                             | None                                                                                                                                                                  |
| The editor row stripe, `Gray._245/_60` and `Gray._230/_45`                   | `ui/framework/RowStripe.java` `EVEN`, `ODD`                                          | `UIUtil.getDecoratedRowColor()` is the platform's striped-row color                                                                              | One constant, three readers (`BaseCard`, `GridPanelBuilder` ×2, `lightmode/FailureForm`)             | Rule-EDITOR-PANEL-250 asks for a stripe, not these grays                                                                                                              |
| The whole table UI delegate replaced                                         | `editor/grid/GridPanelBuilder.java` `buildTable` — `table.setUI(new BasicTableUI())` | `JBTable` ships a UI that honors the theme; `BasicTableUI` is Swing's generic one, so the grid opts out of every IntelliJ table behavior at once | Unknown until tried. This is the single largest customization in the plugin and the least documented | Rule-EDITOR-PANEL-250: the pointer passing over a row changes nothing. Likely why `BasicTableUI` was taken, since the platform UI paints a hovered row. Read, not run |
| A letter drawn into an icon                                                  | `util/Icons.java` `fieldLetter`, `LetterIcon` (`fieldLetter`, `LetterIcon`)          | `com.intellij.ui.TextIcon` — which `ui/dialogs/DialogStyle.java` `trailing` already uses for the keycap note                                     | Medium: `TextIcon` sizes itself from the font, the hand-drawn one centres in a fixed box             | Rule-EDITOR-PANEL-209 asks for the letters, not for how they are drawn                                                                                                |
| A toolbar button painting its own background                                 | `editor/AbstractIconButton.java` `paintComponent`                                    | `ActionButton` and `InplaceButton` paint hover and pressed states from the theme                                                                 | Large: the button also owns its own hover and selection state                                        | Rule-EDITOR-PANEL-248: a button gray under the pointer loses its hover look at once                                                                                   |

## The platform does not ship this

| What                                            | Where                                                                                   | Why nothing fits                                                                                                                 | Rule in docs/                                |
|-------------------------------------------------|-----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| The dialog card: a rounded fill plus a hairline | `ui/dialogs/SectionFill.java` `paintBorder`                                             | `RoundedLineBorderWithBackground` is exactly this and is `@ApiStatus.Internal`. Verified in the SDK, 2026-09-27                  | Rule-INTERNAL-099                            |
| The unfocused field frame                       | `ui/dialogs/FieldFrame.java` `FieldFrame`                                               | It **extends** `DarculaTextBorder` and only adds the unfocused stroke, which is the intended way to specialise a platform border | Rule-INTERNAL-096                            |
| A chosen row as a rounded band                  | `ui/framework/TextFieldWithSelections.java` `SelectionRenderer.paintComponent`          | A list selection is a rectangle everywhere in Swing. Rule-INTERNAL-106                                                           | Rule-INTERNAL-106                            |
| Badge pills                                     | `ui/Badges.java` `fillPill`, `ui/Tag.java` `paint`                                      | No platform pill. The shape carries severity and priority together                                                               | Rule-EDITOR-PANEL-252, Rule-EDITOR-PANEL-253 |
| The verdict donut                               | `view/marker/VerdictDonut.java` `paintComponent`, `view/marker/Swatch.java` `paintIcon` | No platform chart of any kind                                                                                                    | None                                         |
| The grid's selected-cell edge                   | `editor/grid/SelectionCellBorder.java` `paintBorder`                                    | A table cell border is the caller's in Swing                                                                                     | None                                         |
| Light mode's key button                         | `lightmode/KeyBtn.java` `paintComponent`                                                | Related: `ui/framework/Keycap` draws the same idea for dialogs, so **these two are each other's duplicate**, not the platform's  | None                                         |
| Analysis category colors parsed from `#rrggbb`  | `model/ResultAnalysis.java` `onScreen`                                                  | The colors are the tester's own data, not the IDE's                                                                              | None                                         |

## Decided, not drift

These are recorded decisions. They are listed so nobody "cleans" them.

| What                                                          | Where                                                | The decision                                                                            | Rule in docs/                      |
|---------------------------------------------------------------|------------------------------------------------------|-----------------------------------------------------------------------------------------|------------------------------------|
| `GRAY`, `RED`, `GREEN` are a `JBColor` whose halves are equal | `util/Icons.java` `GRAY`, `RED`, `GREEN`             | Testin's icons are one color in both themes, on purpose - the pair says so              | Rule-INTERNAL-077                  |
| Run status badge colors                                       | `model/RunStatus.java` `PASSED`, `FAILED`, `RUNNING` | Passed, failed and running read as green, red and amber to a tester regardless of theme | None - a decision with no rule yet |
| The dialog surfaces                                           | `ui/dialogs/DialogStyle.java` `CONTENT`              | Two named theme colors, chosen per theme by luminance. Rule-INTERNAL-099                | Rule-INTERNAL-099                  |

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
