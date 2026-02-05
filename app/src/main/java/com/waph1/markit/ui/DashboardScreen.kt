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

@OptIn(ExperimentalMaterial3Api::class)
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
    val currentFilter by viewModel.currentFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Selection State
    val selectedNotes by viewModel.selectedNotes.collectAsState()
    val isInSelectionMode = selectedNotes.isNotEmpty()
    
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    
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
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
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
                        "MarkIt",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    androidx.compose.material3.HorizontalDivider()
                    
                    // Notes (All)
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text("All notes") },
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
                            "Labels",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                        visibleLabels.forEach { label ->
                            androidx.compose.material3.NavigationDrawerItem(
                                label = { Text(label) },
                                selected = (currentFilter as? MainViewModel.NoteFilter.Label)?.name == label,
                                onClick = {
                                    viewModel.setFilter(MainViewModel.NoteFilter.Label(label))
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                    
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Archive
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text("Archive") },
                        selected = currentFilter is MainViewModel.NoteFilter.Archive,
                        onClick = {
                            viewModel.setFilter(MainViewModel.NoteFilter.Archive)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Trash
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text("Trash") },
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
            is MainViewModel.NoteFilter.All -> "MarkIt"
            is MainViewModel.NoteFilter.Trash -> "Trash"
            is MainViewModel.NoteFilter.Archive -> "Archive"
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
                                 Icon(Icons.Default.Menu, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.menu))
                             }
                        },
                        actions = {
                            // Empty actions for now, as Sort moved to Search Bar
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

@Composable
fun NoteGrid(
    notes: List<Note>,
    selectedNotes: Set<String>,
    isInSelectionMode: Boolean,
    isLoading: Boolean,
    currentFilter: MainViewModel.NoteFilter,
    listState: LazyStaggeredGridState,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit
) {
    if (isLoading && notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator()
                Text(
                    "Loading notes...",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    "No notes yet",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap + to create your first note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        // If sorting by Label, split into Active and Archived
        val showSeparator = currentFilter is MainViewModel.NoteFilter.Label
        
        // Base Lists (excluding Archive/Trash logic which is handled by ViewModel filter)
        // Wait, ViewModel already filters for Archive/Trash.
        // If Filter is Label, we might have mixed Archived/Active if we didn't filter in VM?
        // VM Logic for Label: `allNotes.filter { it.folder == filter.name && !it.isTrashed }`
        // So Label view includes Archived notes? Yes.
        
        val effectiveNotes = notes
        
        // Pinned Logic: Only relevant if NOT Trash
        val isTrash = currentFilter is MainViewModel.NoteFilter.Trash
        val isArchive = currentFilter is MainViewModel.NoteFilter.Archive
        
        // In Label View, we want: Pinned Active, Other Active, Archived.
        // In All View: Pinned, Others.

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = listState,
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
             if (isTrash || isArchive) {
                 // Simple List
                 items(effectiveNotes) { note ->
                    NoteCard(
                        note = note, 
                        isSelected = selectedNotes.contains(note.file.path),
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                 }
             } else {
                 val pinned = effectiveNotes.filter { it.isPinned && !it.isArchived }
                 val others = effectiveNotes.filter { !it.isPinned && !it.isArchived }
                 val archived = effectiveNotes.filter { it.isArchived } // For Label view

                 // Pinned Section
                 if (pinned.isNotEmpty()) {
                     item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                         Text(
                             text = "Pinned",
                             style = MaterialTheme.typography.labelSmall,
                             modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
                         )
                     }
                     items(pinned) { note ->
                        NoteCard(
                            note = note, 
                            isSelected = selectedNotes.contains(note.file.path),
                            onClick = { onNoteClick(note) },
                            onLongClick = { onNoteLongClick(note) }
                        )
                     }
                 }

                 // Others Section header (only if pinned exists)
                 if (pinned.isNotEmpty() && others.isNotEmpty()) {
                     item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                         Text(
                             text = "Others",
                             style = MaterialTheme.typography.labelSmall,
                             modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 16.dp)
                         )
                     }
                 }
                 
                 // Others Items
                 items(others) { note ->
                    NoteCard(
                        note = note, 
                        isSelected = selectedNotes.contains(note.file.path),
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                 }

                 // Archived Section (For Label View)
                 if (archived.isNotEmpty() && showSeparator) {
                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                         Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                             Text("Archived notes", style = MaterialTheme.typography.labelLarge)
                             androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                         }
                    }
                    items(archived) { note ->
                        NoteCard(
                            note = note, 
                            isSelected = selectedNotes.contains(note.file.path),
                            onClick = { onNoteClick(note) },
                            onLongClick = { onNoteLongClick(note) }
                        )
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
    
    val isTrash = currentFilter is MainViewModel.NoteFilter.Trash
    val isArchive = currentFilter is MainViewModel.NoteFilter.Archive

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete selected notes?") },
            text = { Text("Are you sure you want to delete these $selectionCount notes?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
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
                Icon(Icons.Default.Close, contentDescription = "Close Selection")
            }
        },
        actions = {
             // Restore (Visible if Trash or Archive)
             if (isTrash || isArchive) {
                 androidx.compose.material3.IconButton(onClick = onRestore) {
                     Icon(Icons.Default.Refresh, contentDescription = "Restore") // Use Refresh or Restore icon
                 }
             }
             
             // Move (Hidden in Trash)
             if (!isTrash) {
                 androidx.compose.material3.IconButton(onClick = { showMoveMenu = true }) {
                     Icon(Icons.Default.Add, contentDescription = "Move") 
                 }
                 DropdownMenu(
                     expanded = showMoveMenu,
                     onDismissRequest = { showMoveMenu = false }
                 ) {
                      DropdownMenuItem(
                         text = { Text("Inbox") },
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
                 }
             }
             
             // Archive (Hidden in Trash and Archive)
             if (!isTrash && !isArchive) {
                 androidx.compose.material3.IconButton(onClick = onArchive) {
                     Icon(Icons.Default.Check, contentDescription = "Archive")
                 }
                 
                 // Color Picker
                 Box {
                      androidx.compose.material3.IconButton(onClick = { showColorMenu = true }) {
                          // Use a circle or palette icon. Using a simple Circle for now or generic icon.
                          // Material Icons doesn't have Palette in Default easily without extended.
                          // Draw a circle.
                          Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray))
                      }
                      DropdownMenu(
                          expanded = showColorMenu,
                          onDismissRequest = { showColorMenu = false }
                      ) {
                          // Simple grid or list of colors
                          val colors = listOf(
                               0xFFFFFFFF, 0xFFF28B82, 0xFFFBBC04, 0xFFFFF475, 
                               0xFFCCFF90, 0xFFA7FFEB, 0xFFCBF0F8, 0xFFAECBFA, 
                               0xFFD7AEFB, 0xFFFDCFE8, 0xFFE6C9A8, 0xFFE8EAED
                          )
                          
                          // Use Rows of 4
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
             
             // Pin/Unpin (Hidden in Trash and Archive)
             if (!isTrash && !isArchive) {
                 androidx.compose.material3.IconButton(onClick = onPin) {
                     Icon(Icons.Default.Star, contentDescription = "Pin/Unpin")
                 }
             }
             
             // Delete
             androidx.compose.material3.IconButton(onClick = { showDeleteDialog = true }) {
                 Icon(Icons.Default.Delete, contentDescription = "Delete")
             }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}
