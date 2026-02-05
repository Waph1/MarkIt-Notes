package com.waph1.markit.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Note.
 * This is the "cached" version of what's on disk.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val filePath: String,         // Unique ID: "Label/filename.md"
    val fileName: String,         // "MyNote.md"
    val folder: String,           // "Inbox", "Work", etc.
    val title: String,
    val contentPreview: String,   // First ~200 chars for dashboard
    val content: String,          // Full content
    val lastModifiedMs: Long,     // Timestamp in millis
    val color: Long,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isTrashed: Boolean
)
