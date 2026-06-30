package com.tinybill.util

import kotlin.math.ceil
import kotlin.math.roundToLong

data class AASplitResult(
    val totalAmount: Double,
    val numberOfPeople: Int,
    val perPersonAmount: Double,
    val remainder: Double,
    val splits: List<PersonSplit>
)

data class PersonSplit(
    val personName: String,
    val amount: Double,
    val isPayer: Boolean = false
)

object AACalculator {

    private fun roundToTwoDecimals(value: Double): Double {
        return (value * 100).roundToLong() / 100.0
    }

    fun calculateSplit(
        totalAmount: Double,
        numberOfPeople: Int,
        excludeList: List<Int> = emptyList(),
        payerIndex: Int = -1
    ): AASplitResult {
        if (numberOfPeople <= 0) {
            return AASplitResult(totalAmount, 0, 0.0, totalAmount, emptyList())
        }

        val actualPeople = numberOfPeople - excludeList.size
        if (actualPeople <= 0) {
            return AASplitResult(totalAmount, numberOfPeople, 0.0, totalAmount, emptyList())
        }

        val perPerson = roundToTwoDecimals(totalAmount / actualPeople)
        val totalDistributed = perPerson * actualPeople
        val remainder = roundToTwoDecimals(totalAmount - totalDistributed)

        val splits = mutableListOf<PersonSplit>()
        var index = 0
        for (i in 0 until numberOfPeople) {
            if (excludeList.contains(i)) {
                continue
            }
            splits.add(PersonSplit(
                personName = "第${index + 1}人",
                amount = perPerson,
                isPayer = i == payerIndex
            ))
            index++
        }

        return AASplitResult(
            totalAmount = totalAmount,
            numberOfPeople = actualPeople,
            perPersonAmount = perPerson,
            remainder = remainder,
            splits = splits
        )
    }

    fun calculateWithDiscount(
        totalAmount: Double,
        discountAmount: Double,
        numberOfPeople: Int,
        excludeList: List<Int> = emptyList()
    ): AASplitResult {
        val afterDiscount = roundToTwoDecimals(totalAmount - discountAmount)
        return calculateSplit(afterDiscount, numberOfPeople, excludeList)
    }

    fun calculateWithTips(
        totalAmount: Double,
        numberOfPeople: Int,
        tipPercentage: Float,
        excludeList: List<Int> = emptyList()
    ): AASplitResult {
        val tipAmount = totalAmount * tipPercentage
        val totalWithTip = roundToTwoDecimals(totalAmount + tipAmount)
        return calculateSplit(totalWithTip, numberOfPeople, excludeList)
    }

    fun roundUpPerPerson(perPerson: Double, roundTo: Double = 0.1): Double {
        return roundToTwoDecimals(ceil(perPerson / roundTo) * roundTo)
    }
}