# Changelog

All notable changes to this project will be documented in this file.

## [0.4.0] - 2026-02-08
### Added
- **Search Everywhere**: 
    - Implemented a "Search everywhere" fallback logic: if a search query returns no results in the current filter (Label or All Notes), the app automatically expands the search to include all active and archived notes.
    - Added a clear "Search everywhere" header in the UI to indicate when results are coming from the broader search.
    - Added a "Search results" header for standard filtered searches.
- **Localization**:
    - Added full support for the **Italian** language.
    - Externalized all UI strings to `strings.xml` for easier future translations.
    - The app defaults to English unless the device language is set to Italian.
- **View Mode**:
    - Added a button to toggle between **Grid View** (2 columns) and **List View** (1 column).
    - The user's preference is persisted across app restarts.
- **Label Management**:
    - Users can now delete empty labels by long-pressing them in the side navigation drawer.
    - Added validation to prevent deleting labels that still contain notes (active, archived, or trashed).
- **Trash Management**:
    - Added "Empty Trash" feature with a confirmation dialog to permanently delete all notes in the trash.
- **Editor Enhancements**:
    - Added Archive and Unarchive buttons directly in the note editor top bar.
    - New notes created while viewing a label are now automatically assigned to that label.
- **Visuals & UX**:
    - Custom splash screen with a centered "MarkIt" logo that respects Day/Night mode.
    - Improved screen transitions: opening and closing notes is now buttery smooth with an overlay transition that preserves the dashboard state.
    - Fixed white flashes in Dark Mode by implementing instant theming in the Markdown preview engine.

### Fixed
- **Scrolling**: Resolved a critical bug where the note grid would jump or "clip back" when scrolling near the bottom of the list.
- **Archived Note Moving**: Fixed a bug where moving an archived note to a new label would accidentally unarchive it or lose its file reference.
- **Note Creation**: Fixed an issue where new notes would sometimes default to the "Inbox" even when created within a specific label.
- **UI Consistency**: Unified icon styles across the app to use "Outlined" variants.

## [0.3.1] - 2026-02-06
### Changed
- **Maintenance**: Updated project dependencies to 2024 standards.
    - Updated Kotlin to `1.9.22`.
    - Updated Compose to BOM `2024.02.00` / Compiler `1.5.8`.
    - Updated AGP to `8.2.2`.

## [0.3.0] - 2026-02-05
### Added
- **Search & Filter**: Logic to search notes by title and content with a 300ms debounce for performance.
- **Sorting**: Sort notes by Title, Date Modified, or Date Created (Ascending/Descending).
- **New UI Components**:
    - dedicated `SearchBar` with clear button and sort menu.
    - Adaptive App Icon (Dark Mode compatible, no white border).
### Changed
- **Refactoring**:
    - Globalized all strings to `strings.xml`.
    - Cleaned up `DashboardScreen` by extracting components.
- **Documentation**: Updated README with accurate features and icon.

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
