# KeepNotes Walkthrough

## Overview
A file-based, Google Keep-style notes app built from scratch with Jetpack Compose.
- **Storage**: Markdown files in a user-selected folder.
- **Labels**: Folders act as labels.
- **UI**: Staggered Grid with pastel colors.

## Verification
### Build Status
> [!NOTE]
> Build Successful!

The APK is ready for testing.
Location: `keepnotes/app/build/outputs/apk/debug/app-debug.apk`

### Installation
```bash
adb install -r keepnotes/app/build/outputs/apk/debug/app-debug.apk
```

### Features to Test
1.  **First Launch**:
    *   Click "Select Root Folder".
    *   Grant access to a folder (e.g., create a new `MyNotes` folder on your phone).
2.  **Creating Notes**:
    *   Tap the `+` FAB.
    *   Enter Title and Content.
    *   Select a Color.
    *   **Select a Label**: Type a new Label (e.g., "Work") or leave it empty (defaults to "Inbox").
    *   Press Back to save.
3.  **Inbox Logic**:
    *   Create a text file in your root folder using a file manager.
    *   Open KeepNotes.
    *   Verify the file is moved to the `Inbox` folder and appears in the app.
### New Features to Test (v2)
1.  **Sidebar**:
    *   Tap the **Menu Icon** (top left).
    *   Verify you see: Notes, Labels (folders), Archive, Trash.
2.  **Filtering**:
    *   Select a Label. Verify notes from that folder are shown.
    *   **Archived Separation**: Archive a note with that label. Verify it appears at the bottom under "Archived notes".
3.  **Trash & Archive**:
    *   Open a Note.
    *   Tap **Archive** (Arrow Down icon). Verify it moves to Archive view.
    *   Tap **Delete** (Trash icon). Verify it moves to Trash view.
    *   **Restore**: Go to Trash/Archive, open the note, tap **Restore** (Refresh icon). Verify it returns to its original label/folder.
4.  **Auto-Save & Persistence**:
    *   Change a note color. Close the app completely. Reopen. Color should persist.
    *   Create a note. Close app. Reopen. Note should load instantly (Caching).
5.  **Label & Trash Management (v0.4.0)**:
    *   **Delete Label**: Open sidebar, long-press a label. Confirm deletion. (Try with non-empty label to see error).
    *   **Empty Trash**: Go to Trash view. Tap the Trash icon in the top bar. Confirm to clear all deleted notes.
    *   **Archive from Editor**: Open a note, tap Archive. Verify it closes and moves to archive.
    *   **Contextual Creation**: Go to a label (e.g. "Work"). Tap `+`. Note should already have "Work" label selected.
    *   **Search everywhere**: 
        *   Create a note in a label (e.g. "Work").
        *   Switch to another label (e.g. "Home") where the note doesn't exist.
        *   Search for that note.
        *   Verify the note appears under a **"Search everywhere"** header.
        *   Verify that while searching in "All Notes", archived notes also appear if no active notes match the query.
4.  **Localization**:
    *   Change the device language to **Italian**.
    *   Open MarkIt and verify all UI elements (menus, buttons, hints, headers) are displayed in Italian.
    *   Change the device language to any other language (e.g., French or back to English).
    *   Verify the app defaults to **English**.
5.  **View Mode**:
    *   Locate the new button to the left of the Sort button in the Search Bar.
    *   Tap it to switch between **Grid View** (2 columns) and **List View** (1 column).
    *   Close and reopen the app to verify the selected view mode is persisted.


### 4. Startup & Labels Fixes (Debugging)
- **Startup Logic**: Modified `MainViewModel` to check `PrefsManager` immediately upon initialization, preventing the "Select Folder" screen from flashing or appearing unnecessarily.
- **Permission Handling**: Updated `MainActivity` to check for revoked permissions and reset the UI state if needed.
- **Label Filtering**: Relaxed filtering in `MainViewModel` to include "Inbox" and correctly map folder names to labels. Implemented `NoteFilter` sealed interface to handle All/Label/Archive/Trash states cleanly.
- **Folder Creation**: Verified that typing a new folder name in the Editor's label field creates the directory upon saving.

### 5. Sorting & Performance (v3)
- **Sorting**: Added a "Sort" menu in the dashboard TopAppBar. Supports:
    -   **Criteria**: Date Created (Last Modified), Title.
    -   **Direction**: Ascending, Descending.
    -   Persists selection across app restarts.
- **Performance**:
    -   **Optimistic Updates**: Archiving, restoring, and deleting notes now updates the UI immediately while file operations occur in the background.
    -   **Content Caching**: Note content is cached in memory (keyed by URI + LastModified) to reduce repeated file reads during `refreshNotes`.

