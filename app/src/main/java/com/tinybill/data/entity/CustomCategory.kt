package com.tinybill.data.entity

/**
 * 自定义分类数据类（非 Room Entity）。
 *
 * 自定义分类数据实际由 CustomCategoryPrefsRepository（DataStore）持久化。
 * 本类仅作为备份导出的 DTO。
 */
data class CustomCategory(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: Int,
    val isDefault: Int = 0
) {
    companion object {
        const val TYPE_EXPENSE = 0
        const val TYPE_INCOME = 1
    }
}