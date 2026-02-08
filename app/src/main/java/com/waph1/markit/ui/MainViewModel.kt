package com.waph1.markit.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waph1.markit.data.model.Note
import com.waph1.markit.data.repository.RoomNoteRepository
import com.waph1.markit.data.repository.MetadataManager
import com.waph1.markit.data.repository.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MainViewModel(
    private val repository: RoomNoteRepository,
    private val metadataManager: MetadataManager,
    private val prefsManager: PrefsManager
) : ViewModel() {

    sealed interface NoteFilter {
        object All : NoteFilter
        data class Label(val name: String) : NoteFilter
        object Archive : NoteFilter
        object Trash : NoteFilter
    }

    private val _currentFilter = MutableStateFlow<NoteFilter>(NoteFilter.All)
    val currentFilter: StateFlow<NoteFilter> = _currentFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(prefsManager.getSortOrder())
    val sortOrder: StateFlow<PrefsManager.SortOrder> = _sortOrder.asStateFlow()

    private val _sortDirection = MutableStateFlow(prefsManager.getSortDirection())
    val sortDirection: StateFlow<PrefsManager.SortDirection> = _sortDirection.asStateFlow()

    private val _viewMode = MutableStateFlow(prefsManager.getViewMode())
    val viewMode: StateFlow<PrefsManager.ViewMode> = _viewMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchEverywhere = MutableStateFlow(false)
    val isSearchEverywhere: StateFlow<Boolean> = _isSearchEverywhere.asStateFlow()

    // Notes Flow: Reacts to filter changes and queries appropriate DAO method
    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = combine(
        _currentFilter.flatMapLatest { filter ->
            when (filter) {
                is NoteFilter.All -> repository.getAllNotes()
                is NoteFilter.Label -> repository.getNotesByFolder(filter.name)
                is NoteFilter.Archive -> repository.getArchivedNotes()
                is NoteFilter.Trash -> repository.getTrashedNotes()
            }
        },
        repository.getAllNotesWithArchive(),
        _sortOrder,
        _sortDirection,
        _searchQuery
            .debounce(300L)
            .distinctUntilChanged()
    ) { notesList, allNotesList, order, direction, query ->
        // Search Filter
        val currentFilterValue = _currentFilter.value
        val searched = if (query.isBlank()) {
            _isSearchEverywhere.value = false
            notesList
        } else {
            val q = query.lowercase()
            val filteredResults = notesList.filter { 
                it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
            }
            
            if (filteredResults.isEmpty() && currentFilterValue !is NoteFilter.Trash) {
                // Search everywhere (excluding Trash, but including Archive and all active notes)
                val globalResults = allNotesList.filter {
                    it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
                }
                _isSearchEverywhere.value = globalResults.isNotEmpty()
                globalResults
            } else {
                _isSearchEverywhere.value = false
                filteredResults
            }
        }
        
        val sorted = when (order) {
            PrefsManager.SortOrder.DATE_CREATED,
            PrefsManager.SortOrder.DATE_MODIFIED -> searched.sortedBy { it.lastModified }
            PrefsManager.SortOrder.TITLE -> searched.sortedBy { it.title.lowercase() }
        }

        val directed = if (direction == PrefsManager.SortDirection.DESCENDING) {
            sorted.reversed()
        } else {
            sorted
        }
        
        // Pinned notes always on top
        val result = directed.sortedByDescending { it.isPinned }
        _isLoading.value = false
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Temporary labels for immediate UI feedback (since Room doesn't show empty folders)
    private val _tempLabels = MutableStateFlow<Set<String>>(emptySet())

    // Labels from Room DAO combined with temporarily created ones
    val labels: StateFlow<List<String>> = combine(
        repository.getLabels(),
        _tempLabels
    ) { dbLabels, tempLabels ->
        (dbLabels + tempLabels).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // UI State
    // Default to TRUE, but check immediately on init
    private val _isPermissionNeeded = MutableStateFlow(prefsManager.getRootUri() == null)
    val isPermissionNeeded: StateFlow<Boolean> = _isPermissionNeeded.asStateFlow()

    init {
        // Auto-load managed by MainActivity to ensure permissions
        // val savedUriStr = prefsManager.getRootUri()
        // if (savedUriStr != null) {
        //     setRootFolder(Uri.parse(savedUriStr))
        // }
    }

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _isEditorOpen = MutableStateFlow(false)
    val isEditorOpen: StateFlow<Boolean> = _isEditorOpen.asStateFlow()

    // isLoading: Room reads are synchronous from cache, so always false after initial sync
    // Start true to prevent "No Notes" flash
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setRootFolder(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.setRootFolder(uri.toString())
            _isPermissionNeeded.value = false
            // Loading will be cleared by flow emission
        }
    }

    fun resetPermissionNeeded() {
        _isPermissionNeeded.value = true
    }

    fun openNote(note: Note) {
        _currentNote.value = note
        _isEditorOpen.value = true
    }

    fun createNote() {
        _currentNote.value = null // New note
        _isEditorOpen.value = true
    }

    fun closeEditor() {
        _isEditorOpen.value = false
        _currentNote.value = null
    }
    
    fun setFilter(filter: NoteFilter) {
        _currentFilter.value = filter
    }
    
    fun createLabel(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
             val success = repository.createLabel(name)
             if (success) {
                 val current = _tempLabels.value.toMutableSet()
                 current.add(name)
                 _tempLabels.value = current
             }
        }
    }

    fun deleteLabel(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteLabel(name)
            if (success) {
                val current = _tempLabels.value.toMutableSet()
                if (current.remove(name)) {
                    _tempLabels.value = current
                }
                onSuccess()
            } else {
                onError("Label must be empty to delete it")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note.file.name) 
        }
    }
    
    fun archiveNote(note: Note) {
         viewModelScope.launch {
            repository.archiveNote(note.file.name)
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.restoreNote(note.file.name)
        }
    }

    fun saveNote(note: Note, oldFile: java.io.File? = null) {
        viewModelScope.launch {
            val savedPath = repository.saveNote(note, oldFile)
            if (savedPath.isNotEmpty()) {
                val updatedFile = java.io.File(savedPath)
                // Extract new title from filename (in case it was renamed due to conflict)
                val newTitle = updatedFile.nameWithoutExtension
                _currentNote.value = note.copy(file = updatedFile, title = newTitle)
            }
        }
    }

    fun setSortOrder(order: PrefsManager.SortOrder) {
        _sortOrder.value = order
        prefsManager.saveSortOrder(order)
    }

    fun setSortDirection(direction: PrefsManager.SortDirection) {
        _sortDirection.value = direction
        prefsManager.saveSortDirection(direction)
    }

    fun setViewMode(mode: PrefsManager.ViewMode) {
        _viewMode.value = mode
        prefsManager.saveViewMode(mode)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    // Multi-Selection State
    private val _selectedNotes = MutableStateFlow<Set<String>>(emptySet())
    val selectedNotes: StateFlow<Set<String>> = _selectedNotes.asStateFlow()

    fun toggleSelection(note: Note) {
        val current = _selectedNotes.value.toMutableSet()
        if (current.contains(note.file.path)) {
            current.remove(note.file.path)
        } else {
            current.add(note.file.path)
        }
        _selectedNotes.value = current
    }

    fun clearSelection() {
        _selectedNotes.value = emptySet()
    }

    fun deleteSelectedNotes() {
        val selected = _selectedNotes.value.toList()
        clearSelection() // Clear immediately for responsiveness
        viewModelScope.launch {
            repository.deleteNotes(selected)
        }
    }

    fun archiveSelectedNotes() {
        val selected = _selectedNotes.value.toList()
        clearSelection() // Clear immediately
        viewModelScope.launch {
            repository.archiveNotes(selected)
        }
    }

    fun restoreSelectedNotes() {
        val selected = _selectedNotes.value.toList()
        clearSelection()
        viewModelScope.launch {
            // Restore doesn't have a bulk op yet in generic interface?
            // FileNoteRepository has restoreNote(id).
            // Let's implement bulk restore or loop.
            // Loop for now is fine as restore is rarer, OR valid bulk op.
            // Check Repository.
            // Repository interface: restoreNote(id). No restoreNotes.
            // Loop:
            selected.forEach { repository.restoreNote(it) }
        }
    }

    // Move Selected
    fun moveSelectedNotes(targetLabel: String) {
        val selectedIds = _selectedNotes.value.toSet() 
        val currentNotesList = notes.value 
        // Filter using file.path (ID)
        val notesToMove = currentNotesList.filter { selectedIds.contains(it.file.path) }
        
        clearSelection()
        
        viewModelScope.launch {
            val targetFolder = if (targetLabel.isEmpty()) "Inbox" else targetLabel
            
            // Track as temp label if not Inbox
            if (targetFolder != "Inbox") {
                 val current = _tempLabels.value.toMutableSet()
                 current.add(targetFolder)
                 _tempLabels.value = current
            }
            
            repository.moveNotes(notesToMove, targetFolder)
        }
    }
    
    fun updateSelectedNotesColor(color: Long) {
        val selectedIds = _selectedNotes.value.toList()
        val currentNotesList = notes.value
        val notesToUpdate = currentNotesList.filter { selectedIds.contains(it.file.path) }
        
        clearSelection()
        
        viewModelScope.launch {
            notesToUpdate.forEach { note ->
                repository.setNoteColor(note.file.path, color) // Pass ID (path)
            }
        }
    }

    fun togglePinSelectedNotes() {
        val selectedIds = _selectedNotes.value.toList()
        val currentNotesList = notes.value
        val notesToUpdate = currentNotesList.filter { selectedIds.contains(it.file.path) }
        
        // Logical check: If ANY selected are unpinned, PIN ALL. Else UNPIN ALL.
        val shouldPin = notesToUpdate.any { !it.isPinned }
        
        clearSelection()
        
        viewModelScope.launch {
             // Pass IDs. Repository expects "paths" or "names"? 
             // FileNoteRepository checks contains(file.path) || contains(file.name).
             // safest is passing paths.
             repository.togglePinStatus(selectedIds, shouldPin)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }
}
