package com.tinybill.service.parser

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.tinybill.data.stats.ParserStatsStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 解析失败时把当前页面的 TextNode 树 + 关键属性 dump 到本地文件，
 * 用于微信/支付宝改版后离线分析为何解析失败。
 *
 * 写入位置：`context.filesDir/snapshots/`，无需额外权限。
 * 自动轮转：保留最近 [MAX_SNAPSHOTS] 个文件，更早的自动删除。
 */
class SnapshotWriter(context: Context) {

    private val appContext = context.applicationContext
    private val statsStore = ParserStatsStore.getInstance(appContext)
    private val snapshotDir: File = File(appContext.filesDir, SNAPSHOT_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    /**
     * 写入一次失败快照。文件名为 `yyyyMMdd-HHmmss-{package}.txt`。
     * @return 写入的文件绝对路径，失败返回 null。
     */
    fun writeFailureSnapshot(
        packageName: String,
        rootNode: AccessibilityNodeInfo,
        reason: String,
        extractedTexts: List<String>,
    ): String? {
        return try {
            val timestamp = fileNameFormat.format(Date())
            val safePackage = packageName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val file = File(snapshotDir, "$timestamp-$safePackage.txt")

            val content = buildString {
                appendLine("# TinyBill Parse Failure Snapshot")
                appendLine("# 时间: ${Date()}")
                appendLine("# 包名: $packageName")
                appendLine("# 原因: $reason")
                appendLine()
                appendLine("## 提取到的文本节点 (${extractedTexts.size})")
                extractedTexts.take(MAX_TEXTS_IN_SNAPSHOT).forEachIndexed { i, t ->
                    appendLine("[${i.toString().padStart(3, '0')}] $t")
                }
                if (extractedTexts.size > MAX_TEXTS_IN_SNAPSHOT) {
                    appendLine("... (还有 ${extractedTexts.size - MAX_TEXTS_IN_SNAPSHOT} 条已截断)")
                }
                appendLine()
                appendLine("## 完整 TextNode 树 (前 ${MAX_NODES_IN_SNAPSHOT} 个节点)")
                appendNodeTree(rootNode, depth = 0)
            }

            file.writeText(content, Charsets.UTF_8)
            rotate()
            statsStore.recordSnapshot()
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to write snapshot", e)
            null
        }
    }

    /** 当前已存快照文件列表（按文件名倒序，最新在前）。 */
    fun listSnapshots(): List<File> =
        snapshotDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    /** 清空所有快照。 */
    fun clearSnapshots(): Int {
        val files = listSnapshots()
        var removed = 0
        files.forEach { if (it.delete()) removed++ }
        return removed
    }

    private fun rotate() {
        val files = snapshotDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.name }
            ?: return
        if (files.size > MAX_SNAPSHOTS) {
            files.drop(MAX_SNAPSHOTS).forEach { it.delete() }
        }
    }

    private fun StringBuilder.appendNodeTree(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int = 6) {
        if (node == null || depth > maxDepth) return
        val indent = "  ".repeat(depth)
        val className = node.className?.toString()?.substringAfterLast('.') ?: "?"
        val text = node.text?.toString()?.take(80) ?: ""
        val desc = node.contentDescription?.toString()?.take(80) ?: ""
        val resourceId = node.viewIdResourceName?.substringAfterLast('/') ?: ""
        val line = buildString {
            append(indent).append(className)
            if (text.isNotEmpty()) append(" text=\"").append(text).append('"')
            if (desc.isNotEmpty()) append(" desc=\"").append(desc).append('"')
            if (resourceId.isNotEmpty()) append(" id=$resourceId")
        }
        appendLine(line)
        for (i in 0 until node.childCount) {
            appendNodeTree(node.getChild(i), depth + 1, maxDepth)
        }
    }

    companion object {
        private const val TAG = "SnapshotWriter"
        private const val SNAPSHOT_DIR_NAME = "snapshots"
        private const val MAX_SNAPSHOTS = 20
        private const val MAX_TEXTS_IN_SNAPSHOT = 100
        private const val MAX_NODES_IN_SNAPSHOT = 200
    }
}