### 6. Fixes & Polish (v4)
- **Editor Navigation**: Added `BackHandler` to `EditorScreen` to prevent the system back button from closing the entire app. It now saves and exits the editor properly.
- **Note Renaming**: Fixed a bug where renaming a note would create a duplicate file. The app now detects renaming events and deletes the old file.
- **Cold Start Persistence**: Implemented a JSON disk cache (`notes_cache.json`) for notes. This allows the app to display notes instantly upon startup while it verifies the file system in the background, resolving "cold start" latency.

### Multi-Selection Color Change
- **Bulk Action**: Select multiple notes and change their color via the top bar palette icon.
- **Sort Prevention**: This action explicitly preserves the sort order of all affected notes.

### Markdown Editor (WYSIWYG)
- **Rich Text Editing**: The editor now behaves like Google Keep. As you type `**text**` it effectively displays **text**. You can use the toolbar to toggle styles (Bold, Italic, Underline, Lists).
- **Code Mode**: You can view the underlying Markdown syntax by clicking the generic "Menu/Edit" icon in the top toolbar to switch to "Code Mode".
- **Synchronization**: Changes made in Rich mode are saved as Markdown. Changes in Code mode are reflected in Rich mode upon switching.
- **Dashboard Preview**: Note previews in the main screen now render Markdown (WYSIWYG) instead of raw syntax.
- **Optimized Saves**: Implemented "Initial State Caching" to prevent the editor from flagging a note as modified simply because the Markdown library normalized the syntax (e.g., changing `*` to `_`).
- **Click Handling**: Added touch overlays to rendered previews so clicking anywhere on the note correctly opens it.
- **Keyboard Handling**: Added `imePadding` to the editor layout so the text area resizes correctly when the keyboard opens, ensuring the cursor is always visible.
- **UI Redesign**: 
    - **Dynamic Bottom Bar**: Replaced clutter with a clean 3-state bar (Default, Colors, Formatting).
    - **Smart Logic**: Formatting options automatically appear when you start typing (keyboard opens) but stay available even if you hide the keyboard.
    - **Refined Icons**: Custom "Underlined A" icon for formatting, left-aligned for easy access.
    - **Active Feedback**: Bold, Italic, and Underline buttons now highlight (Circular shape) when the cursor is on formatted text, letting you know exactly what style is active.
    - **Visual Polish**: Reduced text box contrast in Dark Mode to provide a seamless writing surface.
    - **Indentation**: Added a dedicated Indent button (`→|`) that inserts explicit spacing (4 spaces), giving you manual control to align text or lists as needed without interfering with standard behavior.
- **Implementation**: Uses `mohamedrejeb/richeditor-compose` library.

### Version 1.2 (Current)
- **Pin Notes**: Pin important notes to the top.
- **Persistence Fix**: Resolved issue where colors/pins were lost on restart (renamed config file to `keepnotes_config.json` for better compatibility).
- **Format Bar**: Improved formatting controls.
- **Safety**: Added confirmation dialog before deleting multiple notes.
- **Interaction**: Long-press anywhere on a note to select it.
- **Rebrand**: App is now **MarkIt** with a new custom icon.

### 5. Advanced Organization
- **Visual Pinning**: You can now **Pin** important notes to the top.
    - Pinned notes appear in a dedicated **"Pinned" section** at the top of the dashboard.
    - Long-press to select -> Tap Star to toggle.
- **Smart Sorting**: Pinned notes stay at the top; others follow your sort order.

### Verification of Fixes
- **Sort Order**: Confirmed that Restore, Move, and Color Change (Multi & Single) do *not* bump notes to the top.
- **Build**: Successfully assembled debug build.

### 7. Refinements (v5)
- **Dirty Check**: The Editor now checks if Title, Content, Color, or Folder have *actually* changed before saving. This prevents updating the "Last Modified" timestamp just by viewing a note.
- **Optimistic Save**: Saving a note updates the UI list instantly in memory before performing file I/O, removing the perceived 1-2 second lag.
- **Sorting Label**: Renamed "Date Created" to "Date Modified" in the sort menu to accurately reflect the sorting behavior (based on file last modified time).

### 8. Refinements (v6)
- **Scroll Preservation**: The `LazyStaggeredGridState` is now hoisted to `MainActivity`. This ensures that when the user returns from the Editor screen, the dashboard list remains at the exact same scroll position as before.
- **Move Logic**: Changing a note's label (folder) now properly moves the file instead of copying it. The `saveNote` function was updated to detect folder changes and delete the old file.
