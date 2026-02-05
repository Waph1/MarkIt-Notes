# Changelog

All notable changes to this project will be documented in this file.

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
