package com.tinybill.data.repository

import com.tinybill.data.dao.TemplateDao
import com.tinybill.data.entity.Template
import kotlinx.coroutines.flow.Flow

/**
 * 记账模板仓库。
 *
 * 封装 TemplateDao，管理常用记账模板的 CRUD 和使用频率统计。
 * 在 UI 层（QuickAdd）通过 Koin 注入使用。
 */
class TemplateRepository(
    private val templateDao: TemplateDao
) {
    /** 获取所有模板，按使用次数降序 */
    val allTemplates: Flow<List<Template>> = templateDao.getAllTemplates()

    /** 获取使用频率最高的 N 个模板 */
    suspend fun getTopTemplates(limit: Int = 6): List<Template> =
        templateDao.getTopTemplates(limit)

    /** 搜索模板（按名称或商户名） */
    suspend fun searchTemplates(keyword: String): List<Template> =
        templateDao.searchTemplates(keyword)

    /** 新增模板 */
    suspend fun addTemplate(template: Template): Long =
        templateDao.insert(template)

    /** 更新模板 */
    suspend fun updateTemplate(template: Template) =
        templateDao.update(template)

    /** 删除模板 */
    suspend fun deleteTemplate(template: Template) =
        templateDao.delete(template)

    /** 使用模板（增加使用计数 + 记录最后使用时间） */
    suspend fun useTemplate(templateId: Long) {
        templateDao.incrementUseCount(templateId, System.currentTimeMillis())
    }

    /**
     * 从一笔交易自动创建模板（去重检测）。
     * 如果已存在相同 merchant + category + type 的模板，不再重复创建。
     * @return 是否创建了新模板
     */
    suspend fun createFromTransaction(
        merchant: String,
        amount: Double,
        category: String,
        type: Int,
        name: String? = null
    ): Boolean {
        val existing = templateDao.searchTemplates(merchant)
        if (existing.any { it.merchant == merchant && it.category == category && it.type == type }) {
            return false // 已存在，不重复创建
        }
        templateDao.insert(
            Template(
                name = name ?: merchant.ifBlank { category },
                amount = amount,
                merchant = merchant,
                category = category,
                type = type,
                useCount = 1,
                lastUsed = System.currentTimeMillis()
            )
        )
        return true
    }
}
