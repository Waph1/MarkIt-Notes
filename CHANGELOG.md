# Changelog

## [5.2.0] - 2026-02-14
### Added
- **Feature**: Added "Pull to Refresh" functionality in the main notes list to sync files manually.
- **Feature**: Enabled auto-capitalization for sentences in the note editor (Title and Content).

### Changed
- **Refactor**: Renamed application to "MarkIt Notes" and package to `com.waph1.markitnotes`.
- **Refactor**: Removed legacy `FileNoteRepository` and consolidated data access in `RoomNoteRepository`.
- **Polish**: Fixed various compiler warnings and optimized imports.

## [5.0.1] - 2026-02-11
### Added
- **UI**: Added a 3-dots overflow menu for "Archive" and "Delete" actions in the Dashboard and Editor.
- **UI**: Added "Empty Trash" action to a 3-dots menu in the Trash view.
- **Localization**: Added missing strings for "More options" in English and Italian.

### Changed
- **UX**: Disabled the swipe gesture to open the side navigation drawer to prevent accidental triggers.

## [0.2.2] - 2026-02-04
### Changed
- **UI**: Updated App Icon to "Adaptive Icon" format.
- **UI**: Removed white border from app icon by using a dark background (`#202124`).

## [0.2.1] - 2026-01-27
### Changed
- **Polish**: Globalized all hardcoded strings to `strings.xml`.
- **Performance**: Added 300ms debounce to search for smoother typing.
- **Refactor**: Extracted `SearchBar` component for cleaner code.
- **UI**: Improved Search Bar visuals and menu icons.

## [0.2.0] - 2026-01-27
### Added
- **Search Bar**: Users can now search notes by title and content.
- **Sorting**: Added a Sort button to the Search Bar (right side) to sort notes by Date Modified, Date Created, and Title.
- **Refactor**: Renamed package to `com.waph1.markit`.
- **UI**: Moved Sort button to the right of the search bar.
- **UI**: Added "Clear Search" button.
