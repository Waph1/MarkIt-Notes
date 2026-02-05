package com.waph1.markit.data.repository

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.waph1.markit.data.model.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MetadataManager(private val context: Context) {
    private val gson = Gson()
    private val CONFIG_FILENAME = "keepnotes_config.json" // Removed leading dot to ensure visibility

    suspend fun loadConfig(rootDir: DocumentFile): AppConfig = withContext(Dispatchers.IO) {
        val file = rootDir.findFile(CONFIG_FILENAME)
        if (file != null && file.isFile) {
            try {
                context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    gson.fromJson(reader, AppConfig::class.java)
                } ?: AppConfig()
            } catch (e: Exception) {
                e.printStackTrace() // Log error
                AppConfig()
            }
        } else {
            AppConfig()
        }
    }

    suspend fun saveConfig(rootDir: DocumentFile, config: AppConfig) = withContext(Dispatchers.IO) {
        var file = rootDir.findFile(CONFIG_FILENAME)
        if (file == null) {
            file = rootDir.createFile("application/json", CONFIG_FILENAME)
        }
        
        file?.let {
            try {
                // Use "wt" mode to truncate file before writing
                context.contentResolver.openOutputStream(it.uri, "wt")?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    gson.toJson(config, writer)
                    writer.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log error for debugging
            }
        }
    }
}
