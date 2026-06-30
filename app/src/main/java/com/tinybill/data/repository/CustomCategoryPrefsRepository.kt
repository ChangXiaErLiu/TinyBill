package com.tinybill.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * 用户自定义分类的 DataStore 仓库。
 *
 * 替换原来的 CustomCategoryManager（基于 SharedPreferences）。
 * - 数据：JSON 数组持久化
 * - 暴露：`Flow<List<Category>>` 自动响应
 * - 线程：所有读 suspend / Flow，写 suspend，主线程不阻塞
 *
 * 注意：与 Room 的 CustomCategoryRepository（实体在 CustomCategory 表）是
 * 两条独立数据通路。本仓库是真实源；Room 那条历史上从未被初始化过（见
 * SessionMemory 2026-03-04），后续可彻底删除。
 */
private val Context.customCategoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tinybill_custom_category_v2"
)

class CustomCategoryPrefsRepository(context: Context) {

    private val store = context.applicationContext.customCategoryDataStore

    val categories: Flow<List<Category>> = store.data
        .map { prefs -> parseCategories(prefs[KEY_CATEGORIES]) }
        .distinctUntilChanged()

    suspend fun addCategory(
        name: String,
        icon: String,
        isEmoji: Boolean,
        colorValue: Long,
        type: Int,
    ) {
        store.edit { prefs ->
            val current = parseCategories(prefs[KEY_CATEGORIES]).toMutableList()
            if (current.none { it.name == name && it.type == type }) {
                current.add(Category(name, icon, isEmoji, colorValue, type))
                prefs[KEY_CATEGORIES] = serializeCategories(current)
            }
        }
    }

    suspend fun updateCategory(
        oldName: String,
        oldType: Int,
        newCategory: Category,
    ) {
        store.edit { prefs ->
            val current = parseCategories(prefs[KEY_CATEGORIES]).toMutableList()
            current.removeAll { it.name == oldName && it.type == oldType }
            current.add(newCategory)
            prefs[KEY_CATEGORIES] = serializeCategories(current)
        }
    }

    suspend fun deleteCategory(name: String, type: Int) {
        store.edit { prefs ->
            val current = parseCategories(prefs[KEY_CATEGORIES]).toMutableList()
            current.removeAll { it.name == name && it.type == type }
            prefs[KEY_CATEGORIES] = serializeCategories(current)
        }
    }

    suspend fun categoryExists(name: String, type: Int): Boolean {
        val all = categories.first()
        return all.any { it.name == name && it.type == type }
    }

    /**
     * 给 UI 用的便捷方法：把内置分类（颜色硬编码）和自定义分类合并。
     * 不进入 Flow 路径，只是纯计算，避免和 collect 抢资源。
     */
    fun mergeWithDefaults(
        custom: List<Category>,
        type: Int,
        builtIn: List<String>,
        defaultColorOf: (String) -> Color,
    ): List<Pair<String, Color>> {
        val builtInPairs = builtIn.map { it to defaultColorOf(it) }
        val customPairs = custom.filter { it.type == type }.map { it.name to it.color }
        return builtInPairs + customPairs
    }

    private fun parseCategories(json: String?): List<Category> {
        if (json.isNullOrEmpty() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Category(
                    name = obj.getString("name"),
                    icon = obj.getString("icon"),
                    isEmoji = obj.getBoolean("isEmoji"),
                    colorValue = obj.getLong("colorValue"),
                    type = obj.getInt("type"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeCategories(list: List<Category>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("name", c.name)
                    put("icon", c.icon)
                    put("isEmoji", c.isEmoji)
                    put("colorValue", c.colorValue)
                    put("type", c.type)
                }
            )
        }
        return arr.toString()
    }

    data class Category(
        val name: String,
        val icon: String,
        val isEmoji: Boolean,
        val colorValue: Long,
        val type: Int,
    ) {
        val color: Color get() = Color(colorValue)
    }

    companion object {
        private val KEY_CATEGORIES = stringPreferencesKey("categories")
    }
}
