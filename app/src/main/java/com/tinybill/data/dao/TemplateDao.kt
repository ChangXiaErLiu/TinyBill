package com.tinybill.data.dao

import androidx.room.*
import com.tinybill.data.entity.Template
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM template ORDER BY useCount DESC, lastUsed DESC")
    fun getAllTemplates(): Flow<List<Template>>
    
    @Query("SELECT * FROM template ORDER BY useCount DESC, lastUsed DESC LIMIT :limit")
    suspend fun getTopTemplates(limit: Int): List<Template>
    
    @Query("SELECT * FROM template WHERE name LIKE '%' || :keyword || '%' OR merchant LIKE '%' || :keyword || '%'")
    suspend fun searchTemplates(keyword: String): List<Template>
    
    @Insert
    suspend fun insert(template: Template): Long
    
    @Update
    suspend fun update(template: Template)
    
    @Delete
    suspend fun delete(template: Template)
    
    @Query("UPDATE template SET useCount = useCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUseCount(id: Long, timestamp: Long)
}