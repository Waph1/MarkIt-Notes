package com.waph1.markit.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.waph1.markit.data.database.AppDatabase
import com.waph1.markit.data.database.NoteDao
import com.waph1.markit.data.database.NoteEntity
import com.waph1.markit.data.model.AppConfig
import com.waph1.markit.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.util.Date

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
    
    fun getLabels(): Flow<List<String>> = noteDao.getAllLabels()
    
    override suspend fun getNote(id: String): Note? {
        return noteDao.getNoteByPath(id)?.toNote()
    }
    
    override suspend fun saveNote(note: Note, oldFile: java.io.File?): String = withContext(Dispatchers.IO) {
        val root = rootDir ?: return@withContext ""
        
        val targetFolderName = if (note.folder.isNullOrEmpty() || note.folder == "Unknown") "Inbox" else note.folder
        val fileName = "${note.title}.md"
        val filePath = "$targetFolderName/$fileName"
        
        // Ensure folder exists
        var targetFolderDoc = root.findFile(targetFolderName) ?: root.createDirectory(targetFolderName)
        
        // Handle rename: delete old file if name changed
        if (oldFile != null && oldFile.name != fileName) {
            val oldParent = oldFile.parent ?: "Inbox"
            root.findFile(oldParent)?.findFile(oldFile.name)?.delete()
            noteDao.deleteNoteByPath("$oldParent/${oldFile.name}")
        }
        
        // Write to disk
        var fileDoc = targetFolderDoc?.findFile(fileName)
        if (fileDoc == null) {
            fileDoc = targetFolderDoc?.createFile("text/markdown", fileName)
        }
        
        fileDoc?.let { doc ->
            context.contentResolver.openOutputStream(doc.uri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(note.content)
                }
            }
        }
        
        // Update metadata
        appConfig.fileColors[fileName] = note.color
        if (note.isPinned) appConfig.pinnedFiles.add(fileName) else appConfig.pinnedFiles.remove(fileName)
        metadataManager.saveConfig(root, appConfig)
        
        // Update Room
        val entity = NoteEntity(
            filePath = filePath,
            fileName = fileName,
            folder = targetFolderName,
            title = note.title,
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
        val targetFolderDoc = root.findFile(targetFolder) ?: root.createDirectory(targetFolder)
        
        notes.forEach { note ->
            val fileName = note.file.name
            val sourceFolder = note.folder
            val sourceFile = root.findFile(sourceFolder)?.findFile(fileName)
            
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
        
        // Replace all in Room
        noteDao.deleteAll()
        noteDao.insertNotes(allEntities)
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
