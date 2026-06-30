package com.tinybill.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryKeywordManagerTest {

    @Test
    fun `test expense category matching - food merchants`() {
        val manager = CategoryKeywordManagerTestImpl()
        val testCases = listOf(
            "美团外卖" to "餐饮",
            "饿了么订单" to "餐饮",
            "麦当劳" to "餐饮",
            "肯德基宅急送" to "餐饮",
            "星巴克咖啡" to "餐饮",
            "瑞幸咖啡" to "餐饮"
        )

        testCases.forEach { (merchant, expectedCategory) ->
            val result = manager.getMerchantCategory(merchant, 0)
            assertThat(result).isEqualTo(expectedCategory)
        }
    }

    @Test
    fun `test expense category matching - shopping merchants`() {
        val manager = CategoryKeywordManagerTestImpl()
        val testCases = listOf(
            "淘宝订单" to "购物",
            "京东购物" to "购物",
            "拼多多" to "购物",
            "天猫旗舰店" to "购物",
            "盒马鲜生" to "购物",
            "沃尔玛超市" to "购物"
        )

        testCases.forEach { (merchant, expectedCategory) ->
            val result = manager.getMerchantCategory(merchant, 0)
            assertThat(result).isEqualTo(expectedCategory)
        }
    }

    @Test
    fun `test expense category matching - transport merchants`() {
        val manager = CategoryKeywordManagerTestImpl()
        val testCases = listOf(
            "地铁乘车" to "交通",
            "滴滴出行" to "交通",
            "出租车费" to "交通",
            "中石油加油" to "交通",
            "停车费" to "交通",
            "高铁票" to "交通"
        )

        testCases.forEach { (merchant, expectedCategory) ->
            val result = manager.getMerchantCategory(merchant, 0)
            assertThat(result).isEqualTo(expectedCategory)
        }
    }

    @Test
    fun `test income category matching`() {
        val manager = CategoryKeywordManagerTestImpl()
        val testCases = listOf(
            "工资发放" to "工资",
            "薪资到账" to "工资",
            "微信红包" to "红包",
            "支付宝红包" to "红包",
            "收到转账" to "转账"
        )

        testCases.forEach { (merchant, expectedCategory) ->
            val result = manager.getMerchantCategory(merchant, 1)
            assertThat(result).isEqualTo(expectedCategory)
        }
    }

    @Test
    fun `test unknown merchant returns other category`() {
        val manager = CategoryKeywordManagerTestImpl()
        val unknownMerchants = listOf(
            "未知商户",
            "第三方平台",
            "Random Store"
        )

        unknownMerchants.forEach { merchant ->
            val result = manager.getMerchantCategory(merchant, 0)
            assertThat(result).isEqualTo("其他")
        }
    }

    @Test
    fun `test case insensitive matching`() {
        val manager = CategoryKeywordManagerTestImpl()
        val testCases = listOf(
            "MEITUAN" to "餐饮",
            "JD" to "购物",
            "DIDI" to "交通"
        )

        testCases.forEach { (merchant, expectedCategory) ->
            val result = manager.getMerchantCategory(merchant, 0)
            assertThat(result).isEqualTo(expectedCategory)
        }
    }
}

class CategoryKeywordManagerTestImpl : CategoryKeywordManagerTestInterface {
    private val expenseCategories = mapOf(
        "餐饮" to listOf("美团", "饿了么", "餐厅", "饭店", "肯德基", "麦当劳", "星巴克", "咖啡", "奶茶", "瑞幸"),
        "购物" to listOf("淘宝", "京东", "拼多多", "天猫", "超市", "便利店", "沃尔玛", "盒马"),
        "交通" to listOf("地铁", "公交", "滴滴", "出租车", "加油", "停车", "高铁"),
        "其他" to emptyList()
    )

    private val incomeCategories = mapOf(
        "工资" to listOf("工资", "薪资"),
        "红包" to listOf("微信红包", "支付宝红包"),
        "转账" to listOf("转账", "收到转账"),
        "其他收入" to listOf("退款")
    )

    override fun getMerchantCategory(merchant: String, type: Int): String? {
        val categories = if (type == 0) expenseCategories else incomeCategories
        val lowerMerchant = merchant.lowercase()

        for ((category, keywords) in categories) {
            for (keyword in keywords) {
                if (lowerMerchant.contains(keyword.lowercase())) {
                    return category
                }
            }
        }

        return if (type == 0) "其他" else null
    }
}

interface CategoryKeywordManagerTestInterface {
    fun getMerchantCategory(merchant: String, type: Int): String?
}
