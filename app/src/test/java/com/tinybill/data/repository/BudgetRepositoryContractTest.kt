package com.tinybill.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BudgetRepositoryContractTest {

    // Pure-Kotlin 测试，不依赖 Android Context/SharedPreferences，只验证数据契约

    @Test
    fun `CategoryBudget data class equality uses all fields`() {
        val a = BudgetRepository.CategoryBudget(category = "餐饮", budget = 500.0)
        val b = BudgetRepository.CategoryBudget(category = "餐饮", budget = 500.0)
        val c = BudgetRepository.CategoryBudget(category = "交通", budget = 500.0)
        val d = BudgetRepository.CategoryBudget(category = "餐饮", budget = 600.0)

        assertThat(a).isEqualTo(b)
        assertThat(a).isNotEqualTo(c) // 分类不同
        assertThat(a).isNotEqualTo(d) // 金额不同
    }

    @Test
    fun `BudgetSettings data class bundles config`() {
        val settings = BudgetRepository.BudgetSettings(
            enabled = true,
            monthlyBudget = 3000.0,
            mode = BudgetRepository.MODE_CATEGORY_SUM,
        )

        assertThat(settings.enabled).isTrue()
        assertThat(settings.monthlyBudget).isEqualTo(3000.0)
        assertThat(settings.mode).isEqualTo(BudgetRepository.MODE_CATEGORY_SUM)
    }

    @Test
    fun `budget mode constants are stable`() {
        // 兼容历史代码：0 = 固定总预算 / 1 = 分类预算合计。
        // 2026-06 旧 BudgetManager 已删除，但这两个常量值仍是配置 schema 的一部分，
        // 改值会导致用户已设置的预算模式被解读为另一种。
        assertThat(BudgetRepository.MODE_FIXED).isEqualTo(0)
        assertThat(BudgetRepository.MODE_CATEGORY_SUM).isEqualTo(1)
    }
}
