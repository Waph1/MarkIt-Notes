package com.waph1.markitnotes.data.utils

import com.waph1.markitnotes.data.model.Note
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

object NoteFormatUtils {

    data class FrontMatterData(val color: Long, val reminder: Long?, val cleanContent: String)

    fun parseFrontMatter(rawContent: String): FrontMatterData {
        val lines = rawContent.lines()
        if (lines.size < 3 || lines[0].trim() != "---") {
            return FrontMatterData(0xFFFFFFFF, null, rawContent)
        }
        
        val closingIndexInDropped = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (closingIndexInDropped == -1) {
            return FrontMatterData(0xFFFFFFFF, null, rawContent)
        }
        
        val actualClosingIndex = closingIndexInDropped + 1
        val yamlLines = lines.subList(1, actualClosingIndex)
        
        var contentStartIndex = actualClosingIndex + 1
        while (contentStartIndex < lines.size && lines[contentStartIndex].isBlank()) {
            contentStartIndex++
        }
        
        val cleanContent = if (contentStartIndex < lines.size) {
            lines.subList(contentStartIndex, lines.size).joinToString("\n")
        } else {
            ""
        }
        
        var color = 0xFFFFFFFF
        var reminder: Long? = null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        yamlLines.forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                when (key) {
                    "color" -> {
                        try {
                            if (value.startsWith("#")) {
                                val hex = value.substring(1)
                                color = java.lang.Long.parseUnsignedLong(hex, 16)
                                if (hex.length == 6) color = color or 0xFF000000
                            } else {
                                color = value.toLong()
                            }
                        } catch (e: Exception) {}
                    }
                    "reminder" -> {
                        try {
                            reminder = dateFormat.parse(value)?.time
                        } catch (e: Exception) {
                            reminder = value.toLongOrNull()
                        }
                    }
                }
            }
        }
        
        return FrontMatterData(color, reminder, cleanContent)
    }

    fun constructFileContent(note: Note): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            append("---\n")
            append(String.format("color: #%08X\n", note.color))
            note.reminder?.let {
                append("reminder: ")
                append(dateFormat.format(Date(it)))
                append("\n")
            }
            append("---\n\n")
            append(note.content)
        }
    }
}