package com.example.skipadgp.utils

// 导入所需的Android系统类和自定义工具类
import android.graphics.Rect                               // 用于存储矩形边界信息
import android.util.Log                                    // 用于日志输出
import android.view.accessibility.AccessibilityNodeInfo    // 无障碍节点信息类
import java.util.regex.Pattern                            // 正则表达式模式类
import android.content.Context                            // Android上下文类

/**
 * 跳过按钮查找工具类
 * 该类提供了在无障碍服务中查找和识别广告跳过按钮的功能。
 */
object FindSkipButton {
    // 定义正则表达式模式，用于匹配各种跳过按钮的文本格式
    val skipPattern: Pattern = Pattern.compile(
        // 匹配以下格式：跳过、跳过广告、数字跳过、关闭广告、倒计时（如5s、5秒）
        "^(跳过(广告|\\d)?|\\d+跳过|关闭广告|\\d+s|\\d+秒).*$",
        // 设置不区分大小写
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    /**
     * 查找跳过按钮节点的主要方法
     */
    private fun findSkipButton(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        // 空节点检查，防止空指针异常
        if (rootNode == null) return null
        try {
            // 提取节点的文本内容，如果为空则使用空字符串
            val nodeText = rootNode.text?.toString() ?: ""
            // 添加调试日志
            if (nodeText.isNotEmpty()) {
                Log.d("SkipButton", "正在匹配文本: '$nodeText'")
                Log.d("SkipButton", "正则匹配结果: ${skipPattern.matcher(nodeText).find()}")
            }
            // 使用正则表达式检查文本是否匹配跳过按钮模式
            if (skipPattern.matcher(nodeText).find()) {
                // 创建矩形对象存储节点在屏幕上的位置
                val bounds = Rect()
                // 获取节点在屏幕上的边界位置
                rootNode.getBoundsInScreen(bounds)
                // 输出详细的匹配节点信息到日志
                Log.d("WindowEvent", "原生视图中找到匹配的跳过按钮:\n" +
                        "文本内容: ${nodeText}\n" +           // 节点的文本内容
                        "类名: ${rootNode.className}\n" +     // 节点的类名
                        "包名: ${rootNode.packageName}\n" +   // 节点所属的应用包名
                        "窗口ID: ${rootNode.windowId}\n" +    // 节点所在的窗口ID
                        "是否可点击: ${rootNode.isClickable}\n" + // 节点是否可以点击
                        "是否可见: ${rootNode.isVisibleToUser}\n" + // 节点是否对用户可见
                        "位置: ${bounds}")                    // 节点在屏幕上的位置
                return rootNode
            }
            // 使用循环遍历所有子节点
            for (i in 0 until rootNode.childCount) {
                // 获取每个子节点
                val childNode = rootNode.getChild(i)
                // 递归调用本方法检查子节点
                val result = findSkipButton(childNode)
                // 如果找到匹配的节点就返回
                if (result != null) {
                    return result
                }
            }
        } catch (e: Exception) {
            // 捕获并记录可能发生的异常
            Log.e("WindowEvent", "查找按钮时发生错误: ${e.message}")
            Log.e("SkipButton", "匹配过程出错: ${e.message}")
        }
        // 如果没有找到匹配的节点，返回null
        return null
    }

    /**
     * 获取跳过按钮节点及其中心点坐标
     */
    fun getSkipButtonAndPoint(rootNode: AccessibilityNodeInfo?, context: Context): Pair<AccessibilityNodeInfo, android.graphics.Point>? {
        // 先尝试特定应用的模式匹配
        val skipButton = rootNode?.packageName?.toString()?.let { pkg -> 
            // 首先尝试使用NodePatternManager根据应用包名查找已保存的特定模式节点
            NodePatternManager.findMatchingNode(rootNode, pkg)
        } ?: findSkipButton(rootNode) ?: return null    // 如果特定模式匹配失败，则使用通用文本匹配方式查找，都失败则返回null

        val rect = Rect()
        skipButton.getBoundsInScreen(rect)
        return Pair(skipButton, android.graphics.Point(rect.centerX(), rect.centerY()))
    }
}