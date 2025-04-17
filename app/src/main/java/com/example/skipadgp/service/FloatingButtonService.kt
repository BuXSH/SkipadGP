package com.example.skipadgp.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.example.skipadgp.R
import android.content.Context
import android.util.Log
import com.example.skipadgp.service.AccessibilityService.NodeInfo  // 添加 NodeInfo 的导入
import android.content.BroadcastReceiver  // 添加 BroadcastReceiver 的导入
import android.content.IntentFilter  // 添加 IntentFilter 的导入
import android.os.IBinder
import android.os.Build
import android.widget.FrameLayout
import android.util.DisplayMetrics
import android.widget.TextView  // 添加 TextView 导入
import com.example.skipadgp.utils.NodeSaveUtil


/**
 * 悬浮窗服务
 * 用于在屏幕上显示一个可拖动的悬浮窗按钮
 */
class FloatingButtonService : Service() {
    // 添加类级别变量
    private lateinit var metrics: DisplayMetrics  // 用于存储真实屏幕尺寸
    private var currentSelectedNode: NodeInfo? = null  // 被选择的节点信息
    private var infoWindow: View? = null
    private var infoParams: WindowManager.LayoutParams? = null
    private var isVisual = false  // 是否可见的状态标记
    // 添加广播接收器用于接收界面节点信息
    private val nodeInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.skipadgp.NODE_INFO") {
                // 处理接收到的节点信息
                val nodeInfoList = intent.getParcelableArrayListExtra<NodeInfo>("nodes")
                Log.d("FloatingButton", "收到节点信息，数量：${nodeInfoList?.size ?: 0}")
                // 如果当前是可见状态，立即绘制节点轮廓
                if (isVisual && nodeInfoList != null) {
                    drawNodeOutlines(nodeInfoList)
                }
            }
        }
    }

    // 一悬浮窗变量名
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    // 二悬浮窗变量名
    private lateinit var secondFloatingView: View
    private lateinit var secondParams: WindowManager.LayoutParams
    
    // 记录触摸事件的初始位置
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()

        // 注册广播接收器，明确指定为不导出（仅接收应用内广播）
        // registerReceiver(
        //     nodeInfoReceiver, 
        //     IntentFilter("com.example.skipadgp.NODE_INFO"),
        //     Context.RECEIVER_NOT_EXPORTED
        // )

        val filter = IntentFilter("com.example.skipadgp.NODE_INFO")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 对于 Android 8.0 及以上版本，需要使用 Context.RECEIVER_NOT_EXPORTED
            registerReceiver(nodeInfoReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // 对于 Android 8.0 以下版本，不需要使用 Context.RECEIVER_NOT_EXPORTED
            registerReceiver(nodeInfoReceiver, filter)
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 获取屏幕尺寸(用来计算一浮窗位置)
        val screenHeight = resources.displayMetrics.heightPixels
        val screenWidth = resources.displayMetrics.widthPixels
        
        // 获取真实屏幕尺寸
        metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        // 一悬浮窗初始化
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM  // 改为BOTTOM，这样y值就是距离底部的距离
            x = (screenWidth * 0.07).toInt()  // 距离左边缘的距离
            y = (screenHeight * 0.05).toInt()  // 距离底部15%的距离
        }

        // 二悬浮窗初始化
        secondFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_second_view, null)
        secondParams = WindowManager.LayoutParams(
            metrics.widthPixels,  // 使用真实宽度
            metrics.heightPixels, // 使用真实高度
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP  // 改为BOTTOM，这样y值就是距离底部的距离
            x = 0
            y = 0
        }

        // 设置拖动事件
        setupDragListener()
        // 设置按钮点击事件
        setupButtons() 
        // 添加第二个悬浮窗
        windowManager.addView(secondFloatingView, secondParams)
        // 添加悬浮窗到屏幕
        windowManager.addView(floatingView, params)
    }

    private fun setupDragListener() {
        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // 由于 Gravity.BOTTOM，需要反转偏移量来修正拖动方向
                    params.y = initialY - (event.rawY - initialTouchY).toInt()
                    // 更新悬浮窗位置
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    // 悬浮按钮点击事件
    private fun setupButtons() {
        val layoutButton = floatingView.findViewById<ImageView>(R.id.layout_button)
        val saveButton = floatingView.findViewById<ImageView>(R.id.save_button)
        // 设置初始图标为关闭状态
        layoutButton.setImageResource(R.drawable.ic_close)
        layoutButton.setOnClickListener {
            isVisual = !isVisual  // 切换可见状态
            layoutButton.setImageResource(if (isVisual) R.drawable.ic_open else R.drawable.ic_close)
            // 修改悬浮窗属性
            if (isVisual) {
                // 切换到可见状态时，发送广播请求节点信息
                sendBroadcast(Intent("com.example.skipadgp.REQUEST_NODE_INFO"))
                // 保持全屏显示的标志，只移除 FLAG_NOT_TOUCHABLE
                secondParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                // 设置10%的不透明度
                secondFloatingView.setBackgroundColor(0x66000000.toInt())
            } else {
                // 保持全屏显示的标志，添加 FLAG_NOT_TOUCHABLE
                secondParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                // 设置完全透明
                secondFloatingView.setBackgroundColor(0x00000000.toInt())
                clearNodeOutlines()// 清除节点轮廓
            }
            // 更新悬浮窗属性
            windowManager.updateViewLayout(secondFloatingView, secondParams)
            Log.d("FloatingButton", "布局界面当前状态：${if (isVisual) "可见" else "不可见"}") 
        }
        saveButton.setOnClickListener { // 保存按钮的点击事件
            Log.d("FloatingButton", "保存按钮被点击")
            if (currentSelectedNode != null) {
                // 添加详细的日志，帮助调试
                Log.d("FloatingButton", """
                    准备保存节点信息:
                    包名: ${currentSelectedNode!!.packageName}
                    类名: ${currentSelectedNode!!.className}
                    文本: ${currentSelectedNode!!.text}
                    位置: ${currentSelectedNode!!.bounds}
                """.trimIndent())
                // 保存节点信息到文件
                if (NodeSaveUtil.saveNodeToJson(this, currentSelectedNode!!)) {
                    showNodeInfo(currentSelectedNode!!)
                    Log.d("FloatingButton", "节点信息保存成功")
                } else {
                    showTipInfo("保存失败")
                }
            } else {
                showTipInfo("请先选择一个节点")
            }
        }
    }

    // 添加节点绘制方法
    private fun drawNodeOutlines(nodeInfoList: List<NodeInfo>) {
        clearNodeOutlines()  // 先清除之前的节点轮廓
        val overlayContainer = secondFloatingView.findViewById<FrameLayout>(R.id.overlay_container)
        Log.d("drawNodeOutlines", "开始绘制节点轮廓，数量：${nodeInfoList.size}")
        Log.d("drawNodeOutlines", "屏幕真实尺寸: ${metrics.widthPixels}x${metrics.heightPixels}")
        // 按节点面积排序，面积大的在前面绘制（这样会显示在上层）
        val sortedNodes = nodeInfoList.sortedByDescending { node -> 
            node.bounds.width() * node.bounds.height()  // 计算节点面积
        }
        // 遍历节点列表，为每个节点创建轮廓
        sortedNodes.forEach { node ->
            // 创建布局参数
            val params = FrameLayout.LayoutParams(
                node.bounds.width(),
                node.bounds.height()
            ).apply {
                leftMargin = node.bounds.left
                topMargin = node.bounds.top
            }
            // 创建节点轮廓视图
            val outlineView = ImageView(this).apply {
                setBackgroundResource(R.drawable.node)
                isFocusableInTouchMode = true
                // 设置Z轴顺序，使用负的面积值，这样面积越小的节点Z轴越大
                elevation = 1000f - (node.bounds.width() * node.bounds.height()).toFloat() / 10000f
                // 设置点击事件
                setOnClickListener { 
                    it.requestFocus()
                }
               
                // 设置焦点变化监听
                onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                    view.setBackgroundResource(
                        if (hasFocus) R.drawable.node_focus 
                        else R.drawable.node
                    )
                    // 在获得焦点时输出节点信息
                    if (hasFocus) {
                        currentSelectedNode = node
                        Log.d("drawNodeOutlines", """
                            节点信息:
                            包名: ${node.packageName}
                            位置: ${node.bounds}
                            类名: ${node.className}
                            文本: ${node.text ?: "无"}
                            描述: ${node.contentDescription ?: "无"}
                            可点击: ${node.isClickable}
                            深度: ${node.depth}
                        """.trimIndent())
                    }
                }
            }
            // 添加到容器中
            overlayContainer.addView(outlineView, params)
        }
    }

    // 添加清除节点轮廓方法
    private fun clearNodeOutlines() {
        val overlayContainer = secondFloatingView.findViewById<FrameLayout>(R.id.overlay_container)
        overlayContainer.removeAllViews()
        // 清空当前选中的节点信息
        currentSelectedNode = null
        Log.d("clearNodeOutlines", "已清除所有节点轮廓和选中节点")
    }

    // 应用信息提示窗口
    private fun showNodeInfo(node: NodeInfo) {
        // 如果已经有信息窗口，先移除
        infoWindow?.let { windowManager.removeView(it) }

        // 创建信息窗口
        infoWindow = LayoutInflater.from(this).inflate(R.layout.layout_node_info, null)
        
        // 设置内容
        infoWindow?.findViewById<TextView>(R.id.info_text)?.text = """
            添加成功
            节点信息:
            包名: ${node.packageName}
            位置: ${node.bounds}
            类名: ${node.className}
            文本: ${node.text ?: "无"}
            描述: ${node.contentDescription ?: "无"}
            可点击: ${node.isClickable}
        """.trimIndent()

        // 创建窗口参数
        infoParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,  // 添加此标志
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            // 设置窗口层级为最高
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            // 设置窗口在最上层
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        // 显示信息窗口
        windowManager.addView(infoWindow, infoParams)

        // 3秒后自动移除
        infoWindow?.postDelayed({
            infoWindow?.let {
                windowManager.removeView(it)
                infoWindow = null
            }
        }, 3000)
    }

    // 显示提示信息窗口
    private fun showTipInfo(message: String) {
        // 如果已经有信息窗口，先移除
        infoWindow?.let { windowManager.removeView(it) }

        // 创建信息窗口
        infoWindow = LayoutInflater.from(this).inflate(R.layout.layout_node_info, null)
        
        // 设置提示内容
        infoWindow?.findViewById<TextView>(R.id.info_text)?.text = message

        // 创建窗口参数
        infoParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        // 显示信息窗口
        windowManager.addView(infoWindow, infoParams)

        // 2秒后自动移除（提示信息显示时间稍短）
        infoWindow?.postDelayed({
            infoWindow?.let {
                windowManager.removeView(it)
                infoWindow = null
            }
        }, 2000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 返回 START_STICKY 表示服务被系统杀死后会尝试重新创建
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 注销广播接收器
        unregisterReceiver(nodeInfoReceiver)
        // 移除悬浮窗
        if (::floatingView.isInitialized && ::windowManager.isInitialized) {
            windowManager.removeView(floatingView)
        }
        if (::secondFloatingView.isInitialized) {
            windowManager.removeView(secondFloatingView)
        }
    }

    // 添加一个公共方法用于外部关闭服务
    companion object {
        fun stopService(context: Context) {
            context.stopService(Intent(context, FloatingButtonService::class.java))
        }
    }
}
