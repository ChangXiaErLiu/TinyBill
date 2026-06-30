package com.tinybill.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val merchant: String,
    val category: String,
    val type: Int,
    val useCount: Int = 0,
    val lastUsed: Long = 0
)