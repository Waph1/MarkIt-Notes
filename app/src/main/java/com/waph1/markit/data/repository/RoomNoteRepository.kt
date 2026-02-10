package com.waph1.markit.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.waph1.markit.data.database.AppDatabase
import com.waph1.markit.data.database.NoteDao
import com.waph1.markit.data.database.NoteEntity
import com.waph1.markit.data.database.LabelDao
import com.waph1.markit.data.database.LabelEntity
import com.waph1.markit.data.model.AppConfig
import com.waph1.markit.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.util.Date
import androidx.room.withTransaction

class RoomNoteRepository(
    private val context: Context,
    private val metadataManager: MetadataManager
) : NoteRepository {
    
    private val noteDao: NoteDao = AppDatabase.getDatabase(context).noteDao()
    private val labelDao: LabelDao = AppDatabase.getDatabase(context).labelDao()
    private var rootDir: DocumentFile? = null
    private var appConfig = AppConfig()
    
    private val contentCache = mutableMapOf<String, Pair<Long, String>>()

    private data class FrontMatterData(val color: Long, val reminder: Long?, val cleanContent: String)

    private fun parseFrontMatter(rawContent: String): FrontMatterData {
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
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        
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

    private fun constructFileContent(note: Note): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
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

    override suspend fun setRootFolder(uriString: String) {
        val uri = Uri.parse(uriString)
        rootDir = DocumentFile.fromTreeUri(context, uri)
        
        rootDir?.let {
            appConfig = metadataManager.loadConfig(it)
        }
        
        syncFilesToDatabase()
    }
    
    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllActiveNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }

    override fun getAllNotesWithArchive(): Flow<List<Note>> {
        return noteDao.getAllNotesWithArchive().map { entities ->
            entities.map { it.toNote() }
        }
    }
    
    fun getTrashedNotes(): Flow<List<Note>> {
        return noteDao.getTrashedNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }
    
    fun getArchivedNotes(): Flow<List<Note>> {
        return noteDao.getArchivedNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }
    
    fun getNotesByFolder(folder: String): Flow<List<Note>> {
        return noteDao.getNotesByFolder(folder).map { entities ->
            entities.map { it.toNote() }
        }
    }
    
    override fun getLabels(): Flow<List<String>> = labelDao.getAllLabels() 
    
    override suspend fun createLabel(name: String): Boolean = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext false
        if (name.isBlank()) return@withContext false
        
        val existing = root.findFile(name)
        if (existing != null && existing.isDirectory) {
             labelDao.insert(LabelEntity(name))
             return@withContext true
        }
        
        val newDir = root.createDirectory(name)
        if (newDir != null) {
            labelDao.insert(LabelEntity(name))
            return@withContext true
        }
        return@withContext false
    }

    override suspend fun deleteLabel(name: String): Boolean = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext false
        val count = noteDao.countNotesInFolder(name)
        if (count > 0) return@withContext false
        
        root.findFile(name)?.delete()
        root.findFile(".Archive")?.findFile(name)?.delete()
        root.findFile(".Deleted")?.findFile(name)?.delete()
        
        labelDao.delete(name)
        return@withContext true
    }
    
    override suspend fun getNote(id: String): Note? {
        return noteDao.getNoteByPath(id)?.toNote()
    }
    
    override suspend fun saveNote(note: Note, oldFile: java.io.File?): String = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext ""
        val folderName = if (note.folder.isNullOrEmpty() || note.folder == "Unknown") "Inbox" else note.folder
        
        // Determine the actual root for searching/saving based on status
        val effectiveRoot = when {
            note.isTrashed -> root.findFile(".Deleted") ?: root.createDirectory(".Deleted")
            note.isArchived -> root.findFile(".Archive") ?: root.createDirectory(".Archive")
            else -> root
        } ?: root

        var targetDir = effectiveRoot.findFile(folderName) ?: effectiveRoot.createDirectory(folderName) ?: return@withContext ""
        
        if (note.isPinned && !note.isArchived && !note.isTrashed) {
            targetDir = targetDir.findFile("Pinned") ?: targetDir.createDirectory("Pinned") ?: targetDir
        }

        var baseTitle = note.title.trim().ifEmpty { "Untitled" }
        var finalFileName = "$baseTitle.md"
        
        var conflict = false
        var targetFileDoc = targetDir.findFile(finalFileName)
        
        if (targetFileDoc != null) {
             if (oldFile != null && oldFile.name == finalFileName) {
                 conflict = false
             } else {
                 conflict = true
             }
        }
        
        var counter = 1
        var finalTitle = baseTitle
        while (conflict) {
            finalTitle = "$baseTitle ($counter)"
            finalFileName = "$finalTitle.md"
            targetFileDoc = targetDir.findFile(finalFileName)
            if (targetFileDoc == null) conflict = false else counter++
        }

        val filePath = "$folderName/$finalFileName"
        
        if (oldFile != null) {
             val oldName = oldFile.name
             val oldParentName = oldFile.parent ?: "Inbox"
             
             // Check in all possible locations for the old file
             val locations = listOf(root, root.findFile(".Archive"), root.findFile(".Deleted"))
             var oldFileDoc: DocumentFile? = null
             for (loc in locations) {
                 if (loc == null) continue
                 oldFileDoc = loc.findFile(oldParentName)?.findFile(oldName) ?:
                              loc.findFile(oldParentName)?.findFile("Pinned")?.findFile(oldName)
                 if (oldFileDoc != null) break
             }
             
             if (oldFileDoc != null && oldFileDoc.uri != targetFileDoc?.uri) {
                 if (oldName != finalFileName || oldFileDoc.parentFile?.name != targetDir.name) {
                     oldFileDoc.delete()
                     noteDao.deleteNoteByPath("$oldParentName/$oldName")
                 }
             }
        }

        if (targetFileDoc == null) {
            targetFileDoc = targetDir.createFile("text/markdown", finalFileName)
        }
        
        val fullContent = constructFileContent(note)
        
        targetFileDoc?.let { doc ->
            context.contentResolver.openOutputStream(doc.uri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(fullContent)
                }
            }
        }
        
        val entity = NoteEntity(
            filePath = filePath,
            fileName = finalFileName,
            folder = folderName,
            title = finalTitle,
            contentPreview = note.content.take(200),
            content = note.content,
            lastModifiedMs = System.currentTimeMillis(),
            color = note.color,
            reminder = note.reminder,
            isPinned = note.isPinned,
            isArchived = note.isArchived,
            isTrashed = note.isTrashed
        )
        noteDao.insertNote(entity)
        
        return@withContext filePath
    }
    
    override suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        moveNoteToSystemFolder(id, ".Deleted")
        noteDao.trashNote(id)
    }
    
    override suspend fun deleteNotes(noteIds: List<String>) = withContext(Dispatchers.IO) {
        noteIds.forEach { id ->
            moveNoteToSystemFolder(id, ".Deleted")
            noteDao.trashNote(id)
        }
    }
    
    override suspend fun archiveNote(id: String) = withContext(Dispatchers.IO) {
        moveNoteToSystemFolder(id, ".Archive")
        noteDao.archiveNote(id)
    }
    
    override suspend fun archiveNotes(noteIds: List<String>) = withContext(Dispatchers.IO) {
        noteIds.forEach { id ->
            moveNoteToSystemFolder(id, ".Archive")
            noteDao.archiveNote(id)
        }
    }
    
    override suspend fun restoreNote(id: String) = withContext(Dispatchers.IO) {
        val entity = noteDao.getNoteByPath(id) ?: return@withContext
        val root = rootDir ?: return@withContext
        
        val folder = entity.folder
        val fileName = entity.fileName
        
        val deletedSource = root.findFile(".Deleted")?.findFile(folder)?.findFile(fileName)
        val archiveSource = root.findFile(".Archive")?.findFile(folder)?.findFile(fileName)
        val sourceFile = deletedSource ?: archiveSource
        
        if (sourceFile != null) {
            val targetFolder = root.findFile(folder) ?: root.createDirectory(folder)
            val content = readText(sourceFile)
            val newFile = targetFolder?.createFile("text/markdown", fileName)
            newFile?.let { nf ->
                context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                    OutputStreamWriter(os).use { it.write(content) }
                }
                sourceFile.delete()
            }
        }
        
        noteDao.restoreNote(id)
    }
    
    override suspend fun setNoteColor(id: String, color: Long) = withContext(Dispatchers.IO) {
        val entity = noteDao.getNoteByPath(id) ?: return@withContext
        val note = entity.toNote().copy(color = color)
        saveNote(note, note.file)
    }
    
    override suspend fun togglePinStatus(noteIds: List<String>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext
        
        noteIds.forEach { id ->
            val entity = noteDao.getNoteByPath(id) ?: return@forEach
            if (entity.isPinned == isPinned) return@forEach
            
            val folderName = entity.folder
            val fileName = entity.fileName
            
            var sourceFile = root.findFile(folderName)?.findFile(fileName)
            if (sourceFile == null) sourceFile = root.findFile(folderName)?.findFile("Pinned")?.findFile(fileName)
            
            if (sourceFile != null) {
                 val targetDirParent = root.findFile(folderName) ?: root.createDirectory(folderName)
                 val targetDir = if (isPinned) {
                     targetDirParent?.findFile("Pinned") ?: targetDirParent?.createDirectory("Pinned")
                 } else {
                     targetDirParent
                 }
                 
                 if (targetDir != null) {
                      val content = readText(sourceFile)
                      val newFile = targetDir.createFile("text/markdown", fileName)
                      newFile?.let { nf ->
                          context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                              OutputStreamWriter(os).use { it.write(content) }
                          }
                          sourceFile.delete()
                      }
                 }
            }
            
            noteDao.updatePinStatus(id, isPinned)
        }
    }
    
    override suspend fun moveNotes(notes: List<Note>, targetFolder: String) = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext
        
        notes.forEach {
            val fileName = it.file.name
            val sourceFolder = it.folder
            val isArchived = it.isArchived
            val isTrashed = it.isTrashed
            val isPinned = it.isPinned

            val effectiveRoot = when {
                isTrashed -> root.findFile(".Deleted")
                isArchived -> root.findFile(".Archive")
                else -> root
            }

            var sourceFile = effectiveRoot?.findFile(sourceFolder)?.findFile(fileName)
            if (sourceFile == null && !isTrashed && !isArchived) {
                 sourceFile = effectiveRoot?.findFile(sourceFolder)?.findFile("Pinned")?.findFile(fileName)
            }
            
            var targetRoot = when {
                isTrashed -> root.findFile(".Deleted")
                isArchived -> root.findFile(".Archive") ?: root.createDirectory(".Archive")
                else -> root
            }
            
            var targetFolderDoc = targetRoot?.findFile(targetFolder) ?: targetRoot?.createDirectory(targetFolder)
            
            if (isPinned && !isTrashed && !isArchived) {
                targetFolderDoc = targetFolderDoc?.findFile("Pinned") ?: targetFolderDoc?.createDirectory("Pinned")
            }

            if (sourceFile != null && targetFolderDoc != null) {
                val content = readText(sourceFile)
                val newFile = targetFolderDoc.createFile("text/markdown", fileName)
                newFile?.let { nf ->
                    context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                        OutputStreamWriter(os).use { it.write(content) }
                    }
                    sourceFile.delete()
                }
                
                val oldPath = "$sourceFolder/$fileName"
                val newPath = "$targetFolder/$fileName"
                val entity = noteDao.getNoteByPath(oldPath)
                if (entity != null) {
                    noteDao.deleteNoteByPath(oldPath)
                    noteDao.insertNote(entity.copy(filePath = newPath, folder = targetFolder))
                }
            }
        }
    }
    
    private suspend fun syncFilesToDatabase() = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext
        
        try {
            val allEntities = mutableListOf<NoteEntity>()
            if (root.findFile("Inbox") == null) root.createDirectory("Inbox")
            
            root.listFiles().filter { it.isDirectory && !it.name!!.startsWith(".") }.forEach { folder ->
                scanFolderToEntities(folder, isArchived = false, isTrashed = false, allEntities)
            }
            
            root.findFile(".Archive")?.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                scanFolderToEntities(folder, isArchived = true, isTrashed = false, allEntities)
            }
            
            root.findFile(".Deleted")?.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                scanFolderToEntities(folder, isArchived = false, isTrashed = true, allEntities)
            }
            
            val labels = mutableSetOf<String>()
            root.listFiles().filter { it.isDirectory && !it.name!!.startsWith(".") }.forEach { 
                 it.name?.let { name -> labels.add(name) }
            }
            
            AppDatabase.getDatabase(context).withTransaction {
                noteDao.deleteAll()
                noteDao.insertNotes(allEntities)
                labelDao.deleteAll()
                labels.forEach { labelDao.insert(LabelEntity(it)) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun scanFolderToEntities(
        folder: DocumentFile,
        isArchived: Boolean,
        isTrashed: Boolean,
        output: MutableList<NoteEntity>
    ) {
        val folderName = folder.name ?: return
        
        suspend fun processFiles(dir: DocumentFile, isPinned: Boolean) {
            dir.listFiles().filter { it.isFile && (it.name?.endsWith(".md") == true || it.name?.endsWith(".txt") == true) }.forEach { file ->
                val fileName = file.name ?: return@forEach
                val rawContent = readText(file)
                val metadata = parseFrontMatter(rawContent)
                
                val color = if (metadata.color != 0xFFFFFFFF) metadata.color else appConfig.fileColors[fileName] ?: 0xFFFFFFFF
                val reminder = metadata.reminder
                val lastModified = file.lastModified()
                
                output.add(
                    NoteEntity(
                        filePath = "$folderName/$fileName",
                        fileName = fileName,
                        folder = folderName,
                        title = fileName.substringBeforeLast("."),
                        contentPreview = metadata.cleanContent.take(200),
                        content = metadata.cleanContent,
                        lastModifiedMs = lastModified,
                        color = color,
                        reminder = reminder,
                        isPinned = isPinned,
                        isArchived = isArchived,
                        isTrashed = isTrashed
                    )
                )
            }
        }

        processFiles(folder, isPinned = false)
        
        if (!isArchived && !isTrashed) {
            val pinnedDir = folder.findFile("Pinned")
            if (pinnedDir != null) {
                processFiles(pinnedDir, isPinned = true)
            }
        }
    }
    
    private suspend fun moveNoteToSystemFolder(id: String, systemFolderName: String) {
        val root = rootDir ?: return
        val entity = noteDao.getNoteByPath(id) ?: return
        val folder = entity.folder
        val fileName = entity.fileName
        
        var sourceFile = root.findFile(folder)?.findFile(fileName) ?:
                         root.findFile(folder)?.findFile("Pinned")?.findFile(fileName) ?:
                         root.findFile(".Archive")?.findFile(folder)?.findFile(fileName) ?:
                         root.findFile(".Deleted")?.findFile(folder)?.findFile(fileName)
        
        if (sourceFile != null) {
            val sysRoot = root.findFile(systemFolderName) ?: root.createDirectory(systemFolderName)
            val targetLabelFolder = sysRoot?.findFile(folder) ?: sysRoot?.createDirectory(folder)
            
            if (targetLabelFolder != null) {
                val content = readText(sourceFile)
                val newFile = targetLabelFolder.createFile("text/markdown", fileName)
                newFile?.let { nf ->
                    context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                        OutputStreamWriter(os).use { it.write(content) }
                    }
                    sourceFile.delete()
                }
            }
        }
    }
    
    private suspend fun readText(file: DocumentFile): String = withContext(Dispatchers.IO) {
        val pathKey = file.uri.toString()
        val lastModified = file.lastModified()
        
        val cached = contentCache[pathKey]
        if (cached != null && cached.first == lastModified) {
            return@withContext cached.second
        }
        
        try {
            context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                val text = BufferedReader(inputStream.reader()).use { it.readText() }
                contentCache[pathKey] = Pair(lastModified, text)
                text
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    override suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext
        val deletedDir = root.findFile(".Deleted")
        
        deletedDir?.listFiles()?.forEach { 
            it.delete()
        }
        
        noteDao.deleteAllTrashed()
    }
    
    private fun NoteEntity.toNote(): Note {
        return Note(
            file = java.io.File(folder, fileName),
            title = title,
            content = content,
            lastModified = Date(lastModifiedMs),
            color = color,
            reminder = reminder,
            isPinned = isPinned,
            isArchived = isArchived,
            isTrashed = isTrashed
        )
    }
}