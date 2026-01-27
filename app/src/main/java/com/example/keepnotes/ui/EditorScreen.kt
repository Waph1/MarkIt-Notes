package com.example.keepnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.example.keepnotes.data.model.Note
import java.io.File
import java.util.Date
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.model.RichTextState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentNote by viewModel.currentNote.collectAsState()
    val labels by viewModel.labels.collectAsState()
    
    var title by remember { mutableStateOf(currentNote?.title ?: "") }
    
    // Raw Content State (for Code Mode)
    var rawContent by remember { mutableStateOf(TextFieldValue(currentNote?.content ?: "")) }
    // Rich Content State (for WYSIWYG Mode)
    val richTextState = rememberRichTextState()
    // Track initial parsed content to avoid false-positive saves due to library normalization
    var initialParsedContent by remember { mutableStateOf<String?>(null) }
    
    var color by remember { mutableStateOf(currentNote?.color ?: 0xFFFFFFFF) }
    var folder by remember { mutableStateOf(currentNote?.folder?.takeIf { it != "Unknown" && it != "Inbox" } ?: "") }
    
    // UI States
    var showLabelMenu by remember { mutableStateOf(false) }
    var bottomBarState by remember { mutableStateOf(BottomBarState.DEFAULT) }
    var isCodeMode by remember { mutableStateOf(false) }

    // Initialize Rich Text State
    LaunchedEffect(currentNote) {
        if (currentNote != null) {
            richTextState.setMarkdown(currentNote!!.content)
            rawContent = TextFieldValue(currentNote!!.content)
            // Capture what the library thinks the markdown is immediately after load
            initialParsedContent = richTextState.toMarkdown()
        }
    }

    fun saveAndExit() {
        // Sync content based on mode
        val finalContent = if (isCodeMode) rawContent.text else richTextState.toMarkdown()
        
        if (title.isNotEmpty() || finalContent.isNotEmpty()) {
             val parentPath = if (folder.isEmpty()) "Inbox" else folder
             val fileName = currentNote?.file?.name?.takeIf { it.isNotEmpty() } ?: ""
             
             val fileObj = File(parentPath, fileName)

             val note = Note(
                file = fileObj,
                title = title.ifEmpty { "Untitled" },
                content = finalContent,
                lastModified = Date(),
                color = color
            )
            
            val isChanged = if (currentNote == null) {
                title.isNotEmpty() || finalContent.isNotEmpty()
            } else {
                title != currentNote?.title || 
                // Compare against the initial PARSED content if available, otherwise raw.
                (initialParsedContent != null && finalContent != initialParsedContent) ||
                (initialParsedContent == null && finalContent != currentNote?.content) ||
                color != currentNote?.color ||
                folder != (currentNote?.folder?.takeIf { it != "Unknown" && it != "Inbox" } ?: "")
            }
            
            if (isChanged) {
                viewModel.saveNote(note, currentNote?.file)
            }
        }
        onBack()
    }

    BackHandler {
        saveAndExit() 
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val noteColor = Color(color.toInt())
    
    // Improved Background Logic: Less contrast for Text Fields
    // Use the note color as the base.
    val backgroundColor = if (isDark) {
        if (color == 0xFFFFFFFF.toLong()) MaterialTheme.colorScheme.background
        else noteColor.copy(alpha = 0.1f).compositeOver(MaterialTheme.colorScheme.background)
    } else {
        noteColor
    }

    // Keyboard Detection
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    
    // Auto-open formatting when keyboard opens
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen) {
            bottomBarState = BottomBarState.FORMATTING
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Label Selector (Moved to Top)
                     Box {
                        IconButton(onClick = { showLabelMenu = true }) {
                            // Using generic icon for Label/Folder
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, contentDescription = "Label")
                        }
                        DropdownMenu(
                            expanded = showLabelMenu,
                            onDismissRequest = { showLabelMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Inbox (No Label)") },
                                onClick = {
                                    folder = ""
                                    showLabelMenu = false
                                }
                            )
                            labels.forEach { label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        folder = label
                                        showLabelMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Mode Toggle (Rich <-> Code)
                    IconButton(onClick = { 
                        if (isCodeMode) {
                            richTextState.setMarkdown(rawContent.text)
                        } else {
                            rawContent = TextFieldValue(richTextState.toMarkdown())
                        }
                        isCodeMode = !isCodeMode 
                    }) {
                         Icon(
                             if (isCodeMode) Icons.Default.Edit else Icons.Default.Menu, 
                             contentDescription = if (isCodeMode) "Show Rich Text" else "Show Code"
                         )
                    }

                    val currentNoteObj = currentNote
                    if (currentNoteObj != null) {
                        if (currentNoteObj.isTrashed) {
                            IconButton(onClick = { 
                                viewModel.restoreNote(currentNoteObj)
                                onBack() 
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Refresh, "Restore")
                            }
                        } else {
                            IconButton(onClick = { 
                                viewModel.deleteNote(currentNoteObj)
                                onBack()
                            }) {
                                Icon(Icons.Default.Delete, "Delete")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor
                )
            )
        },
        bottomBar = {
            // Dynamic Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor) // Match background
                    .padding(8.dp)
                    .imePadding()
            ) {
                 when (bottomBarState) {
                     BottomBarState.DEFAULT -> {
                         androidx.compose.foundation.layout.Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.Start, // Left aligned
                             verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                         ) {
                             // Palette Button
                             IconButton(onClick = { bottomBarState = BottomBarState.COLORS }) {
                                 Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray))
                             }
                             
                             androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))

                             // Text Format Button (Underlined 'A')
                             IconButton(onClick = { bottomBarState = BottomBarState.FORMATTING }) {
                                 Text(
                                     text = "A",
                                     style = MaterialTheme.typography.titleLarge.copy(
                                         textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                         fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                     )
                                 )
                             }
                             
                             // Push Date to the right
                             androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                             
                             // Date
                             Text(
                                 text = "Edited ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(Date())}",
                                 style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         }
                     }
                     BottomBarState.COLORS -> {
                         // Color Picker (Hides everything else)
                         ColorPicker(
                             selectedColor = color,
                             onColorSelected = { 
                                 color = it
                                 bottomBarState = BottomBarState.DEFAULT // Close on selection
                             }
                         )
                     }
                     BottomBarState.FORMATTING -> {
                         // Formatting Toolbar + Close Button
                         androidx.compose.foundation.layout.Row(
                             modifier = Modifier.fillMaxWidth(),
                             verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                         ) {
                             Box(modifier = Modifier.weight(1f)) {
                                 FormattingToolbar(
                                     isCodeMode = isCodeMode,
                                     richState = richTextState,
                                     onInsertRaw = { text, offset ->
                                         val currentText = rawContent.text
                                         val selection = rawContent.selection
                                         val start = if (selection.collapsed) selection.start else selection.min
                                         val end = if (selection.collapsed) selection.start else selection.max
                                         
                                         val prefix = text.substringBefore("><", text)
                                         val suffix = if (text.contains("><")) text.substringAfter("><") else ""
                                         
                                         val newText = if (suffix.isNotEmpty()) {
                                                 currentText.replaceRange(start, end, prefix + currentText.substring(start, end) + suffix)
                                         } else {
                                                 currentText.replaceRange(start, end, prefix)
                                         }
                                         
                                         val newCursorPos = start + prefix.length + (if (suffix.isNotEmpty()) currentText.substring(start, end).length else 0)
                                         
                                         rawContent = TextFieldValue(
                                             text = newText,
                                             selection = TextRange(newCursorPos)
                                         )
                                     }
                                 )
                             }
                             // Close Formatting
                             IconButton(onClick = { bottomBarState = BottomBarState.DEFAULT }) {
                                 Icon(androidx.compose.material.icons.Icons.Default.Close, "Close")
                             }
                         }
                     }
                 }
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.headlineMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (isCodeMode) {
                // Raw Markdown Editor
                TextField(
                    value = rawContent,
                    onValueChange = { rawContent = it },
                    placeholder = { Text("Note (Markdown)") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                // WYSIWYG Editor
                RichTextEditor(
                    state = richTextState,
                    placeholder = { Text("Note") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

enum class BottomBarState {
    DEFAULT,
    COLORS,
    FORMATTING
}

@Composable
fun FormattingToolbar(
    isCodeMode: Boolean,
    richState: RichTextState,
    onInsertRaw: (String, Int) -> Unit
) {
    LazyRow(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isCodeMode) {
             // Raw Markdown Actions
            item { ToolbarButton("B", true, false) { onInsertRaw("**><**", 2) } }
            item { ToolbarButton("I", false, false) { onInsertRaw("_><_", 1) } }
            item { ToolbarButton("H1", true, false) { onInsertRaw("# ", 2) } }
            item { ToolbarButton("-", true, false) { onInsertRaw("- ", 2) } }
            item { ToolbarButton("[ ]", false, false) { onInsertRaw("- [ ] ", 6) } }
        } else {
            // Rich Text Actions
            val spanStyle = richState.currentSpanStyle
            val isBold = spanStyle.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold
            val isItalic = spanStyle.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic
            val isUnderline = spanStyle.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.Underline) == true
            
            item { 
                ToolbarButton("B", true, isBold) { 
                    richState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) 
                } 
            }
            item { ToolbarButton("I", false, isItalic) { 
                richState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) 
            } }
            item { ToolbarButton("U", false, isUnderline) { 
                richState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) 
            } }
            
            item { ToolbarButton("-", true, false) { richState.toggleUnorderedList() } }
            item { ToolbarButton("1.", false, false) { richState.toggleOrderedList() } }
            
            // Indent Button (Tab)
            item {
                ToolbarButton("→|", true, false) {
                    try {
                        val markdown = richState.toMarkdown()
                        val cursor = richState.selection.max
                        
                        if (cursor >= 0 && cursor <= markdown.length) {
                             // Insert 4 non-breaking spaces for manual indentation
                             val indent = "\u00A0\u00A0\u00A0\u00A0" 
                             val newMarkdown = markdown.substring(0, cursor) + indent + markdown.substring(cursor)
                             richState.setMarkdown(newMarkdown)
                             richState.selection = TextRange(cursor + 4)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}


@Composable
fun ToolbarButton(text: String, bold: Boolean, isActive: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = Modifier
            .clip(CircleShape) // Changed to CircleShape
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text, 
            fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
fun ColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    val defaultColor = 0xFFFFFFFF
    val allColors = listOf(
        0xFFFFFFFF, 0xFFF28B82, 0xFFFBBC04, 0xFFFFF475, 
        0xFFCCFF90, 0xFFA7FFEB, 0xFFCBF0F8, 0xFFAECBFA, 
        0xFFD7AEFB, 0xFFFDCFE8, 0xFFE6C9A8, 0xFFE8EAED
    )
    
    // Sort ordering: [Selected, Default, ...Rest] (Deduplicated)
    val orderedColors = remember(selectedColor) {
        val list = mutableListOf<Long>()
        if (selectedColor != defaultColor) {
            list.add(selectedColor)
            list.add(defaultColor)
        } else {
            list.add(defaultColor)
        }
        list.addAll(allColors.filter { it != selectedColor && it != defaultColor })
        list
    }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(orderedColors) { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color.toInt()))
                    .border(
                        width = if (color == selectedColor) 2.dp else 1.dp,
                        color = if (color == selectedColor) Color.Blue else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}
