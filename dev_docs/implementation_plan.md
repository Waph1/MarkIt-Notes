# Markdown Editor Upgrade Plan

## Goal
Enhance the Note Editor to support Markdown rendering, rich text formatting tools (Bold, Italic, etc.), and a raw code editing mode, similar to Google Keep but backed by Markdown files.

## User Review Required
> [!IMPORTANT]
> I will add the `com.github.jeziellago:compose-markdown:0.5.0` library to render the markdown content.

## Proposed Changes

### Build Configuration
#### [MODIFY] [build.gradle.kts](file:///home/gianfranco/Documents/Poject/keepnotes/app/build.gradle.kts)
- Add `implementation("com.github.jeziellago:compose-markdown:0.5.0")`
#### [MODIFY] [MainViewModel.kt](file:///home/gianfranco/Documents/Poject/keepnotes/app/src/main/java/com/example/keepnotes/ui/MainViewModel.kt)
#### [MODIFY] [DashboardScreen.kt](file:///home/gianfranco/Documents/Poject/keepnotes/app/src/main/java/com/example/keepnotes/ui/DashboardScreen.kt)

### Pin Feature
#### [MODIFY] [NoteRepository.kt](file:///home/gianfranco/Documents/Poject/keepnotes/app/src/main/java/com/example/keepnotes/data/repository/NoteRepository.kt)
- Add `togglePinStatus(noteIds: List<String>, isPinned: Boolean)`

#### [MODIFY] [FileNoteRepository.kt](file:///home/gianfranco/Documents/Poject/keepnotes/app/src/main/java/com/example/keepnotes/data/repository/FileNoteRepository.kt)
- Implement pin logic (Update `AppConfig.pinnedFiles`).

#### [MODIFY] [MainViewModel.kt](file:///home/gianfranco/Documents/Poject/keepnotes/app/src/main/java/com/example/keepnotes/ui/MainViewModel.kt)
- Add `togglePin(notes: List<Note>)`.

#### [MODIFY] [DashboardScreen.kt](file:///home/gianfranco/Documents/Poject/keepnotes/app/src/main/java/com/example/keepnotes/ui/DashboardScreen.kt)
- Add Pin Icon to Selection Bar.
- Logic to split or sort Pinned notes to top.

### UI Layer

#### [MODIFY] [EditorScreen.kt](file:///home/gianfranco/Documents/Poject/keepnotes/app/src/main/java/com/example/keepnotes/ui/EditorScreen.kt)
- **State**: Add `isPreviewMode` (Boolean) state.
- **Top Bar**: Add "Visible/Code" toggle icon (Eye vs Code icon).
- **Content**:
    - If `isPreviewMode`: Show `MarkdownText` (Rendered View).
    - If `!isPreviewMode`: Show `TextField` (Raw Editor).
- **Bottom Bar**: Add `FormattingToolbar` composable.
    - Buttons: Bold, Italic, Title (#), List (-), Checkbox ([ ]).
    - Logic: Inserts markdown syntax at the current cursor position or wrapping deletion.

### Logic
- **Cursor Handling**: Need to change `String` state to `TextFieldValue` state to track cursor position for inserting formatting tags correctly.

## Verification Plan
### Manual Verification
- **Toolbar**: Click "Bold" -> Inserts `****` and positions cursor.
- **Preview**: Switch mode -> See rendered bold text.
- **Save**: Confirm file is saved with markdown syntax.
