# Implementation Plan - MarkIt Updates

## Completed Tasks

### 1. UI Enhancements (Multi-Selection & Icons)
- **Archive Icon Position:** Moved the Archive icon to the left of the Trash icon in the multi-selection top bar for better logical grouping.
- **Unified Icon Style:** Updated all icons in the Note Editor and Note Viewer to use the "Outlined" style, matching the Dashboard's multi-selection mode.
- **Label Icon Sync:** Updated the "Change Label" icon in the Editor to use the `DriveFileMove` icon, consistent with the multi-selection menu.

### 2. Multi-Selection Logic Improvements
- **Archive/Restore Context:**
    - Enabled "Unarchive" (Restore) action directly from Label views when archived notes are selected.
    - Added mixed selection handling: "Archive" and "Restore" actions are hidden if a mix of archived and active notes is selected.
- **Move Logic:**
    - Fixed `moveNotes` to correctly handle archived notes. Moving an archived note to a new label now moves it within the `.Archive` system folder, preserving its status.

### 3. Note Creation & Editor Logic
- **Contextual Creation:** New notes created while viewing a specific label are now automatically assigned to that label.
- **Stale State Fix:** Fixed an issue where the Editor would cache the initial label and not update when switching contexts.
- **Folder Extraction Fix:** Added a placeholder filename for new notes to ensure the `java.io.File` logic correctly extracts the parent folder path.

### 4. Trash Management
- **Empty Trash:** Added a "Empty Trash" button in the Trash view with a confirmation dialog.
- **Data Cleanup:** Implemented physical file deletion from the `.Deleted` folder and removal from the Room database.

### 5. Performance & Bug Fixes
- **Scrolling Stability:** 
    - Added stable keys to `LazyVerticalStaggeredGrid` items.
    - Refactored `NoteGrid` to use a unified item list (Headers + Notes) to prevent layout jumps.
    - Added a `Spacer` item at the end of the list instead of using container padding to fix overscroll clipping issues.
- **Startup Flash Fix:** 
    - Updated themes to support Day/Night modes at the window level.
    - Added a centered streamlined logo to the splash screen.
- **WebView Flash Fix:** 
    - Set `WebView` background to transparent.
    - Implemented instant theming in `preview.html` via URL parameters to prevent white flashes in Dark Mode.
- **Smooth Transitions:** 
    - Replaced `AnimatedContent` with an overlay approach in `MainActivity`. The Dashboard now stays active in the background when the Editor is open, eliminating stutter when closing notes.

### 6. Label Management
- **Delete Empty Labels**: 
    - Added long-press support to labels in the side menu.
    - Implemented a check to ensure labels are empty before allowing deletion.
    - **Fix:** Custom `Surface`-based implementation for drawer items to reliably support both click and long-press gestures.

### 7. Intelligent Search
- **Search Everywhere Fallback:**
    - Implemented `getAllNotesWithArchive()` in `NoteDao` and `NoteRepository` to fetch all non-trashed notes.
    - Updated `MainViewModel` to use a multi-flow `combine` that reacts to filter, sort, query, and global note changes.
    - Added logic to fallback to global results if the current filtered view returns zero matches.
- **Search UI Indicators:**
    - Added "Search results" and "Search everywhere" headers to `NoteGrid` to provide clear context to the user.
    - Exposed `isSearchEverywhere` state from ViewModel to trigger the specific header.

### 8. Localization
- **String Externalization**:
    - Scanned and replaced all hardcoded strings in `DashboardScreen.kt`, `EditorScreen.kt`, and `SearchBar.kt` with `stringResource(R.string...)` or `context.getString(...)`.
    - Consolidated all base strings in the default `app/src/main/res/values/strings.xml`.
- **Italian Translation**:
    - Created `app/src/main/res/values-it/strings.xml` with complete Italian translations.
    - Verified that the app correctly switches to Italian based on system locale.

### 9. View Mode (Grid/List)
- **Persistence**: Added `ViewMode` enum to `PrefsManager` to store user preference (GRID vs LIST).
- **ViewModel**: Exposed `viewMode` state flow and `setViewMode` toggle logic.
- **UI Toggle**: Integrated a toggle button in the `SearchBar` (left of Sort) that switches icons and updates state.
- **Dynamic Layout**: Modified `NoteGrid` to dynamically calculate `StaggeredGridCells.Fixed` based on the active `viewMode` (1 for list, 2 for grid).

## Documentation Updated