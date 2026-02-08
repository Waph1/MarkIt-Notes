package com.waph1.markit.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.waph1.markit.data.model.Note
import java.io.File
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    initialLabel: String = ""
) {
    val currentNote by viewModel.currentNote.collectAsState()
    val labels by viewModel.labels.collectAsState()
    
    var title by remember { mutableStateOf(currentNote?.title ?: "") }
    var content by remember { mutableStateOf(TextFieldValue(currentNote?.content ?: "")) }
    var color by remember { mutableStateOf(currentNote?.color ?: 0xFFFFFFFF) }
    var folder by remember(currentNote, initialLabel) { 
        mutableStateOf(
            currentNote?.folder?.takeIf { it != "Unknown" && it != "Inbox" } 
            ?: initialLabel
        ) 
    }
    
    // UI States
    var showLabelMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showCreateLabelDialog by remember { mutableStateOf(false) }

    if (showCreateLabelDialog) {
        CreateLabelDialog(
            onDismiss = { showCreateLabelDialog = false },
            onConfirm = { name ->
                viewModel.createLabel(name)
                folder = name
                showCreateLabelDialog = false
            }
        )
    }
    
    // Mode: New notes start in Edit, existing in View
    var isEditing by remember { mutableStateOf(currentNote == null) }
    
    // Long-press menu states
    var showHeadingMenu by remember { mutableStateOf(false) }
    var showMathMenu by remember { mutableStateOf(false) }
    
    // Initialize content from note
    LaunchedEffect(currentNote) {
        if (currentNote != null) {
            title = currentNote!!.title
            content = TextFieldValue(currentNote!!.content)
            color = currentNote!!.color
            folder = currentNote!!.folder.takeIf { it != "Unknown" && it != "Inbox" } ?: ""
        }
    }
    
    fun saveNote() {
        if (title.isNotEmpty() || content.text.isNotEmpty()) {
            val parentPath = if (folder.isEmpty()) "Inbox" else folder
            val fileName = currentNote?.file?.name?.takeIf { it.isNotEmpty() } ?: "new_note_placeholder"
            val fileObj = File(parentPath, fileName)
            
            val note = Note(
                file = fileObj,
                title = title.ifEmpty { 
                    val dateFormat = java.text.SimpleDateFormat("yyyy_MM_dd_HH_mm", java.util.Locale.getDefault())
                    dateFormat.format(Date())
                },
                content = content.text,
                lastModified = Date(),
                color = color,
                isPinned = currentNote?.isPinned ?: false,
                isArchived = currentNote?.isArchived ?: false,
                isTrashed = currentNote?.isTrashed ?: false
            )
            
            val isChanged = if (currentNote == null) {
                title.isNotEmpty() || content.text.isNotEmpty()
            } else {
                title != currentNote?.title || 
                content.text != currentNote?.content ||
                color != currentNote?.color ||
                folder != (currentNote?.folder?.takeIf { it != "Unknown" && it != "Inbox" } ?: "")
            }
            
            if (isChanged) {
                viewModel.saveNote(note, currentNote?.file)
            }
        }
    }
    
    // Helper to insert text at cursor
    fun insertAtCursor(prefix: String, suffix: String = "") {
        val text = content.text
        val start = content.selection.min
        val end = content.selection.max
        val selectedText = text.substring(start, end)
        
        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        val newCursor = start + prefix.length + selectedText.length
        
        content = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }
    
    // Back Handler
    BackHandler {
        if (isEditing) {
            saveNote()
            isEditing = false
        } else {
            onBack()
        }
    }
    
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val noteColor = Color(color.toInt())
    val backgroundColor = if (isDark) {
        if (color == 0xFFFFFFFF.toLong()) MaterialTheme.colorScheme.background
        else noteColor.copy(alpha = 0.1f).compositeOver(MaterialTheme.colorScheme.background)
    } else {
        noteColor
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (isEditing) {
                        IconButton(onClick = { 
                            saveNote()
                            isEditing = false
                        }) {
                            Icon(Icons.Outlined.Check, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.done))
                        }
                    } else {
                        IconButton(onClick = { onBack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.back))
                        }
                    }
                },
                actions = {
                    // Label Selector
                    Box {
                        IconButton(onClick = { showLabelMenu = true }) {
                            Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.label), 
                                 modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showLabelMenu,
                            onDismissRequest = { showLabelMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.inbox_no_label)) },
                                onClick = { folder = ""; showLabelMenu = false }
                            )
                            labels.forEach { label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { folder = label; showLabelMenu = false }
                                )

                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.create_new_label)) },
                                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                                onClick = { 
                                    showLabelMenu = false
                                    showCreateLabelDialog = true
                                }
                            )
                        }
                    }
                    
                    if (!isEditing) {
                        // Edit button
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.edit))
                        }
                    }
                    
                    // Delete/Restore & Archive
                    val currentNoteObj = currentNote
                    if (currentNoteObj != null) {
                        // Archive/Unarchive (if not Trashed)
                        if (!currentNoteObj.isTrashed) {
                             if (currentNoteObj.isArchived) {
                                 IconButton(onClick = { 
                                     viewModel.restoreNote(currentNoteObj)
                                     onBack()
                                 }) {
                                     Icon(Icons.Outlined.Refresh, androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.unarchive))
                                 }
                             } else {
                                 IconButton(onClick = { 
                                     viewModel.archiveNote(currentNoteObj)
                                     onBack()
                                 }) {
                                     Icon(Icons.Outlined.Archive, androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.archive))
                                 }
                             }
                        }

                        if (currentNoteObj.isTrashed) {
                            IconButton(onClick = { 
                                viewModel.restoreNote(currentNoteObj)
                                onBack() 
                            }) {
                                Icon(Icons.Outlined.Refresh, androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.restore))
                            }
                        } else {
                            IconButton(onClick = { 
                                viewModel.deleteNote(currentNoteObj)
                                onBack()
                            }) {
                                Icon(Icons.Outlined.Delete, androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.delete))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        bottomBar = {
            if (isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .imePadding()
                ) {
                    if (showColorPicker) {
                        ColorPicker(
                            selectedColor = color,
                            onColorSelected = { 
                                color = it
                                showColorPicker = false
                            }
                        )
                    } else {
                        // Formatting Toolbar
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color Picker Button
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(color.toInt()))
                                        .border(1.dp, Color.Gray, CircleShape)
                                        .clickable { showColorPicker = true }
                                )
                            }
                            
                            item { Spacer(Modifier.width(8.dp)) }
                            
                            // Heading (with long-press menu)
                            item {
                                Box {
                                    ToolbarIconButton(
                                        text = "H1",
                                        bold = true,
                                        onClick = { insertAtCursor("# ") },
                                        onLongClick = { showHeadingMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showHeadingMenu,
                                        onDismissRequest = { showHeadingMenu = false }
                                    ) {
                                        listOf("H1" to "# ", "H2" to "## ", "H3" to "### ", "H4" to "#### ").forEach { (label, md) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = { insertAtCursor(md); showHeadingMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Separator
                            item { ToolbarIconButton(text = "—", onClick = { insertAtCursor("---\n") }) }
                            
                            // Vertical Divider
                            item { VerticalDivider(Modifier.height(24.dp)) }
                            
                            // Bold
                            item { ToolbarIconButton(text = "B", bold = true, onClick = { insertAtCursor("**", "**") }) }
                            
                            // Italic
                            item { ToolbarIconButton(text = "I", italic = true, onClick = { insertAtCursor("_", "_") }) }
                            
                            // Underline
                            item { ToolbarIconButton(text = "U", underline = true, onClick = { insertAtCursor("<u>", "</u>") }) }
                            
                            // Strikethrough
                            item { ToolbarIconButton(text = "S", strikethrough = true, onClick = { insertAtCursor("~~", "~~") }) }
                            
                            // Vertical Divider
                            item { VerticalDivider(Modifier.height(24.dp)) }
                            
                            // URL
                            item { ToolbarIconButton(text = "🔗", onClick = { insertAtCursor("[", "](url)") }) }
                            
                            // Inline Code
                            item { ToolbarIconButton(text = "<>", onClick = { insertAtCursor("`", "`") }) }
                            
                            // Quote
                            item { ToolbarIconButton(text = "\"", onClick = { insertAtCursor("> ") }) }
                            
                            // Math (with long-press menu)
                            item {
                                Box {
                                    ToolbarIconButton(
                                        text = "ƒ",
                                        onClick = { insertAtCursor("$", "$") },
                                        onLongClick = { showMathMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showMathMenu,
                                        onDismissRequest = { showMathMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.inline_math)) },
                                            onClick = { insertAtCursor("$", "$"); showMathMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.block_math)) },
                                            onClick = { insertAtCursor("$$\n", "\n$$"); showMathMenu = false }
                                        )
                                    }
                                }
                            }
                            
                            // Bullet List
                            item { ToolbarIconButton(text = "•", onClick = { insertAtCursor("- ") }) }
                            
                            // Numbered List
                            item { ToolbarIconButton(text = "1.", onClick = { insertAtCursor("1. ") }) }
                            
                            // Checkbox
                            item { ToolbarIconButton(text = "☐", onClick = { insertAtCursor("- [ ] ") }) }
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
            if (isEditing) {
                // EDIT MODE: Plain text
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.title_hint)) },
                    textStyle = MaterialTheme.typography.headlineMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (content.text.isEmpty()) {
                                Text(
                                    androidx.compose.ui.res.stringResource(com.waph1.markit.R.string.start_typing_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                // VIEW MODE: Rendered markdown
                PreviewWebView(
                    content = "# ${title}\n\n${content.text}",
                    isDark = isDark,
                    onCheckboxToggled = { index, checked ->
                        val newText = toggleTask(content.text, index, checked)
                        content = TextFieldValue(newText)
                        
                        // Auto-save on checkbox toggle
                        val parentPath = if (folder.isEmpty()) "Inbox" else folder
                        val fileName = currentNote?.file?.name ?: ""
                        val fileObj = File(parentPath, fileName)
                        val note = Note(
                            file = fileObj,
                            title = title.ifEmpty { 
                                val dateFormat = java.text.SimpleDateFormat("yyyy_MM_dd_HH_mm", java.util.Locale.getDefault())
                                dateFormat.format(Date())
                            },
                            content = newText,
                            lastModified = Date(),
                            color = color
                        )
                        viewModel.saveNote(note, currentNote?.file)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarIconButton(
    text: String,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strikethrough: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = when {
                    underline && strikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    underline -> TextDecoration.Underline
                    strikethrough -> TextDecoration.LineThrough
                    else -> null
                }
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    val colors = listOf(
        0xFFFFFFFF, 0xFFF28B82, 0xFFFBBC04, 0xFFFFF475, 
        0xFFCCFF90, 0xFFA7FFEB, 0xFFCBF0F8, 0xFFAECBFA, 
        0xFFD7AEFB, 0xFFFDCFE8, 0xFFE6C9A8, 0xFFE8EAED
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        items(colors) { c ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(c.toInt()))
                    .border(
                        width = if (c == selectedColor) 2.dp else 1.dp,
                        color = if (c == selectedColor) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(c) }
            )
        }
    }
}

// Helper to toggle checkbox in markdown text
fun toggleTask(markdown: String, index: Int, checked: Boolean): String {
    val regex = Regex("- \\[[ xX]\\]")
    var matchIndex = 0
    
    return regex.replace(markdown) { matchResult ->
        if (matchIndex++ == index) {
            if (checked) "- [x]" else "- [ ]"
        } else {
            matchResult.value
        }
    }
}

// Extension to escape string for JavaScript double-quoted strings
fun String.escapeForJs(): String {
    return this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")
}

@Composable
fun PreviewWebView(
    content: String,
    isDark: Boolean,
    onCheckboxToggled: (Int, Boolean) -> Unit
) {
    val escapedContent = content.escapeForJs()
    
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                setBackgroundColor(0) // Transparent
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = true
                
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onCheckboxToggled(index: Int, checked: Boolean) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onCheckboxToggled(index, checked)
                        }
                    }
                }, "Android")
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        val jsCode = "updateContent(\"" + escapedContent + "\", " + isDark + ")"
                        evaluateJavascript(jsCode, null)
                    }
                }
                
                loadUrl("file:///android_asset/preview/preview.html?dark=$isDark")
            }
        },
        update = { view ->
            val jsCode = "updateContent(\"" + escapedContent + "\", " + isDark + ")"
            view.evaluateJavascript(jsCode, null)
        }
    )
}
