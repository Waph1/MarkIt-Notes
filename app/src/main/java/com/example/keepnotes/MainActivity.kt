package com.example.keepnotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keepnotes.data.repository.FileNoteRepository
import com.example.keepnotes.data.repository.MetadataManager
import com.example.keepnotes.data.repository.PrefsManager
import com.example.keepnotes.ui.DashboardScreen
import com.example.keepnotes.ui.EditorScreen
import com.example.keepnotes.ui.MainViewModel
import com.example.keepnotes.ui.theme.KeepNotesTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: FileNoteRepository
    private lateinit var metadataManager: MetadataManager
    private lateinit var prefsManager: PrefsManager

    private val viewModel by viewModels<MainViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, metadataManager, prefsManager) as T
            }
        }
    }

    private val openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            prefsManager.saveRootUri(it.toString())
            viewModel.setRootFolder(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        metadataManager = MetadataManager(applicationContext)
        repository = FileNoteRepository(applicationContext, metadataManager)
        prefsManager = PrefsManager(applicationContext)
        
        // Auto-load if persisted
        // Auto-load if persisted
        val savedUriStr = prefsManager.getRootUri()
        if (savedUriStr != null) {
            val uri = android.net.Uri.parse(savedUriStr)
            // Check if we still have permission
            val hasPermission = contentResolver.persistedUriPermissions.any { 
                it.uri == uri && (it.isReadPermission || it.isWritePermission) 
            }
            if (hasPermission) {
                viewModel.setRootFolder(uri)
            } else {
                viewModel.resetPermissionNeeded()
            }
        }

        setContent {
            KeepNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isEditorOpen by viewModel.isEditorOpen.collectAsState()
                    val listState = rememberLazyStaggeredGridState()
                    
                    if (isEditorOpen) {
                        EditorScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.closeEditor() }
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            listState = listState,
                            onSelectFolder = { openDocumentTreeLauncher.launch(null) },
                            onNoteClick = { note -> viewModel.openNote(note) },
                            onFabClick = { viewModel.createNote() }
                        )
                    }
                }
            }
        }
    }
}
