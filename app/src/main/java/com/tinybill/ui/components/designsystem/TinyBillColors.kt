package com.tinybill.ui.components.designsystem

import androidx.compose.ui.graphics.Color
import com.tinybill.ui.theme.EntertainmentColor
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.FoodColor
import com.tinybill.ui.theme.LivingColor
import com.tinybill.ui.theme.MedicalColor
import com.tinybill.ui.theme.OtherColor
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.ui.theme.PrimaryGreenDark
import com.tinybill.ui.theme.PrimaryGreenLight
import com.tinybill.ui.theme.ShoppingColor
import com.tinybill.ui.theme.TransportColor
import com.tinybill.ui.theme.WarningColor

/**
 * 统一颜色系统（design system 入口）。
 *
 * 整合记录（2026-06）：
 *  - 旧版 `designsystem/TinyBillColors.kt` 复制了 `theme/Color.kt` 的颜色 + 自定义了一份 Tailwind 调色板，
 *    导致两套色卡并存。重构后这里只保留对 `theme/Color.kt` 的引用，
 *    **真实颜色值只在 `theme/Color.kt` 维护一份**。
 *  - 历史只被使用过的字段：`Primary / Expense / Income / Warning / CategoryFood / CategoryShopping
 *    / CategoryTransport / CategoryEntertainment / CategoryHousing / CategoryUtilities / CategoryOther`。
 *    其余字段（Surface / Background / Text*）即使原定义存在也没有调用方，删除以避免再次漂移。
 *  - 命名风格对照：
 *      Primary        ≡ PrimaryGreen
 *      Expense        ≡ ErrorColor
 *      Income         ≡ PrimaryGreen（语义复用主色）
 *      Warning        ≡ WarningColor
 *      CategoryHousing ≡ LivingColor
 *      CategoryUtilities ≡ WarningColor
 */
object TinyBillColors {
    // -------- Primary --------
    val Primary: Color get() = PrimaryGreen
    val PrimaryLight: Color get() = PrimaryGreenLight
    val PrimaryDark: Color get() = PrimaryGreenDark

    // -------- Semantic --------
    val Expense: Color get() = ErrorColor
    val Income: Color get() = PrimaryGreen
    val Warning: Color get() = WarningColor

    /** 预算进度色：根据百分比自动返回红/黄/绿 */
    fun budgetStatusColor(percentage: Float): Color = when {
        percentage >= 100f -> Expense
        percentage >= 80f -> Warning
        else -> Primary
    }

    // -------- Category --------
    val CategoryFood: Color get() = FoodColor
    val CategoryShopping: Color get() = ShoppingColor
    val CategoryTransport: Color get() = TransportColor
    val CategoryEntertainment: Color get() = EntertainmentColor
    val CategoryHousing: Color get() = LivingColor
    val CategoryUtilities: Color get() = WarningColor
    val CategoryMedical: Color get() = MedicalColor
    val CategoryOther: Color get() = OtherColor
}
