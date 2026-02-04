package com.waph1.markit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waph1.markit.R
import com.waph1.markit.data.repository.PrefsManager

@Composable
fun SearchBar(
    viewModel: MainViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Input
        BasicTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Clear Button (only if text exists)
        if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sort Button (Right Edge)
        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.sort_notes),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_date_modified)) },
                    onClick = {
                        viewModel.setSortOrder(PrefsManager.SortOrder.DATE_MODIFIED)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_date_created)) },
                    onClick = {
                        viewModel.setSortOrder(PrefsManager.SortOrder.DATE_CREATED)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_title)) },
                    onClick = {
                        viewModel.setSortOrder(PrefsManager.SortOrder.TITLE)
                        showSortMenu = false
                    }
                )
                androidx.compose.material3.Divider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_ascending)) },
                    onClick = {
                        viewModel.setSortDirection(PrefsManager.SortDirection.ASCENDING)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_descending)) },
                    onClick = {
                        viewModel.setSortDirection(PrefsManager.SortDirection.DESCENDING)
                        showSortMenu = false
                    }
                )
            }
        }
    }
}
