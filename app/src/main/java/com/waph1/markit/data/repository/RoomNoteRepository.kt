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

/**
 * Repository that bridges Room (fast UI) and the File System (source of truth).
 * 
 * Read Path: UI observes Room via Flow.
 * Write Path: Changes go to Disk first, then sync to Room.
 */
class RoomNoteRepository(
    private val context: Context,
    private val metadataManager: MetadataManager
) : NoteRepository {
    
    private val noteDao: NoteDao = AppDatabase.getDatabase(context).noteDao()
    private val labelDao: LabelDao = AppDatabase.getDatabase(context).labelDao()
    private var rootDir: DocumentFile? = null
    private var appConfig = AppConfig()
    
    // Content Cache: URI -> Pair(LastModified, Content)
    private val contentCache = mutableMapOf<String, Pair<Long, String>>()
    
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
             // Ensure it's in DB even if exists on disk (e.g. after clear)
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
        
        // 1. Check if empty in DB
        val count = noteDao.countNotesInFolder(name)
        if (count > 0) return@withContext false
        
        // 2. Delete from disk (Active, Archive, Deleted subfolders)
        root.findFile(name)?.delete()
        root.findFile(".Archive")?.findFile(name)?.delete()
        root.findFile(".Deleted")?.findFile(name)?.delete()
        
        // 3. Delete from DB
        labelDao.delete(name)
        
        return@withContext true
    }
    
    override suspend fun getNote(id: String): Note? {
        return noteDao.getNoteByPath(id)?.toNote()
    }
    
    override suspend fun saveNote(note: Note, oldFile: java.io.File?): String = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext ""
        
        val targetFolderName = if (note.folder.isNullOrEmpty() || note.folder == "Unknown") "Inbox" else note.folder
        
        // --- Unique Filename Logic ---
        var baseTitle = note.title.trim()
        if (baseTitle.isEmpty()) baseTitle = "Untitled"
        // Sanitize title for filename? (Already assumed mostly safe, but good to be careful. Repository assumes valid title from UI?)
        
        var finalFileName = "$baseTitle.md"
        val targetFolderDoc = root.findFile(targetFolderName) ?: root.createDirectory(targetFolderName) ?: return@withContext ""
        
        // If we are just saving the SAME file (no rename), allow overwrite.
        // If oldFile is present and has same name and is in same folder, it's an overwrite.
        // logic: check if target exists. If it exists and is NOT our oldFile, we conflict.
        
        var conflict = false
        var targetFileDoc = targetFolderDoc.findFile(finalFileName)
        
        if (targetFileDoc != null) {
            // File exists. Is it us?
            if (oldFile != null && oldFile.name == finalFileName) {
                // It is us (same name).
                // Check if we moved folder?
                // oldFile path logic is a bit loose here passed from UI.
                // But generally if name matches oldFile.name, we assume it's the intended overwrite.
                conflict = false
            } else {
                // It exists, and it's NOT our old file (either logic says new file, or we renamed to a name that exists).
                conflict = true
            }
        }
        
        var counter = 1
        var finalTitle = baseTitle
        while (conflict) {
            finalTitle = "$baseTitle ($counter)"
            finalFileName = "$finalTitle.md"
            targetFileDoc = targetFolderDoc.findFile(finalFileName)
            if (targetFileDoc == null) {
                conflict = false
            } else {
                // Should also check if THIS collision is somehow our old file?
                // Unlikely if we are incrementing away from base.
                counter++
            }
        }
        // -----------------------------

        val filePath = "$targetFolderName/$finalFileName"
        
        // Handle rename: delete old file if name changed (AND we didn't just decide effectively to keep it? No, we are making a NEW file effectively if renamed)
        if (oldFile != null && oldFile.name != finalFileName) {
            val oldParent = oldFile.parent ?: "Inbox"
            // Start of fix for phantom files:
            // If we renamed, the old file was at oldParent/oldFile.name
            root.findFile(oldParent)?.findFile(oldFile.name)?.delete()
            noteDao.deleteNoteByPath("$oldParent/${oldFile.name}")
        }
        
        // Write to disk
        // targetFileDoc might be null if we resolved conflict to a new non-existent file
        if (targetFileDoc == null) {
            targetFileDoc = targetFolderDoc.createFile("text/markdown", finalFileName)
        }
        
        targetFileDoc?.let { doc ->
            context.contentResolver.openOutputStream(doc.uri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(note.content)
                }
            }
        }
        
        // Update metadata
        appConfig.fileColors[finalFileName] = note.color
        if (note.isPinned) appConfig.pinnedFiles.add(finalFileName) else appConfig.pinnedFiles.remove(finalFileName)
        metadataManager.saveConfig(root, appConfig)
        
        // Update Room
        val entity = NoteEntity(
            filePath = filePath,
            fileName = finalFileName,
            folder = targetFolderName,
            title = finalTitle,
            contentPreview = note.content.take(200),
            content = note.content,
            lastModifiedMs = System.currentTimeMillis(),
            color = note.color,
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
        
        // Find source (in .Deleted or .Archive)
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
        noteDao.updateColor(id, color)
        val root = rootDir ?: return@withContext
        val entity = noteDao.getNoteByPath(id) ?: return@withContext
        appConfig.fileColors[entity.fileName] = color
        metadataManager.saveConfig(root, appConfig)
    }
    
    override suspend fun togglePinStatus(noteIds: List<String>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        noteIds.forEach { id ->
            noteDao.updatePinStatus(id, isPinned)
        }
        val root = rootDir ?: return@withContext
        noteIds.forEach { id ->
            val entity = noteDao.getNoteByPath(id)
            if (entity != null) {
                if (isPinned) appConfig.pinnedFiles.add(entity.fileName)
                else appConfig.pinnedFiles.remove(entity.fileName)
            }
        }
        metadataManager.saveConfig(root, appConfig)
    }
    
    override suspend fun moveNotes(notes: List<Note>, targetFolder: String) = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext
        
        notes.forEach { note ->
            val fileName = note.file.name
            val sourceFolder = note.folder
            val isArchived = note.isArchived
            val isTrashed = note.isTrashed

            // Determine Root based on status
            val effectiveRoot = when {
                isTrashed -> root.findFile(".Deleted")
                isArchived -> root.findFile(".Archive")
                else -> root
            }

            // Source File
            val sourceFile = effectiveRoot?.findFile(sourceFolder)?.findFile(fileName)
            
            // Target Folder logic
            // Maintain status: If archived, move within .Archive. If active, move within root.
            val targetRoot = when {
                isTrashed -> root.findFile(".Deleted")
                isArchived -> root.findFile(".Archive") ?: root.createDirectory(".Archive")
                else -> root
            }
            
            val targetFolderDoc = targetRoot?.findFile(targetFolder) ?: targetRoot?.createDirectory(targetFolder)

            if (sourceFile != null && targetFolderDoc != null) {
                val content = readText(sourceFile)
                val newFile = targetFolderDoc.createFile("text/markdown", fileName)
                newFile?.let { nf ->
                    context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                        OutputStreamWriter(os).use { it.write(content) }
                    }
                    sourceFile.delete()
                }
                
                // Update Room
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
    
    // --- Sync Logic ---
    
    private suspend fun syncFilesToDatabase() = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext
        
        try {
            val allEntities = mutableListOf<NoteEntity>()
            
            // Ensure Inbox exists
            if (root.findFile("Inbox") == null) root.createDirectory("Inbox")
            
            // Scan visible folders
            root.listFiles().filter { it.isDirectory && !it.name!!.startsWith(".") }.forEach { folder ->
                scanFolderToEntities(folder, isArchived = false, isTrashed = false, allEntities)
            }
            
            // Scan .Archive
            root.findFile(".Archive")?.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                scanFolderToEntities(folder, isArchived = true, isTrashed = false, allEntities)
            }
            
            // Scan .Deleted
            root.findFile(".Deleted")?.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                scanFolderToEntities(folder, isArchived = false, isTrashed = true, allEntities)
            }
            
           // Prepare Labels OUTSIDE transaction (I/O)
            val labels = mutableSetOf<String>()
            root.listFiles().filter { it.isDirectory && !it.name!!.startsWith(".") }.forEach { 
                 it.name?.let { name -> labels.add(name) }
            }
            
            // Replace all in Room with Transaction to avoid empty state blink
            AppDatabase.getDatabase(context).withTransaction {
                noteDao.deleteAll()
                noteDao.insertNotes(allEntities)
                
                // Sync Labels
                labelDao.deleteAll()
                labels.forEach { labelDao.insert(LabelEntity(it)) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Log error or set error state
        }
    }
    
    private suspend fun scanFolderToEntities(
        folder: DocumentFile,
        isArchived: Boolean,
        isTrashed: Boolean,
        output: MutableList<NoteEntity>
    ) {
        val folderName = folder.name ?: return
        
        folder.listFiles().filter { it.isFile && (it.name?.endsWith(".md") == true || it.name?.endsWith(".txt") == true) }.forEach { file ->
            val fileName = file.name ?: return@forEach
            val content = readText(file)
            val color = appConfig.fileColors[fileName] ?: 0xFFFFFFFF
            val isPinned = appConfig.pinnedFiles.contains(fileName)
            val lastModified = appConfig.customTimestamps[fileName] ?: file.lastModified()
            
            output.add(
                NoteEntity(
                    filePath = "$folderName/$fileName",
                    fileName = fileName,
                    folder = folderName,
                    title = fileName.substringBeforeLast("."),
                    contentPreview = content.take(200),
                    content = content,
                    lastModifiedMs = lastModified,
                    color = color,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    isTrashed = isTrashed
                )
            )
        }
    }
    
    private suspend fun moveNoteToSystemFolder(id: String, systemFolderName: String) {
        val root = rootDir ?: return
        val entity = noteDao.getNoteByPath(id) ?: return
        val folder = entity.folder
        val fileName = entity.fileName
        
        // Preserve timestamp
        appConfig.customTimestamps[fileName] = entity.lastModifiedMs
        metadataManager.saveConfig(root, appConfig)
        
        val sourceFile = root.findFile(folder)?.findFile(fileName)
            ?: root.findFile(".Archive")?.findFile(folder)?.findFile(fileName)
        
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
    
    // --- Converters ---
    
    private fun NoteEntity.toNote(): Note {
        return Note(
            file = java.io.File(folder, fileName),
            title = title,
            content = content,
            lastModified = Date(lastModifiedMs),
            color = color,
            isPinned = isPinned,
            isArchived = isArchived,
            isTrashed = isTrashed
        )
    }
}
