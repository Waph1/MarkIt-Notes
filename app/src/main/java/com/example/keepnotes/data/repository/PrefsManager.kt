package com.example.keepnotes.data.repository

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keepnotes_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ROOT_URI = "root_uri"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_SORT_DIRECTION = "sort_direction"
    }

    enum class SortOrder {
        DATE_CREATED,
        TITLE,
        DATE_MODIFIED
    }

    enum class SortDirection {
        ASCENDING,
        DESCENDING
    }

    fun saveSortOrder(order: SortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
    }

    fun getSortOrder(): SortOrder {
        val name = prefs.getString(KEY_SORT_ORDER, SortOrder.DATE_CREATED.name)
        return try {
            SortOrder.valueOf(name ?: SortOrder.DATE_CREATED.name)
        } catch (e: Exception) {
            SortOrder.DATE_CREATED
        }
    }

    fun saveSortDirection(direction: SortDirection) {
        prefs.edit().putString(KEY_SORT_DIRECTION, direction.name).apply()
    }

    fun getSortDirection(): SortDirection {
        val name = prefs.getString(KEY_SORT_DIRECTION, SortDirection.DESCENDING.name)
        return try {
            SortDirection.valueOf(name ?: SortDirection.DESCENDING.name)
        } catch (e: Exception) {
            SortDirection.DESCENDING
        }
    }

    fun saveRootUri(uri: String) {
        prefs.edit().putString(KEY_ROOT_URI, uri).apply()
    }

    fun getRootUri(): String? {
        return prefs.getString(KEY_ROOT_URI, null)
    }
}
