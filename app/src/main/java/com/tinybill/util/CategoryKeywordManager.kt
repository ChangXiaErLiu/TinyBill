package com.tinybill.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.InputStreamReader

data class CategoryKeywords(
    val version: Int,
    val updateTime: String,
    val expenseCategories: Map<String, List<String>>,
    val incomeCategories: Map<String, List<String>>,
    val excludedTexts: List<String>
)

data class KeywordUpdateResult(
    val success: Boolean,
    val message: String,
    val keywords: CategoryKeywords? = null
)

class CategoryKeywordManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private var cachedKeywords: CategoryKeywords? = null

    companion object {
        private const val PREFS_NAME = "tinybill_keywords"
        private const val KEY_LOCAL_VERSION = "local_version"
        private const val KEY_LOCAL_KEYWORDS = "local_keywords"
        private const val KEY_CUSTOM_KEYWORDS = "custom_keywords"

        @Volatile
        private var INSTANCE: CategoryKeywordManager? = null

        fun getInstance(context: Context): CategoryKeywordManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CategoryKeywordManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun loadKeywords(): CategoryKeywords {
        cachedKeywords?.let { return it }

        val localJson = prefs.getString(KEY_LOCAL_KEYWORDS, null)
        if (localJson != null) {
            return try {
                val keywords = parseKeywords(localJson)
                cachedKeywords = keywords
                keywords
            } catch (e: Exception) {
                loadFromAssets()
            }
        }

        return loadFromAssets()
    }

    private fun loadFromAssets(): CategoryKeywords {
        return try {
            context.assets.open("category_keywords.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val json = reader.readText()
                    val keywords = parseKeywords(json)
                    prefs.edit().putString(KEY_LOCAL_KEYWORDS, json).apply()
                    cachedKeywords = keywords
                    keywords
                }
            }
        } catch (e: Exception) {
            getDefaultKeywords()
        }
    }

    private fun parseKeywords(json: String): CategoryKeywords {
        val parser = JsonParser()
        val obj = parser.parse(json).asJsonObject

        val expenseCategories = mutableMapOf<String, List<String>>()
        val expenseObj = obj.getAsJsonObject("expenseCategories")
        expenseObj.entrySet().forEach { (key, value) ->
            expenseCategories[key] = value.asJsonArray.map { it.asString }
        }

        val incomeCategories = mutableMapOf<String, List<String>>()
        val incomeObj = obj.getAsJsonObject("incomeCategories")
        incomeObj.entrySet().forEach { (key, value) ->
            incomeCategories[key] = value.asJsonArray.map { it.asString }
        }

        return CategoryKeywords(
            version = obj.get("version")?.asInt ?: 1,
            updateTime = obj.get("updateTime")?.asString ?: "",
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            excludedTexts = obj.getAsJsonArray("excludedTexts").map { it.asString }
        )
    }

    fun updateFromFile(file: File): KeywordUpdateResult {
        return try {
            val json = file.readText()
            validateAndSaveKeywords(json)
        } catch (e: Exception) {
            KeywordUpdateResult(false, "读取文件失败: ${e.message}")
        }
    }

    fun updateFromJson(jsonString: String): KeywordUpdateResult {
        return try {
            validateAndSaveKeywords(jsonString)
        } catch (e: Exception) {
            KeywordUpdateResult(false, "解析 JSON 失败: ${e.message}")
        }
    }

    private fun validateAndSaveKeywords(json: String): KeywordUpdateResult {
        val keywords = parseKeywords(json)

        if (keywords.expenseCategories.isEmpty()) {
            return KeywordUpdateResult(false, "支出分类不能为空")
        }

        prefs.edit()
            .putString(KEY_LOCAL_KEYWORDS, json)
            .putInt(KEY_LOCAL_VERSION, keywords.version)
            .apply()

        cachedKeywords = keywords

        return KeywordUpdateResult(true, "更新成功", keywords)
    }

    fun addCustomKeyword(category: String, keywords: List<String>, type: Int) {
        val custom = getCustomKeywords().toMutableMap()
        val key = if (type == 0) "expense_$category" else "income_$category"
        custom[key] = keywords

        val json = gson.toJson(custom)
        prefs.edit().putString(KEY_CUSTOM_KEYWORDS, json).apply()
    }

    fun getCustomKeywords(): Map<String, List<String>> {
        val json = prefs.getString(KEY_CUSTOM_KEYWORDS, "{}") ?: "{}"
        return try {
            gson.fromJson(json, Map::class.java) as Map<String, List<String>>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getEffectiveKeywords(): CategoryKeywords {
        val base = loadKeywords()
        val custom = getCustomKeywords()

        val mergedExpense = base.expenseCategories.toMutableMap()
        val mergedIncome = base.incomeCategories.toMutableMap()

        custom.forEach { (key, keywords) ->
            if (key.startsWith("expense_")) {
                val category = key.removePrefix("expense_")
                val existing = mergedExpense[category] ?: emptyList()
                mergedExpense[category] = existing + keywords
            } else if (key.startsWith("income_")) {
                val category = key.removePrefix("income_")
                val existing = mergedIncome[category] ?: emptyList()
                mergedIncome[category] = existing + keywords
            }
        }

        return CategoryKeywords(
            version = base.version,
            updateTime = base.updateTime,
            expenseCategories = mergedExpense,
            incomeCategories = mergedIncome,
            excludedTexts = base.excludedTexts
        )
    }

    fun getMerchantCategory(merchant: String, type: Int): String? {
        val keywords = getEffectiveKeywords()
        val categories = if (type == 0) keywords.expenseCategories else keywords.incomeCategories
        val lowerMerchant = merchant.lowercase()

        for ((category, categoryKeywords) in categories) {
            for (keyword in categoryKeywords) {
                if (lowerMerchant.contains(keyword.lowercase())) {
                    return category
                }
            }
        }

        return null
    }

    fun getCurrentVersion(): Int {
        return prefs.getInt(KEY_LOCAL_VERSION, 0)
    }

    fun resetToDefault(): Boolean {
        return try {
            prefs.edit().remove(KEY_LOCAL_KEYWORDS).apply()
            cachedKeywords = null
            loadFromAssets()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getDefaultKeywords(): CategoryKeywords {
        return CategoryKeywords(
            version = 1,
            updateTime = "",
            expenseCategories = mapOf(
                "餐饮" to listOf("美团", "饿了么", "餐厅", "饭店", "肯德基", "麦当劳"),
                "购物" to listOf("淘宝", "京东", "拼多多", "天猫", "超市", "便利店"),
                "交通" to listOf("地铁", "公交", "滴滴", "出租车", "加油", "停车"),
                "娱乐" to listOf("电影", "视频", "会员", "游戏", "KTV", "网吧"),
                "其他" to emptyList()
            ),
            incomeCategories = mapOf(
                "工资" to listOf("工资", "薪资"),
                "红包" to listOf("微信红包", "支付宝红包"),
                "转账" to listOf("转账收入"),
                "其他收入" to listOf("退款")
            ),
            excludedTexts = listOf("支付", "付款", "商户", "金额", "成功", "失败")
        )
    }
}
