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
    
    // In-memory cache of metadata (color, pinned)
    private var appConfig = AppConfig()

    // Content Cache: Path -> Pair(LastModified, Content)
    private val contentCache = mutableMapOf<String, Pair<Long, String>>()

    override suspend fun setRootFolder(uriString: String) {
        val uri = Uri.parse(uriString)
        rootDir = DocumentFile.fromTreeUri(context, uri)
        
        // Load Disk Cache immediately
        loadCacheFromDisk()
        
        rootDir?.let {
            appConfig = metadataManager.loadConfig(it)
        }
        refreshNotes()
    }

    override fun getAllNotes(): Flow<List<Note>> = _notes.map { list ->
        list.filter { !it.isArchived && !it.isTrashed }
    }

    override fun getAllNotesWithArchive(): Flow<List<Note>> = _notes.map { list ->
        list.filter { !it.isTrashed }
    }

    suspend fun refreshNotes() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val notesList = mutableListOf<Note>()
        rootDir?.let { root ->
            // Ensure Inbox
            var inboxDir = root.findFile("Inbox") ?: root.createDirectory("Inbox")
            
            // Move root files to Inbox... (Existing logic preserved)
            val rootFiles = root.listFiles()
            for (file in rootFiles) {
                if (file.isFile && (file.name?.endsWith(".md") == true || file.name?.endsWith(".txt") == true)) {
                    inboxDir?.let { targetDir ->
                         try {
                             val content = readText(file)
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

            // Define scan helper
            suspend fun scanFolder(folder: DocumentFile, isArchived: Boolean, isTrashed: Boolean) {
                val folderFiles = folder.listFiles()
                for (file in folderFiles) {
                    if (file.isFile && (file.name?.endsWith(".md") == true || file.name?.endsWith(".txt") == true)) {
                        val content = readText(file) // Slow but necessary for now
                        val color = appConfig.fileColors[file.name] ?: 0xFFFFFFFF
                        // Determine "Label" (Folder Name)
                        // If we are in .Deleted/Work/Note.md, 'folder' is 'Work'.
                        // But current logic: file.parent is just the immediate parent name.
                        // So correct label is folder.name
                        
                        // Read Timestamp: Prefer Custom -> FS
                        val savedTimestamp = appConfig.customTimestamps[file.name]
                        val finalLastModified = if (savedTimestamp != null) Date(savedTimestamp) else Date(file.lastModified())

                        notesList.add(
                            Note(
                                file = java.io.File(folder.name, file.name ?: ""),
                                title = file.name?.substringBeforeLast(".") ?: "Untitled",
                                content = content,
                                lastModified = finalLastModified,
                                color = color,
                                isPinned = appConfig.pinnedFiles.contains(file.name),
                                isArchived = isArchived,
                                isTrashed = isTrashed
                            )
                        )
                    } else if (file.isDirectory && !file.name!!.startsWith(".")) {
                        // Recurse? No, structure is Root/Label/Note.md
                        // BUT system folders are Root/.Deleted/Label/Note.md
                        // This scanFolder helper is intended for the Label Folder itself.
                    }
                }
            }
            
            // 1. Scan Visible Folders (Labels)
            val visibleLabelFolders = root.listFiles().filter { it.isDirectory && !it.name!!.startsWith(".") }
            _labels.value = visibleLabelFolders.mapNotNull { it.name }
            for (labelFolder in visibleLabelFolders) {
                scanFolder(labelFolder, isArchived = false, isTrashed = false)
            }
            
            // 2. Scan .Archive
            val archiveRoot = root.findFile(".Archive")
            if (archiveRoot != null) {
                val archivedLabelFolders = archiveRoot.listFiles().filter { it.isDirectory }
                for (labelFolder in archivedLabelFolders) {
                    scanFolder(labelFolder, isArchived = true, isTrashed = false)
                }
            }

            // 3. Scan .Deleted
            val deletedRoot = root.findFile(".Deleted")
            if (deletedRoot != null) {
                val deletedLabelFolders = deletedRoot.listFiles().filter { it.isDirectory }
                for (labelFolder in deletedLabelFolders) {
                    scanFolder(labelFolder, isArchived = false, isTrashed = true)
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
        // Optimistic Update Data Setup
        val currentNotes = _notes.value.toMutableList()
        val existingNote = _notes.value.find { it.file.name == note.file.name || it.file.name == note.title + ".md" }
        
        // Detect Content Change
        val isMetadataOnly = existingNote != null && 
                             existingNote.content == note.content && 
                             existingNote.title == note.title &&
                             existingNote.folder == note.folder
                             
        if (isMetadataOnly) {
             // In-Place Update (Preserve Order & Timestamp)
             val index = currentNotes.indexOfFirst { it.file.name == existingNote!!.file.name }
             if (index != -1) {
                 val newFileName = "${note.title}.md"
                 currentNotes[index] = note.copy(
                     file = java.io.File(note.file.parent, newFileName),
                     lastModified = existingNote!!.lastModified
                 )
                 _notes.value = currentNotes
                 
                 rootDir?.let { root ->
                     appConfig.fileColors[newFileName] = note.color
                     metadataManager.saveConfig(root, appConfig)
                 }
                  saveCacheToDisk(_notes.value)
             }
             return@withContext "${note.title}.md"
        }
                             
        // Prepare new list state (Content Change)
        currentNotes.removeAll { it.file.name == note.file.name || it.file.name == note.title + ".md" }
        if (oldFile != null) currentNotes.removeAll { it.file.name == oldFile.name }
        
        val originalName = oldFile?.name ?: note.file.name
        if (originalName.isNotEmpty()) currentNotes.removeAll { it.file.name == originalName }
        
        val newFileName = "${note.title}.md"
        currentNotes.removeAll { it.file.name == newFileName }

        // Increase Timestamp
        val newLastModified = Date()
        
        // Add new (Optimistic) - Move to Top
        currentNotes.add(0, note.copy(
            file = java.io.File(note.file.parent, newFileName),
            lastModified = newLastModified
        )) 
        
        _notes.value = currentNotes
        saveCacheToDisk(_notes.value)
        

        rootDir?.let { root ->
            // Update metadata cache & persist
            // appConfig.fileColors[note.file.name] = note.color // Note: file.name might change if renamed?
            appConfig.fileColors[note.title + ".md"] = note.color // Use filename as key
            
            // Persist Pinned Status
            val fileNameKey = note.title + ".md"
            if (note.isPinned) {
                appConfig.pinnedFiles.add(fileNameKey)
            } else {
                appConfig.pinnedFiles.remove(fileNameKey)
            }
            
            // If Content Changed, REMOVE custom timestamp (let it update naturally to 'now')
            // If Content Changed, REMOVE custom timestamp (let it update naturally to 'now')
            appConfig.customTimestamps.remove(note.title + ".md")
            metadataManager.saveConfig(root, appConfig)
            
            // Logic to find target folder and write
            var targetFolderName = note.file.parent // "Inbox", "Work"
            if (targetFolderName.isNullOrEmpty()) targetFolderName = "Inbox"
            
            // Pinned Notes Logic: Enforce "Inbox/Pinned" for pinned notes
            // If pinned, folder is effectively "Inbox/Pinned" (or "Pinned" inside Inbox)
            // But internal folder struct is Root/Label.
            // If we want "Inbox/Pinned", we need a "Pinned" folder inside "Inbox".
            // DocumentFile structure is tree-based.
            
            var targetFolderDoc: DocumentFile? = null
            
            if (note.isPinned) {
                 // Ensure Inbox exists
                 val inbox = root.findFile("Inbox") ?: root.createDirectory("Inbox")
                 // Ensure Pinned exists inside Inbox
                 val pinnedDir = inbox?.findFile("Pinned") ?: inbox?.createDirectory("Pinned")
                 targetFolderDoc = pinnedDir
            } else {
                 // Normal Logic
                 // If the note WAS in Inbox/Pinned, we need to move it back to Inbox (or original folder).
                 // note.file.parent might still say "Pinned" if we just unpinned it in memory?
                 // No, viewModel updates the list.
                 // We will trust 'targetFolderName'. Only override if Pinned.
                 // If unpinned, we put it in 'targetFolderName' (e.g. Inbox).
                 
                 targetFolderDoc = root.findFile(targetFolderName)
                 if (targetFolderDoc == null) targetFolderDoc = root.createDirectory(targetFolderName)
            }
            
            val fileName = "${note.title}.md"
            
            // Fix Duplicate on Rename OR Move:
            // Check if oldFile is different from new file (Name OR Folder)
            if (oldFile != null) {
                 val oldName = oldFile.name
                 // "Inbox" handling: if parent is null/empty, it's Inbox.
                 val oldParent = if (oldFile.parent.isNullOrEmpty()) "Inbox" else oldFile.parent
                 
                 val isDifferent = (oldName != fileName) || (oldParent != targetFolderName)
                 
                 if (isDifferent && oldName.endsWith(".md")) {
                      // Delete old file
                      val oldParentFolder = oldFile.parent ?: "Inbox"
                      val oldFileDoc = if (oldParentFolder == targetFolderName) {
                           targetFolderDoc?.findFile(oldName)
                      } else {
                           // Search via root
                           root.findFile(oldParentFolder)?.findFile(oldName)
                      }
                      
                      try {
                          oldFileDoc?.delete()
                      } catch (e: Exception) { e.printStackTrace() }
                 }
            } else {
                 // Fallback to name-only check if oldFile not passed (legacy/safety)
                 val originalNameFallback = note.file.name
                 if (originalNameFallback.isNotEmpty() && originalNameFallback != fileName && originalNameFallback.endsWith(".md")) {
                     targetFolderDoc?.findFile(originalNameFallback)?.delete()
                 }
            }

            var fileDoc = targetFolderDoc?.findFile(fileName)
            if (fileDoc == null) fileDoc = targetFolderDoc?.createFile("text/markdown", fileName)
            
            fileDoc?.let { ioFile ->
                context.contentResolver.openOutputStream(ioFile.uri, "wt")?.use { outputStream ->
                     OutputStreamWriter(outputStream).use { writer ->
                         writer.write(note.content)
                     }
                }
            }
            refreshNotes()
        }
        return@withContext "${note.title}.md"
    }

    override suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        // Optimistic Update
        val currentNotes = _notes.value.toMutableList()
        val index = currentNotes.indexOfFirst { it.file.name == id }
        if (index != -1) {
             val note = currentNotes[index]
             currentNotes[index] = note.copy(isTrashed = true)
             _notes.value = currentNotes
             saveCacheToDisk(_notes.value)
        }

        moveNoteToSystemFolder(id, ".Deleted")
        refreshNotes()
    }

    override suspend fun archiveNote(id: String) = withContext(Dispatchers.IO) {
        // Optimistic Update
        val currentNotes = _notes.value.toMutableList()
        val index = currentNotes.indexOfFirst { it.file.name == id }
        if (index != -1) {
             val note = currentNotes[index]
             currentNotes[index] = note.copy(isArchived = true)
             _notes.value = currentNotes
             saveCacheToDisk(_notes.value)
        }
        
        moveNoteToSystemFolder(id, ".Archive")
        refreshNotes()
    }
    
    override suspend fun restoreNote(id: String) {
        withContext(Dispatchers.IO) {
            
            // Optimistic Update
            val currentNotes = _notes.value.toMutableList()
            val index = currentNotes.indexOfFirst { it.file.name == id }
            if (index != -1) {
                 val note = currentNotes[index]
                 currentNotes[index] = note.copy(isTrashed = false, isArchived = false) // Don't update lastModified
                 _notes.value = currentNotes
                 saveCacheToDisk(_notes.value)
            }
            
            // Move back to active folder (Label)
            rootDir?.let { root ->
                 // ... existing logic ...
                 val note = _notes.value.find { it.file.name == id || it.file.path == id }
                 if (note != null) {
                     val label = note.folder
                     val fileName = note.file.name
                     
                     // Find source (could be in .Deleted or .Archive)
                     var sourceFile: DocumentFile? = null
                     
                     val deletedRoot = root.findFile(".Deleted")
                     sourceFile = deletedRoot?.findFile(label)?.findFile(fileName)
                     
                     if (sourceFile == null) {
                         val archiveRoot = root.findFile(".Archive")
                         sourceFile = archiveRoot?.findFile(label)?.findFile(fileName)
                     }
    
                     if (sourceFile != null) {
                         // Target: Root/Label
                         var targetLabelFolder = root.findFile(label) ?: root.createDirectory(label)
                         
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
                 refreshNotes()
            }
        }
    }

    override suspend fun deleteNotes(noteIds: List<String>) = withContext(Dispatchers.IO) {
        // Capture notes BEFORE removing them
        val notesToDelete = _notes.value.filter { noteIds.contains(it.file.path) || noteIds.contains(it.file.name) }
        
        // Optimistic Update
        val currentNotes = _notes.value.toMutableList()
        currentNotes.removeAll { noteIds.contains(it.file.path) || noteIds.contains(it.file.name) }
        _notes.value = currentNotes
        saveCacheToDisk(_notes.value)

        // File Operations
        notesToDelete.forEach { note ->
            moveNoteToSystemFolder(note, ".Deleted")
        }
        refreshNotes()
    }

    override suspend fun archiveNotes(noteIds: List<String>) = withContext(Dispatchers.IO) {
        // Optimistic Update
        val currentNotes = _notes.value.toMutableList()
        currentNotes.replaceAll { note ->
            if (noteIds.contains(note.file.path) || noteIds.contains(note.file.name)) {
                note.copy(isArchived = true) // Don't update lastModified
            } else {
                note
            }
        }
        _notes.value = currentNotes
        saveCacheToDisk(_notes.value)
        
        noteIds.forEach { id ->
            moveNoteToSystemFolder(id, ".Archive")
        }
        refreshNotes()
    }

    override suspend fun moveNotes(notes: List<Note>, targetFolder: String) = withContext(Dispatchers.IO) {
        // Optimistic Update
        val currentNotes = _notes.value.toMutableList()
        // We need to update the folder of the moved notes in the list
        val noteIds = notes.map { it.file.path }.toSet()
        
        currentNotes.replaceAll { note ->
            if (noteIds.contains(note.file.path) || notes.any { it.file.name == note.file.name }) {
                // Update file path to reflect new folder but KEEP lastModified
                note.copy(file = java.io.File(targetFolder, note.file.name))
            } else {
                note
            }
        }
        _notes.value = currentNotes
        saveCacheToDisk(_notes.value)

        // IO Operations
        rootDir?.let { root ->
             // Cache target dirs to avoid repeated lookup
             // Map: "Root" -> DocFile, ".Archive" -> DocFile
             val rootTarget = root.findFile(targetFolder) ?: root.createDirectory(targetFolder)
             val archiveRoot = root.findFile(".Archive") ?: root.createDirectory(".Archive")
             val archiveTarget = archiveRoot?.findFile(targetFolder) ?: archiveRoot?.createDirectory(targetFolder)
             
             notes.forEach { note ->
                 val oldFile = note.file
                 val fileName = oldFile.name
                 
                 // Determine correct target based on current state
                 val finalTargetDir = if (note.isArchived) archiveTarget else rootTarget
                 
                 if (note.folder != targetFolder && finalTargetDir != null) {
                     // Verify if source file exists
                     // Search order: Label, Archive, Deleted
                     // NOTE: We use the PREVIOUS folder from the passed 'note' object (before copy)
                     val currentFolder = note.folder
                     
                     // Try to find the file doc
                     val sourceFile = root.findFile(currentFolder)?.findFile(fileName) 
                                      ?: root.findFile(".Archive")?.findFile(currentFolder)?.findFile(fileName)
                     
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
        // Optimistic Update
        val currentNotes = _notes.value.toMutableList()
        val index = currentNotes.indexOfFirst { it.file.path == id || it.file.name == id }
        if (index != -1) {
             val note = currentNotes[index]
             // Only update color, preserve everything else including timestamp
             currentNotes[index] = note.copy(color = color)
             _notes.value = currentNotes
             saveCacheToDisk(_notes.value)
             
             // Persist Metadata
             rootDir?.let { root ->
                 val fileName = note.file.name // Use filename for config key
                 appConfig.fileColors[fileName] = color
                 metadataManager.saveConfig(root, appConfig)
             }
        }
    }
    
    override suspend fun togglePinStatus(noteIds: List<String>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        // Optimistic Update
        val currentNotes = _notes.value.toMutableList()
        currentNotes.replaceAll { note ->
            if (noteIds.contains(note.file.path) || noteIds.contains(note.file.name)) {
                note.copy(isPinned = isPinned)
            } else {
                note
            }
        }
        _notes.value = currentNotes
        saveCacheToDisk(_notes.value)
        
        // Persist Metadata & Move Files
        rootDir?.let { root ->
            // Ensure Folders exist
            val inbox = root.findFile("Inbox") ?: root.createDirectory("Inbox")
            val pinnedDir = inbox?.findFile("Pinned") ?: inbox?.createDirectory("Pinned")
            
            noteIds.forEach { id ->
                val note = _notes.value.find { it.file.path == id || it.file.name == id } ?: return@forEach
                val fileName = note.file.name
                
                // Update Metadata using FILENAME
                if (isPinned) appConfig.pinnedFiles.add(fileName) else appConfig.pinnedFiles.remove(fileName)
                
                // Find Source File (could be anywhere)
                val sourceFolderStr = note.file.parent ?: "Inbox"
                
                // ... (rest of move logic can remain similar, but ensure we use 'fileName' and 'note' correctly)
                // Simplify finding source using the note object's current knowledge
                
                var sourceFileDoc: DocumentFile? = null
                
                // Try to find file based on known parent
                // Note: note.file.parent "Pinned" usually means Inbox/Pinned in file structure logic?
                // Or if we are in File repo, 'folder' might be just the name. 
                // Let's rely on recursive search if simple lookup fails? No, too slow.
                // Check if currently physically in pinned folder
                sourceFileDoc = pinnedDir?.findFile(fileName)
                
                if (sourceFileDoc == null) {
                    // Check active folder
                     sourceFileDoc = root.findFile(sourceFolderStr)?.findFile(fileName)
                }
                
                // Check Archive
                if (sourceFileDoc == null) {
                    sourceFileDoc = root.findFile(".Archive")?.findFile(sourceFolderStr)?.findFile(fileName)
                }

                if (sourceFileDoc != null) {
                    if (isPinned) {
                        // Move TO Inbox/Pinned
                        if (pinnedDir != null && sourceFileDoc.parentFile?.name != "Pinned") {
                             try {
                                val content = readText(sourceFileDoc)
                                val newFile = pinnedDir.createFile("text/markdown", fileName)
                                newFile?.let { nf ->
                                     context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                                         OutputStreamWriter(os).use { it.write(content) }
                                     }
                                     sourceFileDoc.delete()
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    } else {
                        // Move FROM Pinned TO Inbox (default unpin location)
                        // Only move if it IS in Pinned folder
                        if (sourceFileDoc.parentFile?.name == "Pinned" && inbox != null) {
                            try {
                                val content = readText(sourceFileDoc)
                                val newFile = inbox.createFile("text/markdown", fileName)
                                newFile?.let { nf ->
                                     context.contentResolver.openOutputStream(nf.uri)?.use { os ->
                                         OutputStreamWriter(os).use { it.write(content) }
                                     }
                                     sourceFileDoc.delete()
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }
            }
            metadataManager.saveConfig(root, appConfig)
        }
        Unit
    }
    private suspend fun moveNoteToSystemFolder(id: String, systemFolderName: String) {
        val note = _notes.value.find { it.file.name == id || it.file.path == id }
        if (note != null) {
            moveNoteToSystemFolder(note, systemFolderName)
        }
    }

    // Helper for Object-based move (Delete usage - since object is removed from list)
    private suspend fun moveNoteToSystemFolder(note: Note, systemFolderName: String) {
         rootDir?.let { root ->
            // Preserve Timestamp
            appConfig.customTimestamps[note.file.name] = note.lastModified.time
            metadataManager.saveConfig(root, appConfig)

            val folderName = note.file.parent ?: "Inbox"
            val fileName = note.file.name
            
            var sourceFile: DocumentFile? = null
            
            // Check Active folders
            val activeFolder = root.findFile(folderName)
            sourceFile = activeFolder?.findFile(fileName)
            
            // If not found, check .Archive
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
        val pathKey = file.uri.toString() // Use URI as key since distinct files have distinct URIs
        
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

    
    // Disk Persistence for Cold Start
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
