package com.waph1.markit.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.waph1.markit.data.model.AppConfig
import com.waph1.markit.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.util.Date
import java.util.regex.Pattern

class FileNoteRepository(
    private val context: Context,
    private val metadataManager: MetadataManager
) : NoteRepository {
    private var rootDir: DocumentFile? = null
    
    // Cache
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    private val _labels = MutableStateFlow<List<String>>(emptyList())
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading
    
    // In-memory cache for legacy support if needed
    private var appConfig = AppConfig()

    // Content Cache: Path -> Pair(LastModified, Content)
    private val contentCache = mutableMapOf<String, Pair<Long, String>>()

    // Regex for Front Matter
    private val frontMatterRegex = Pattern.compile("""^---\n([\s\S]*?)\n---\n""")
    private val keyValueRegex = Pattern.compile("""(\w+):\s*(.*)""")

    override suspend fun setRootFolder(uriString: String) {
        val uri = Uri.parse(uriString)
        rootDir = DocumentFile.fromTreeUri(context, uri)
        
        loadCacheFromDisk()
        
        rootDir?.let {
            appConfig = metadataManager.loadConfig(it)
        }
        refreshNotes()
    }

    override fun getAllNotes(): Flow<List<Note>> = _notes.map {
        it.filter { !it.isArchived && !it.isTrashed }
    }

    override fun getAllNotesWithArchive(): Flow<List<Note>> = _notes.map {
        it.filter { !it.isTrashed }
    }

    private data class FrontMatterData(val color: Long, val reminder: Long?, val cleanContent: String)

    private fun parseFrontMatter(rawContent: String): FrontMatterData {
        val matcher = frontMatterRegex.matcher(rawContent)
        var color = 0xFFFFFFFF
        var reminder: Long? = null
        var content = rawContent

        if (matcher.find()) {
            val yamlBlock = matcher.group(1) ?: ""
            content = rawContent.substring(matcher.end())
            
            yamlBlock.lines().forEach {
                val kvMatcher = keyValueRegex.matcher(it)
                if (kvMatcher.find()) {
                    val key = kvMatcher.group(1)
                    val value = kvMatcher.group(2)?.trim()
                    when (key) {
                        "color" -> value?.toLongOrNull()?.let { color = it }
                        "reminder" -> value?.toLongOrNull()?.let { reminder = it }
                    }
                }
            }
        }
        return FrontMatterData(color, reminder, content)
    }

    private fun constructFileContent(note: Note): String {
        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("color: ").append(note.color).append("\n")
        if (note.reminder != null) {
            sb.append("reminder: ").append(note.reminder).append("\n")
        }
        sb.append("---\n")
        sb.append(note.content)
        return sb.toString()
    }

    suspend fun refreshNotes() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val notesList = mutableListOf<Note>()
        rootDir?.let { root ->
            // Ensure Inbox
            var inboxDir = root.findFile("Inbox") ?: root.createDirectory("Inbox")
            
            // Move root files to Inbox
            val rootFiles = root.listFiles()
            for (file in rootFiles) {
                if (file.isFile && (file.name?.endsWith(".md") == true || file.name?.endsWith(".txt") == true)) {
                    inboxDir?.let { targetDir ->
                         try {
                             val content = readText(file) // Reads raw content
                             val newFile = targetDir.createFile("text/markdown", file.name ?: "Untitled.md")
                             newFile?.let { nf ->
                                 context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                                     OutputStreamWriter(os).use { it.write(content) }
                                 }
                                 file.delete()
                             }
                         } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }

            suspend fun scanFolder(folder: DocumentFile, labelName: String, isArchived: Boolean, isTrashed: Boolean) {
                val folderFiles = folder.listFiles()
                
                // 1. Process regular files in this folder
                for (file in folderFiles) {
                    if (file.isFile && (file.name?.endsWith(".md") == true || file.name?.endsWith(".txt") == true)) {
                        val rawContent = readText(file)
                        val metadata = parseFrontMatter(rawContent)
                        
                        // Check if file is physically in a "Pinned" folder?
                        // No, scanFolder is called on "Inbox", "Work", etc.
                        // So regular files here are NOT pinned.
                        val isPinned = folder.name == "Pinned" 

                        notesList.add(
                            Note(
                                file = java.io.File(labelName, file.name ?: ""),
                                title = file.name?.substringBeforeLast(".") ?: "Untitled",
                                content = metadata.cleanContent,
                                lastModified = Date(file.lastModified()), // Ignore legacy custom timestamps for now
                                color = metadata.color,
                                reminder = metadata.reminder,
                                isPinned = isPinned,
                                isArchived = isArchived,
                                isTrashed = isTrashed
                            )
                        )
                    }
                }
                
                // 2. Check for "Pinned" subfolder if we are in a main label folder (not archived/deleted)
                if (!isArchived && !isTrashed) {
                    val pinnedDir = folder.findFile("Pinned")
                    if (pinnedDir != null && pinnedDir.isDirectory) {
                        for (file in pinnedDir.listFiles()) {
                            if (file.isFile && (file.name?.endsWith(".md") == true || file.name?.endsWith(".txt") == true)) {
                                val rawContent = readText(file)
                                val metadata = parseFrontMatter(rawContent)
                                
                                notesList.add(
                                    Note(
                                        file = java.io.File(labelName, file.name ?: ""), // Logic keeps Label as parent
                                        title = file.name?.substringBeforeLast(".") ?: "Untitled",
                                        content = metadata.cleanContent,
                                        lastModified = Date(file.lastModified()),
                                        color = metadata.color,
                                        reminder = metadata.reminder,
                                        isPinned = true, // Implicitly pinned
                                        isArchived = isArchived,
                                        isTrashed = isTrashed
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // 1. Scan Visible Folders (Labels)
            val visibleLabelFolders = root.listFiles().filter { it.isDirectory && !it.name!!.startsWith(".") }
            _labels.value = visibleLabelFolders.mapNotNull { it.name }
            for (labelFolder in visibleLabelFolders) {
                scanFolder(labelFolder, labelFolder.name ?: "Unknown", isArchived = false, isTrashed = false)
            }
            
            // 2. Scan .Archive
            val archiveRoot = root.findFile(".Archive")
            if (archiveRoot != null) {
                val archivedLabelFolders = archiveRoot.listFiles().filter { it.isDirectory }
                for (labelFolder in archivedLabelFolders) {
                    scanFolder(labelFolder, labelFolder.name ?: "Unknown", isArchived = true, isTrashed = false)
                }
            }

            // 3. Scan .Deleted
            val deletedRoot = root.findFile(".Deleted")
            if (deletedRoot != null) {
                val deletedLabelFolders = deletedRoot.listFiles().filter { it.isDirectory }
                for (labelFolder in deletedLabelFolders) {
                    scanFolder(labelFolder, labelFolder.name ?: "Unknown", isArchived = false, isTrashed = true)
                }
            }
        }
        _notes.value = notesList
        saveCacheToDisk(notesList)
        _isLoading.value = false
    }

    override suspend fun getNote(id: String): Note? {
        return _notes.value.find { it.file.name == id }
    }

    override suspend fun saveNote(note: Note, oldFile: java.io.File?): String = withContext(Dispatchers.IO) {
        val currentNotes = _notes.value.toMutableList()
        val oldNoteRef = currentNotes.find { it.file.name == (oldFile?.name ?: note.file.name) }
        
        // Determine Target Folder
        val labelName = note.folder.ifEmpty { "Inbox" }
        val isPinned = note.isPinned
        
        // Prepare Content with FrontMatter
        val fullContent = constructFileContent(note)
        
        rootDir?.let { root ->
            val labelDir = root.findFile(labelName) ?: root.createDirectory(labelName)
            var targetDir = labelDir
            
            if (isPinned && labelDir != null) {
                targetDir = labelDir.findFile("Pinned") ?: labelDir.createDirectory("Pinned")
            }
            
            val fileName = "${note.title}.md"
            
            // Handle Renaming / Moving
            if (oldFile != null) {
                // If the old file is different (different name, or we changed pinned status so folder changed)
                // Note: note.isPinned logic is handled by targetDir. 
                // We need to know where the OLD file was to delete it.
                // oldFile is just a java.io.File with (Folder, Name). It doesn't know about "Pinned" subfolder.
                // We must rely on the existing note data or search.
                
                val oldLabel = oldFile.parent ?: "Inbox"
                val oldName = oldFile.name
                
                // Find existing file doc to delete/overwrite
                // Check normal
                var oldFileDoc = root.findFile(oldLabel)?.findFile(oldName)
                // Check Pinned
                if (oldFileDoc == null) {
                    oldFileDoc = root.findFile(oldLabel)?.findFile("Pinned")?.findFile(oldName)
                }
                
                // If we are renaming or moving (changing pinned status constitutes a move), delete old
                val isRename = oldName != fileName
                // Changing pin status changes target folder.
                // Check if targetDir is different from where we found oldFileDoc
                val isMove = oldFileDoc?.parentFile?.uri != targetDir?.uri
                
                if ((isRename || isMove) && oldFileDoc != null) {
                    try { oldFileDoc.delete() } catch (e: Exception) { e.printStackTrace() }
                }
            } else {
                 // Safety cleanup for overwrite by name if not renaming
                 targetDir?.findFile(fileName)?.delete()
            }
            
            targetDir?.createFile("text/markdown", fileName)?.let { newFile ->
                context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(fullContent)
                    }
                }
            }
            refreshNotes()
        }
        return@withContext "${note.title}.md"
    }

    override suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        // Find the note to get its location
        val note = _notes.value.find { it.file.name == id } ?: return@withContext
        moveNoteToSystemFolder(note, ".Deleted")
        refreshNotes()
    }

    override suspend fun archiveNote(id: String) = withContext(Dispatchers.IO) {
        val note = _notes.value.find { it.file.name == id } ?: return@withContext
        moveNoteToSystemFolder(note, ".Archive")
        refreshNotes()
    }
    
    override suspend fun restoreNote(id: String) {
        withContext(Dispatchers.IO) {
        val note = _notes.value.find { it.file.name == id } ?: return@withContext
        // Move back to Inbox (or original label if we tracked it, but data model uses label as parent)
        val label = note.folder
        val fileName = note.file.name
        
        rootDir?.let { root ->
            // Find Source
            var sourceFile: DocumentFile? = null
            sourceFile = root.findFile(".Deleted")?.findFile(label)?.findFile(fileName)
            if (sourceFile == null) sourceFile = root.findFile(".Archive")?.findFile(label)?.findFile(fileName)
            
            if (sourceFile != null) {
                val rawContent = readText(sourceFile)
                // Target
                val targetLabel = root.findFile(label) ?: root.createDirectory(label)
                val targetFile = targetLabel?.createFile("text/markdown", fileName)
                
                targetFile?.let { tf ->
                    context.contentResolver.openOutputStream(tf.uri)?.use { os ->
                         OutputStreamWriter(os).use { it.write(rawContent) }
                    }
                    sourceFile.delete()
                }
            }
            refreshNotes()
        }
    }
    }

    override suspend fun deleteNotes(noteIds: List<String>) = withContext(Dispatchers.IO) {
        val notesToDelete = _notes.value.filter { noteIds.contains(it.file.path) || noteIds.contains(it.file.name) }
        notesToDelete.forEach { moveNoteToSystemFolder(it, ".Deleted") }
        refreshNotes()
    }

    override suspend fun archiveNotes(noteIds: List<String>) = withContext(Dispatchers.IO) {
        val notesToArchive = _notes.value.filter { noteIds.contains(it.file.path) || noteIds.contains(it.file.name) }
        notesToArchive.forEach { moveNoteToSystemFolder(it, ".Archive") }
        refreshNotes()
    }

    override suspend fun moveNotes(notes: List<Note>, targetFolder: String) = withContext(Dispatchers.IO) {
         rootDir?.let { root ->
             val targetDir = root.findFile(targetFolder) ?: root.createDirectory(targetFolder)
             val archiveRoot = root.findFile(".Archive") ?: root.createDirectory(".Archive")
             val archiveTarget = archiveRoot?.findFile(targetFolder) ?: archiveRoot?.createDirectory(targetFolder)
             
             notes.forEach { note ->
                 val fileName = note.file.name
                 // Determine correct target based on current state
                 var finalTargetDir = if (note.isArchived) archiveTarget else targetDir
                 
                 // If moving a Pinned note, it should go to Label/Pinned
                 if (note.isPinned && !note.isArchived) {
                     finalTargetDir = targetDir?.findFile("Pinned") ?: targetDir?.createDirectory("Pinned")
                 }

                 if (note.folder != targetFolder && finalTargetDir != null) {
                     // Find Source
                     val currentLabel = note.folder
                     var sourceFile: DocumentFile? = null
                     
                     // Check Pinned first if pinned
                     if (note.isPinned) {
                         sourceFile = root.findFile(currentLabel)?.findFile("Pinned")?.findFile(fileName)
                     }
                     if (sourceFile == null) {
                         sourceFile = root.findFile(currentLabel)?.findFile(fileName)
                     }
                     // Check Archive
                     if (sourceFile == null) {
                         sourceFile = root.findFile(".Archive")?.findFile(currentLabel)?.findFile(fileName)
                     }
                     
                     if (sourceFile != null) {
                          try {
                              val content = readText(sourceFile)
                              val newFile = finalTargetDir.createFile("text/markdown", fileName)
                              newFile?.let { nf ->
                                  context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                                      OutputStreamWriter(os).use { it.write(content) }
                                  }
                                  sourceFile.delete()
                              }
                          } catch (e: Exception) { e.printStackTrace() }
                     }
                 }
             }
        }
        refreshNotes()
    }
    
    override suspend fun setNoteColor(id: String, color: Long) = withContext(Dispatchers.IO) {
        // Rewrite file with new metadata
        val note = _notes.value.find { it.file.name == id } ?: return@withContext
        val newNote = note.copy(color = color)
        // We use saveNote to handle the rewrite + metadata
        saveNote(newNote, note.file)
    }
    
    override suspend fun togglePinStatus(noteIds: List<String>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val notesToToggle = _notes.value.filter { noteIds.contains(it.file.path) || noteIds.contains(it.file.name) }
        
        notesToToggle.forEach {
            if (it.isPinned != isPinned) {
                 val newNote = it.copy(isPinned = isPinned)
                 saveNote(newNote, it.file)
            }
        }
    }

    private suspend fun moveNoteToSystemFolder(note: Note, systemFolderName: String) {
         rootDir?.let { root ->
            val folderName = note.folder
            val fileName = note.file.name
            
            var sourceFile: DocumentFile? = null
            
            // Check Active folders
            // Check Pinned
            if (note.isPinned) {
                 sourceFile = root.findFile(folderName)?.findFile("Pinned")?.findFile(fileName)
            }
            if (sourceFile == null) sourceFile = root.findFile(folderName)?.findFile(fileName)
            
            // If not found, check .Archive / .Deleted
             if (sourceFile == null) {
                val archiveRoot = root.findFile(".Archive")
                val archiveLabel = archiveRoot?.findFile(folderName)
                sourceFile = archiveLabel?.findFile(fileName)
            }

            if (sourceFile != null) {
                // Create Target System Folder
                var sysRoot = root.findFile(systemFolderName) ?: root.createDirectory(systemFolderName)
                var targetLabelFolder = sysRoot?.findFile(folderName) ?: sysRoot?.createDirectory(folderName)
                
                if (targetLabelFolder != null) {
                     try {
                          val content = readText(sourceFile)
                          val newFile = targetLabelFolder.createFile("text/markdown", fileName)
                          newFile?.let { nf ->
                              context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                                  OutputStreamWriter(os).use { it.write(content) }
                              }
                              sourceFile.delete()
                          }
                     } catch (e: Exception) { e.printStackTrace() }
                }
            }
         }
    }

    private suspend fun readText(file: DocumentFile): String = withContext(Dispatchers.IO) {
        val lastModified = file.lastModified()
        val pathKey = file.uri.toString()
        
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
    
    // Legacy Disk Cache methods preserved...
    private fun saveCacheToDisk(notes: List<Note>) {
        try {
            val json = com.google.gson.Gson().toJson(notes)
            val file = java.io.File(context.filesDir, "notes_cache.json")
            file.writeText(json)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadCacheFromDisk() {
        try {
            val file = java.io.File(context.filesDir, "notes_cache.json")
            if (file.exists()) {
                val json = file.readText()
                val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
                val notes: List<Note> = com.google.gson.Gson().fromJson(json, type)
                _notes.value = notes
            }
        } catch (e: Exception) { e.printStackTrace() }
    }


    override fun getLabels(): Flow<List<String>> = _labels

    override suspend fun createLabel(name: String): Boolean = withContext(Dispatchers.IO) {
        rootDir?.let { root ->
            try {
                if (root.findFile(name) == null) {
                    root.createDirectory(name)
                    refreshNotes()
                    return@withContext true
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        return@withContext false
    }

    override suspend fun deleteLabel(name: String): Boolean = false

    override suspend fun emptyTrash() {}
}