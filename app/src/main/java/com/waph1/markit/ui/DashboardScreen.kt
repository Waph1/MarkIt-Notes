package com.waph1.markit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning // Placeholder
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.waph1.markit.data.model.Note
import com.waph1.markit.data.repository.PrefsManager
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    listState: LazyStaggeredGridState,
    onSelectFolder: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onFabClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val isPermissionNeeded by viewModel.isPermissionNeeded.collectAsState()
    val isSearchEverywhere by viewModel.isSearchEverywhere.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    // ... (UI State remains the same)
    var showCreateLabelDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var labelToDelete by remember { mutableStateOf<String?>(null) }

    if (showCreateLabelDialog) {
        CreateLabelDialog(
            onDismiss = { showCreateLabelDialog = false },
            onConfirm = { name ->
                viewModel.createLabel(name)
                showCreateLabelDialog = false
            }
        )
    }
    
    if (showEmptyTrashDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.empty_trash_title)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.empty_trash_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.empty_trash_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.cancel))
                }
            }
        )
    }

    if (labelToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { labelToDelete = null },
            title = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete_label_title)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete_label_message, labelToDelete!!)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val name = labelToDelete!!
                        viewModel.deleteLabel(
                            name = name,
                            onSuccess = { 
                                Toast.makeText(context, context.getString(com.waph1.markit.R.string.label_deleted_toast), Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                // Error is currently a raw string from ViewModel, but should be localized
                                val localizedError = if (error == "Label must be empty to delete it") {
                                    context.getString(com.waph1.markit.R.string.error_delete_label_not_empty)
                                } else error
                                Toast.makeText(context, localizedError, Toast.LENGTH_SHORT).show()
                            }
                        )
                        labelToDelete = null
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete_label_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { labelToDelete = null }) {
                    Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.cancel))
                }
            }
        )
    }
    
    fun onRequestCreateLabel() {
        showCreateLabelDialog = true
    }
    
    // Selection State
    val selectedNotes by viewModel.selectedNotes.collectAsState()
    val isInSelectionMode = selectedNotes.isNotEmpty()
    
    val selectedNotesList = notes.filter { selectedNotes.contains(it.file.path) }
    val allSelectedArchived = selectedNotesList.isNotEmpty() && selectedNotesList.all { it.isArchived }
    val allSelectedActive = selectedNotesList.isNotEmpty() && selectedNotesList.all { !it.isArchived && !it.isTrashed }
    
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Double back to exit state
    var lastBackPressTime by remember { mutableStateOf(0L) }
    
    BackHandler {
        when {
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }
            isInSelectionMode -> {
                viewModel.clearSelection()
            }
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    (context as? ComponentActivity)?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, context.getString(com.waph1.markit.R.string.press_back_again_exit), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    var showSortMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet {
                Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Text(
                        androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.app_name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    androidx.compose.material3.HorizontalDivider()
                    
                    // Notes (All)
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.all_notes)) },
                        selected = currentFilter is MainViewModel.NoteFilter.All,
                        onClick = {
                            viewModel.setFilter(MainViewModel.NoteFilter.All)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Labels
                    val visibleLabels = labels.filter { it != "Inbox" }
                    if (visibleLabels.isNotEmpty()) {
                        Text(
                            androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.labels),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                        visibleLabels.forEach { label ->
                            val haptic = LocalHapticFeedback.current
                            val isSelected = (currentFilter as? MainViewModel.NoteFilter.Label)?.name == label
                            
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer 
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.setFilter(MainViewModel.NoteFilter.Label(label))
                                            scope.launch { drawerState.close() }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            labelToDelete = label
                                        }
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // New Label Button
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.create_new_label)) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        selected = false,
                        onClick = {
                            onRequestCreateLabel()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Archive
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.archive)) },
                        selected = currentFilter is MainViewModel.NoteFilter.Archive,
                        onClick = {
                            viewModel.setFilter(MainViewModel.NoteFilter.Archive)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Trash
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.trash)) },
                        selected = currentFilter is MainViewModel.NoteFilter.Trash,
                        onClick = {
                            viewModel.setFilter(MainViewModel.NoteFilter.Trash)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        val title = when (currentFilter) {
            is MainViewModel.NoteFilter.All -> androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.app_name)
            is MainViewModel.NoteFilter.Trash -> androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.trash)
            is MainViewModel.NoteFilter.Archive -> androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.archive)
            is MainViewModel.NoteFilter.Label -> (currentFilter as MainViewModel.NoteFilter.Label).name
        }

        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = isInSelectionMode,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    SelectionTopAppBar(
                        selectionCount = selectedNotes.size,
                        currentFilter = currentFilter,
                        allSelectedArchived = allSelectedArchived,
                        allSelectedActive = allSelectedActive,
                        onClearSelection = { viewModel.clearSelection() },
                        onDelete = { viewModel.deleteSelectedNotes() },
                        onArchive = { viewModel.archiveSelectedNotes() },
                        onRestore = { viewModel.restoreSelectedNotes() },
                        onMove = { targetLabel -> viewModel.moveSelectedNotes(targetLabel) },
                        onColorChange = { color -> viewModel.updateSelectedNotesColor(color) },
                        onPin = { viewModel.togglePinSelectedNotes() },
                        availableLabels = labels
                    )
                }
                AnimatedVisibility(
                    visible = !isInSelectionMode,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    androidx.compose.material3.TopAppBar(
                        title = {
                            androidx.compose.material3.Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                SearchBar(viewModel = viewModel)
                            }
                        },
                        navigationIcon = {
                             androidx.compose.material3.IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                 Icon(Icons.Outlined.Menu, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.menu))
                             }
                        },
                        actions = {
                            if (currentFilter is MainViewModel.NoteFilter.Trash) {
                                androidx.compose.material3.IconButton(onClick = { showEmptyTrashDialog = true }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.empty_trash_desc))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            floatingActionButton = {
                if (!isPermissionNeeded && currentFilter !is MainViewModel.NoteFilter.Trash && currentFilter !is MainViewModel.NoteFilter.Archive) {
                    FloatingActionButton(onClick = onFabClick) {
                        Icon(Icons.Default.Add, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.add_note))
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading && notes.isNotEmpty()) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp) // Add horizontal padding for grid
                ) {
                if (isPermissionNeeded) {
                    PermissionRequestState(
                        onSelectFolder = onSelectFolder,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    NoteGrid(
                        notes = notes,
                        selectedNotes = selectedNotes,
                        isInSelectionMode = isInSelectionMode,
                        isLoading = isLoading,
                        isSearchEverywhere = isSearchEverywhere,
                        searchQuery = searchQuery,
                        viewMode = viewMode,
                        currentFilter = currentFilter,
                        listState = listState,
                        onNoteClick = { note ->
                            if (isInSelectionMode) {
                                viewModel.toggleSelection(note)
                            } else {
                                onNoteClick(note)
                            }
                        },
                        onNoteLongClick = { note ->
                            viewModel.toggleSelection(note)
                        }
                    )
                }
            }
        }
    }
}
}


