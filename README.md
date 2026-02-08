# MarkIt (v0.4.0)

**MarkIt** is a modern, beautiful, and privacy-focused Markdown note-taking app for Android. Designed with a clean Google Keep-inspired aesthetic, it puts your content first while giving you the power of Markdown formatting.

![MarkIt Icon](app/src/main/res/drawable/ic_launcher_foreground.png)

## About
MarkIt differs from other note apps by treating your notes as **real files**. There is no hidden database; every note is a plain text Markdown (`.md`) file stored in a folder of your choice on your device. This means you truly own your data—you can sync it, back it up, or open it with any other text editor.

## Features

*   **File-Based Storage**: Your notes are yours. Stored as local `.md` files.
*   **Markdown Support**: Full Markdown rendering and editing (Bold, Italic, Lists, Checkbox, etc.).
*   **Grid Layout**: A beautiful staggered grid view (like Google Keep) or a single-column list view.
*   **Localization**: Support for English and Italian. Defaults to English.
*   **Organization**:
    *   **Folders as Labels**: Use folders to organize notes into categories.
    *   **Pins**: Keep important notes at the top.
    *   **Colors**: Color-code your notes for visual grouping.
    *   **Archive & Trash**: Keep your workspace clutter-free.
    *   **Label Management**: Long-press labels in the sidebar to delete empty ones.
    *   **Empty Trash**: Permanently clear your trash with a single tap.
*   **Privacy First**: No internet permissions required. No tracking. No cloud lock-in.
*   **Modern UI**: Built with Jetpack Compose and Material 3, featuring Dark Mode support.
*   **Smooth UX**: Splash screen matching theme, instant Dark Mode rendering, and fluid screen transitions.
*   **Intelligent Search**: Powerful search with "Search everywhere" fallback. If no results are found in your current view, MarkIt automatically searches through all active and archived notes.
*   **Advanced Sorting**: Sort notes by Date Modified, Created, or Title.

## Installation

1.  Download the latest APK from the [Releases](https://github.com/Waph1/MarkIt/releases) page.
2.  Install it on your Android device.
3.  On first launch, select a folder where you want your notes to live.

## Building from source

1.  Clone the repository:
    ```bash
    git clone https://github.com/Waph1/MarkIt.git
    ```
2.  Open in **Android Studio**.
3.  Build and Run.

## License

[MIT License](LICENSE) (or whichever license you choose)
