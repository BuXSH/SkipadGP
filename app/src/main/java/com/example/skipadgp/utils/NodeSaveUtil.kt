package com.example.skipadgp.utils

import android.content.Context
import android.util.Log
import com.example.skipadgp.service.AccessibilityService.NodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NodeSaveUtil {
    fun saveNodeToJson(context: Context, node: NodeInfo): Boolean {
        Log.d("NodeSaveUtil", "开始保存节点信息")
        return try {
            val file = File(context.getExternalFilesDir(null), "nodes.json")
            // 创建新的节点 JSON 对象
            val nodeJson = JSONObject().apply {
                put("className", node.className)
                put("text", node.text ?: "无")
                put("contentDescription", node.contentDescription ?: "无")
                put("bounds", node.bounds.toString())
                put("isClickable", node.isClickable)
                put("depth", node.depth)
                put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }
            // 读取现有数据或创建新的 JSON 对象
            val patternsJson = if (file.exists()) {
                JSONObject(file.readText())
            } else {
                JSONObject()
            }
            // 获取或创建包名对应的节点数组
            val packageNodes = if (patternsJson.has(node.packageName)) {
                patternsJson.getJSONArray(node.packageName)
            } else {
                JSONArray().also { patternsJson.put(node.packageName, it) }
            }
            // 添加新节点数据到对应包名的数组中
            packageNodes.put(nodeJson)
            // 保存到文件，使用缩进格式化
            file.writeText(patternsJson.toString(2))
            
            Log.d("NodeSaveUtil", "节点信息已保存到文件: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("NodeSaveUtil", "保存节点信息失败", e)
            false
        }
    }
}