@Composable
fun PermissionRequestState(
    onSelectFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.welcome_message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onSelectFolder) {
            Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.select_folder))
        }
    }
}

private sealed interface DashboardUiItem {
    val key: String
    data class NoteItem(val note: Note) : DashboardUiItem {
        override val key: String = note.file.path
    }
    data class HeaderItem(val title: String) : DashboardUiItem {
        override val key: String = "header_$title"
    }
    object SpacerItem : DashboardUiItem {
        override val key: String = "spacer_bottom"
    }
}

@Composable
fun NoteGrid(
    notes: List<Note>,
    selectedNotes: Set<String>,
    isInSelectionMode: Boolean,
    isLoading: Boolean,
    isSearchEverywhere: Boolean,
    searchQuery: String,
    viewMode: PrefsManager.ViewMode,
    currentFilter: MainViewModel.NoteFilter,
    listState: LazyStaggeredGridState,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit
) {
    if (isLoading && notes.isEmpty()) {
        // ... (loading state remains the same)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator()
                Text(
                    androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.loading_notes),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (notes.isEmpty()) {
        // ... (empty state remains the same)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.no_results_found),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val showSeparator = currentFilter is MainViewModel.NoteFilter.Label
        val isTrash = currentFilter is MainViewModel.NoteFilter.Trash
        val isArchive = currentFilter is MainViewModel.NoteFilter.Archive
        
        // Define string values for remember keys
        val searchEverywhereLabel = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.search_everywhere)
        val searchResultsLabel = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.search_results)
        val pinnedLabel = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.pinned)
        val othersLabel = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.others)
        val archivedNotesLabel = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.archived_notes_header)

        val uiItems = remember(notes, currentFilter, isSearchEverywhere, searchQuery) {
            val list = mutableListOf<DashboardUiItem>()
            
            if (isSearchEverywhere) {
                list.add(DashboardUiItem.HeaderItem(searchEverywhereLabel))
                notes.forEach { list.add(DashboardUiItem.NoteItem(it)) }
            } else if (searchQuery.isNotBlank()) {
                list.add(DashboardUiItem.HeaderItem(searchResultsLabel))
                notes.forEach { list.add(DashboardUiItem.NoteItem(it)) }
            } else if (isTrash || isArchive) {
                notes.forEach { list.add(DashboardUiItem.NoteItem(it)) }
            } else {
                 val pinned = notes.filter { it.isPinned && !it.isArchived }
                 val others = notes.filter { !it.isPinned && !it.isArchived }
                 val archived = notes.filter { it.isArchived }

                 if (pinned.isNotEmpty()) {
                     list.add(DashboardUiItem.HeaderItem(pinnedLabel))
                     pinned.forEach { list.add(DashboardUiItem.NoteItem(it)) }
                 }

                 if (pinned.isNotEmpty() && others.isNotEmpty()) {
                     list.add(DashboardUiItem.HeaderItem(othersLabel))
                 }
                 others.forEach { list.add(DashboardUiItem.NoteItem(it)) }

                 if (archived.isNotEmpty() && showSeparator) {
                    list.add(DashboardUiItem.HeaderItem(archivedNotesLabel))
                    archived.forEach { list.add(DashboardUiItem.NoteItem(it)) }
                 }
            }
            // Add Spacer at the end
            list.add(DashboardUiItem.SpacerItem)
            list
        }

        val columns = if (viewMode == PrefsManager.ViewMode.GRID) 2 else 1

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = listState,
            contentPadding = PaddingValues(0.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = uiItems,
                key = { it.key },
                span = { item ->
                    if (item is DashboardUiItem.HeaderItem || item is DashboardUiItem.SpacerItem) StaggeredGridItemSpan.FullLine
                    else StaggeredGridItemSpan.SingleLane
                }
            ) { item ->
                when (item) {
                    is DashboardUiItem.HeaderItem -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp, top = 8.dp)) {
                             Text(
                                 text = item.title,
                                 style = MaterialTheme.typography.labelSmall
                             )
                             if (item.title == "Archived notes") {
                                 androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                             }
                        }
                    }
                    is DashboardUiItem.NoteItem -> {
                        NoteCard(
                            note = item.note, 
                            isSelected = selectedNotes.contains(item.note.file.path),
                            onClick = { onNoteClick(item.note) },
                            onLongClick = { onNoteLongClick(item.note) }
                        )
                    }
                    is DashboardUiItem.SpacerItem -> {
                         androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(80.dp).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val noteColor = Color(note.color.toInt())
    
    // In dark mode, we use a much darker version of the pastel color to maintain contrast
    // or we use the pastel color with low alpha on top of the surface
    val containerColor = if (isDark) {
        if (note.color == 0xFFFFFFFF) MaterialTheme.colorScheme.surfaceVariant
        else noteColor.copy(alpha = 0.3f).compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        noteColor
    }

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val onLongClickAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLongClick()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClickAction
            )
            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (note.folder != "Unknown" && note.folder != "Inbox") {
                 Text(
                    text = note.folder,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp).background(Color(0x22000000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            if (note.content.isNotEmpty()) {
                Box {
                    MarkdownText(
                        markdown = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 10,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    // Overlay to capture clicks
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null, // Ripple is handled by the shared interaction source on the Card
                                onClick = onClick,
                                onLongClick = onLongClickAction
                            )
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectionCount: Int,
    currentFilter: MainViewModel.NoteFilter,
    allSelectedArchived: Boolean,
    allSelectedActive: Boolean,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onMove: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onPin: () -> Unit,
    availableLabels: List<String>
) {
    var showMoveMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showColorMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showCreateLabelDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showCreateLabelDialog) {
        CreateLabelDialog(
            onDismiss = { showCreateLabelDialog = false },
            onConfirm = { name ->
                onMove(name) // Creates and moves
                showCreateLabelDialog = false
            }
        )
    }
    
    val isTrash = currentFilter is MainViewModel.NoteFilter.Trash
    // isArchive is no longer the sole determinant for Restore visibility
    
    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete_selected_notes_title)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete_selected_notes_message, selectionCount)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.cancel))
                }
            }
        )
    }

    TopAppBar(
        title = {
             Text(
                 text = "$selectionCount",
                 style = MaterialTheme.typography.titleLarge,
                 fontWeight = FontWeight.Bold
             )
        },
        navigationIcon = {
            androidx.compose.material3.IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.close_selection))
            }
        },
        actions = {
             // Restore / Unarchive
             // Show if in Trash OR (All Selected are Archived)
             if (isTrash || allSelectedArchived) {
                 androidx.compose.material3.IconButton(onClick = onRestore) {
                     Icon(Icons.Outlined.Refresh, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.restore)) // Use Refresh or Restore icon
                 }
             }
             
             // Move (Hidden in Trash)
             if (!isTrash) {
                 androidx.compose.material3.IconButton(onClick = { showMoveMenu = true }) {
                     Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.move)) 
                 }
                 DropdownMenu(
                     expanded = showMoveMenu,
                     onDismissRequest = { showMoveMenu = false }
                 ) {
                      DropdownMenuItem(
                         text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.inbox)) },
                         onClick = { 
                             onMove("Inbox")
                             showMoveMenu = false
                         }
                      )
                      availableLabels.forEach { label ->
                          DropdownMenuItem(
                              text = { Text(label) },
                              onClick = { 
                                  onMove(label) 
                                  showMoveMenu = false
                              }
                          )
                      }
                      androidx.compose.material3.HorizontalDivider()
                      DropdownMenuItem(
                          text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.create_new_label)) },
                          leadingIcon = { Icon(Icons.Default.Add, null) },
                          onClick = { 
                              showMoveMenu = false
                              showCreateLabelDialog = true
                          }
                      )
                 }
             }

             // Color Picker
             // Allowed for Active and Archived? User said "remove archive or restore option".
             // We'll allow Color for Archived notes as it's useful.
             if (!isTrash) {
                 Box {
                      androidx.compose.material3.IconButton(onClick = { showColorMenu = true }) {
                          Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray))
                      }
                      DropdownMenu(
                          expanded = showColorMenu,
                          onDismissRequest = { showColorMenu = false }
                      ) {
                          val colors = listOf(
                               0xFFFFFFFF, 0xFFF28B82, 0xFFFBBC04, 0xFFFFF475, 
                               0xFFCCFF90, 0xFFA7FFEB, 0xFFCBF0F8, 0xFFAECBFA, 
                               0xFFD7AEFB, 0xFFFDCFE8, 0xFFE6C9A8, 0xFFE8EAED
                          )
                          
                          Column(modifier = Modifier.padding(8.dp)) {
                              colors.chunked(4).forEach { rowColors ->
                                  androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                      rowColors.forEach { color ->
                                           Box(
                                               modifier = Modifier
                                                   .size(32.dp)
                                                   .clip(CircleShape)
                                                   .background(Color(color.toInt()))
                                                   .border(1.dp, Color.Gray, CircleShape)
                                                   .clickable { 
                                                       onColorChange(color)
                                                       showColorMenu = false
                                                   }
                                           )
                                      }
                                  }
                                  androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                              }
                          }
                      }
                 }
             }
             
             // Pin/Unpin (Hidden in Trash, and only for Active notes?)
             // Pinning archived notes usually implies unarchiving or just pinning in archive.
             // We'll restrict to Active notes to keep it simple and consistent.
             if (!isTrash && allSelectedActive) {
                 androidx.compose.material3.IconButton(onClick = onPin) {
                     Icon(Icons.Outlined.Star, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.pin_unpin))
                 }
             }

             // Archive (Hidden in Trash, and only for Active notes)
             if (!isTrash && allSelectedActive) {
                 androidx.compose.material3.IconButton(onClick = onArchive) {
                     Icon(Icons.Outlined.Archive, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.archive))
                 }
             }
             
             // Delete
             androidx.compose.material3.IconButton(onClick = { showDeleteDialog = true }) {
                 Icon(Icons.Outlined.Delete, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete))
             }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
fun CreateLabelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.create_new_label)) },
        text = { 
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.label_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.create))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.cancel))
            }
        }
    )
}
