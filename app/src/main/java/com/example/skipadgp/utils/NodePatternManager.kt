package com.example.skipadgp.utils

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.skipadgp.model.NodePattern
import org.json.JSONObject
import java.io.File

object NodePatternManager {
    private const val PATTERN_FILE = "nodes.json"
    private var patterns: MutableMap<String, List<NodePattern>> = mutableMapOf()

    /**
     * 加载节点特征数据
     * 从外部存储中读取预先保存的节点特征JSON文件，并解析成内存中的数据结构
     * 
     * @param context Android上下文，用于获取外部存储目录
     */
    fun loadPatterns(context: Context) {
        // 在外部存储目录中定位节点特征文件
        val file = File(context.getExternalFilesDir(null), PATTERN_FILE)
        // 如果文件不存在，直接返回
        Log.d("NodePatternManager", "检查节点特征文件是否存在: ${file.absolutePath}")
        if (!file.exists()) {
            Log.d("NodePatternManager", "节点特征文件不存在，跳过加载")
            return
        }
        try {
            // 读取文件内容并解析为JSON对象，然后填充patterns
            val json = JSONObject(file.readText())
            Log.d("NodePatternManager", "成功读取JSON文件内容")
            
            // 遍历所有包名并解析节点数据
            json.keys().forEach { packageName ->
                val nodesArray = json.getJSONArray(packageName)
                Log.d("NodePatternManager", "处理包名: $packageName, 节点数量: ${nodesArray.length()}")
                
                val nodePatterns = mutableListOf<NodePattern>()
                for (i in 0 until nodesArray.length()) {
                    val nodeJson = nodesArray.getJSONObject(i)
                    val bounds = nodeJson.getString("bounds").let { boundsStr ->
                        // 处理 "Rect(232, 2185 - 616, 2304)" 格式的字符串
                        val numbers = boundsStr
                            .replace("Rect(", "")
                            .replace(")", "")
                            .replace(" - ", ", ")  // 将 " - " 替换为 ", "
                            .split(",")
                            .map { it.trim().toInt() }
                        android.graphics.Rect(numbers[0], numbers[1], numbers[2], numbers[3])
                    }
                    
                    val pattern = NodePattern(
                        packageName = packageName,
                        className = nodeJson.getString("className"),
                        text = nodeJson.getString("text").takeIf { it != "无" },
                        contentDescription = nodeJson.getString("contentDescription").takeIf { it != "无" },
                        bounds = bounds,
                        isClickable = nodeJson.getBoolean("isClickable"),
                        depth = nodeJson.getInt("depth")
                    )
                    nodePatterns.add(pattern)
                }
                patterns[packageName] = nodePatterns
                Log.d("NodePatternManager", "已加载 ${nodePatterns.size} 个节点模式到 $packageName")
            }
            Log.d("NodePatternManager", "节点特征加载完成，共 ${patterns.size} 个包名")
        } catch (e: Exception) {
            Log.e("NodePatternManager", "加载节点特征失败", e)
        }
    }

    /**
     * 在当前界面查找匹配的节点
     * @param rootNode 当前界面的根节点
     * @param packageName 要匹配的应用包名
     * @return 匹配成功返回对应的节点，否则返回null
     */
    fun findMatchingNode(rootNode: AccessibilityNodeInfo?, packageName: String): AccessibilityNodeInfo? {
        if (rootNode == null || !patterns.containsKey(packageName)) return null
        
        val packagePatterns = patterns[packageName] ?: return null
        return findNodeByPatterns(rootNode, packagePatterns)
    }

    /**
     * 递归查找匹配特征的节点
     * @param node 当前检查的节点
     * @param patterns 要匹配的节点特征列表
     * @return 找到匹配的节点则返回该节点，否则返回null
     */
    private fun findNodeByPatterns(node: AccessibilityNodeInfo?, patterns: List<NodePattern>): AccessibilityNodeInfo? {
        if (node == null) return null

        // 检查当前节点是否匹配任何模式
        for (pattern in patterns) {
            if (pattern.matches(node)) {
                return node
            }
        }

        // 递归检查子节点
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            val result = findNodeByPatterns(childNode, patterns)
            if (result != null) {
                return result
            }
            childNode?.recycle()  // 及时释放未匹配的节点
        }

        return null
    }
}