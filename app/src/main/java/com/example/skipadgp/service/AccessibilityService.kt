package com.example.skipadgp.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Rect
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.accessibility.AccessibilityNodeInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import com.example.skipadgp.utils.WhitelistManager
import com.example.skipadgp.utils.FindSkipButton
import com.example.skipadgp.utils.ScreenClickHelper
import com.example.skipadgp.utils.NodePatternManager 

/**
 * 无障碍服务类，用于监听和处理系统的无障碍事件
 * 通过继承AccessibilityService实现自定义的无障碍服务功能
 */
class AccessibilityService : AccessibilityService() {
    private var lastProcessTime = 0L
    private var appStartTime = 0L
    private var isAppStarting = false
    private var currentPackage: String? = null
    private val STARTUP_MONITOR_DURATION = 5000L  // 启动监听持续5秒
    private val PROCESS_INTERVAL = 200L           // 事件处理间隔200毫秒
    /**
     * 当服务连接成功时调用
     * 在这里配置无障碍服务的相关参数，如事件类型、反馈类型等
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        // 加载节点特征数据
        Log.d("AccessibilityService", "开始加载节点特征数据...")
        NodePatternManager.loadPatterns(this)
        Log.d("AccessibilityService", "节点特征数据加载完成")
        val info = AccessibilityServiceInfo().apply {
            // 监听所有类型的无障碍事件
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            // 允许监听所有应用的窗口事件（关键）
            packageNames = null // null 表示不限制包名
            // 设置反馈类型为通用反馈
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            // 设置标志位，用于报告视图ID
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            // 设置通知超时时间为100毫秒
            notificationTimeout = 100
        }
        serviceInfo = info

        // 注册广播接收器
        val filter = IntentFilter("com.example.skipadgp.REQUEST_NODE_INFO")
        registerReceiver(nodeInfoRequestReceiver, filter)
    }

    // 添加广播接收器
    private val nodeInfoRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.skipadgp.REQUEST_NODE_INFO") {
                // 收到请求时才获取节点信息
                getAllNodes()
            }
        }
    }

    /**
     * 处理无障碍事件
     * @param event AccessibilityEvent 接收到的无障碍事件
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = event.packageName?.toString()
                    // 在应用切换时进行白名单检查
                    if (packageName != null && 
                        packageName != currentPackage && 
                        !WhitelistManager.getInstance(this).isPackageWhitelisted(packageName)) {
                        isAppStarting = true
                        appStartTime = System.currentTimeMillis()
                        currentPackage = packageName
                        Log.d("WindowEvent", "检测到新应用启动: $packageName")
                    }
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    val currentTime = System.currentTimeMillis()
                    val packageName = event.packageName?.toString()
                    
                    if (isAppStarting && 
                        packageName != null &&
                        currentTime - appStartTime < STARTUP_MONITOR_DURATION &&
                        currentTime - lastProcessTime > PROCESS_INTERVAL) {
                        
                        lastProcessTime = currentTime
                        rootInActiveWindow?.let { rootNode ->
                            try {
                                FindSkipButton.getSkipButtonAndPoint(rootNode, this)?.let { (skipButton, point) ->
                                    Log.d("WindowEvent", "在 $packageName 中找到跳过按钮")
                                    val clickResult = ScreenClickHelper.performClickAtPosition(this, point)
                                    if (clickResult) {
                                        isAppStarting = false
                                        currentPackage = null
                                    }
                                }
                            } catch (e: StackOverflowError) {
                                Log.e("WindowEvent", "处理节点时发生栈溢出错误")
                                isAppStarting = false
                                currentPackage = null
                            }
                        }
                    }
                    
                    if (currentTime - appStartTime > STARTUP_MONITOR_DURATION) {
                        isAppStarting = false
                        currentPackage = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WindowEvent", "事件处理发生错误: ${e.message}")
            isAppStarting = false
            currentPackage = null
        }
    }

    /**
     * 当服务中断时调用
     * 用于处理服务中断时的清理工作
     */
    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销广播接收器
        unregisterReceiver(nodeInfoRequestReceiver)
    }

    // 节点信息数据类
    data class NodeInfo(
        val packageName: String,    // 应用包名
        val bounds: Rect,           // 节点在屏幕上的位置
        val className: String,      // 节点类名
        val text: String?,          // 节点文本内容
        val contentDescription: String?, // 节点描述文本
        val isClickable: Boolean,   // 是否可点击
        val depth: Int             // 节点深度
    ): Parcelable {                 // 实现Parcelable接口，用于在Intent中传递数据
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",  // packageName
            Rect().apply { parcel.readParcelable<Rect>(Rect::class.java.classLoader)?.let { set(it) } },
            parcel.readString() ?: "",  // className  
            parcel.readString(),       // text
            parcel.readString(),       // contentDescription
            parcel.readByte() != 0.toByte(),    // isClickable
            parcel.readInt()           // depth
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(packageName)
            parcel.writeParcelable(bounds, flags)
            parcel.writeString(className)
            parcel.writeString(text)
            parcel.writeString(contentDescription)
            parcel.writeByte(if (isClickable) 1 else 0)
            parcel.writeInt(depth)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<NodeInfo> {
            override fun createFromParcel(parcel: Parcel): NodeInfo {
                return NodeInfo(parcel)
            }

            override fun newArray(size: Int): Array<NodeInfo?> {
                return arrayOfNulls(size)
            }
        }
    }

    /**
     * 获取当前界面的所有节点信息
     * @return List<NodeInfo> 节点信息列表
     */
    private fun getAllNodes(): List<NodeInfo> {
        val nodes = mutableListOf<NodeInfo>()
        val rootNode = rootInActiveWindow ?: return nodes
        Log.d("NodeInfo", "开始获取节点信息...")
        // 递归遍历所有节点
        fun traverseNodes(node: AccessibilityNodeInfo?, depth: Int) {
            node?.let {
                // 获取节点边界
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                // 创建节点信息对象
                val nodeInfo = NodeInfo(
                    packageName = node.packageName?.toString() ?: "",
                    bounds = bounds,
                    className = node.className?.toString() ?: "",
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    isClickable = node.isClickable,
                    depth = depth
                )

                // 添加调试日志
                Log.d("NodeInfo", """
                    应用包名: ${nodeInfo.packageName}
                    节点深度: ${nodeInfo.depth}
                    类名: ${nodeInfo.className}
                    文本: ${nodeInfo.text ?: "无"}
                    描述: ${nodeInfo.contentDescription ?: "无"}
                    位置: ${nodeInfo.bounds}
                    可点击: ${nodeInfo.isClickable}
                    ----------------------
                """.trimIndent())

                // 添加节点信息到列表
                nodes.add(nodeInfo)

                // 递归遍历子节点
                for (i in 0 until node.childCount) {
                    traverseNodes(node.getChild(i), depth + 1)
                }
            }
        }

        // 开始遍历
        traverseNodes(rootNode, 0)
        Log.d("NodeInfo", "节点获取完成，共找到 ${nodes.size} 个节点")

        // 发送广播
        val intent = Intent("com.example.skipadgp.NODE_INFO")
        intent.putParcelableArrayListExtra("nodes", ArrayList(nodes))
        sendBroadcast(intent)
        return nodes
    }
}