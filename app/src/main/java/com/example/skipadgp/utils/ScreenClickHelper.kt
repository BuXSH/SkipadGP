package com.example.skipadgp.utils

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.graphics.Point
import android.util.Log

/**
 * 屏幕点击辅助类
 * 提供模拟点击屏幕特定位置的功能
 *
 * 该类使用Android的无障碍服务API来模拟用户的点击操作
 * 通过GestureDescription来描述点击手势，实现精确的屏幕坐标点击
 *
 * @see AccessibilityService 使用无障碍服务来执行点击操作
 * @see GestureDescription 用于描述点击手势的具体参数
 * @see Point 用于指定点击的具体坐标位置
 */
object ScreenClickHelper {

    /** 日志标签，用于标识来自该类的日志信息 */
    private const val TAG = "ScreenClickHelper"
    
    /** 
     * 点击手势的持续时间（毫秒）
     * 设置较短的持续时间以模拟快速点击，提高响应速度
     */
    private const val GESTURE_DURATION = 100L

    /**
     * 在指定位置执行点击操作
     *
     * 该方法通过创建GestureDescription来模拟用户的点击操作
     * 使用Path来定义点击的精确位置，通过AccessibilityService来执行手势
     *
     * @param service AccessibilityService 无障碍服务实例，用于执行实际的点击操作
     * @param point Point 要点击的屏幕坐标，包含x和y坐标值
     * @return Boolean 点击操作是否成功执行
     *         - true: 点击手势已成功分发给系统
     *         - false: 点击操作失败（服务为空或发生异常）
     *
     * @throws Exception 可能在创建或执行手势时抛出异常
     */
    fun performClickAtPosition(service: AccessibilityService?, point: Point): Boolean {
        // 调试查看performClickAtPosition是否被调用
        // Log.d(TAG, "[${System.currentTimeMillis()}] performClickAtPosition被调用-service=${service?.javaClass?.simpleName}")
        // 检查服务实例是否有效
        if (service == null) {
            Log.w(TAG, "无法执行点击操作：服务实例为空")
            return false
        }

        try {
            // 创建点击路径，设置点击的精确坐标
            val clickPath = Path().apply {
                moveTo(point.x.toFloat(), point.y.toFloat())
            }
            //调试查看点击的精确坐标
            // Log.d(TAG, "点击的精确坐标：(${point.x}, ${point.y})")
            // 创建手势描述，配置点击参数
            val gestureBuilder = GestureDescription.Builder()
            val gestureStroke = GestureDescription.StrokeDescription(
                clickPath,
                0, // 开始时间：1000毫秒后开始点击，以防UI元素还未完全加载
                GESTURE_DURATION // 持续时间：快速点击
            )

            // 将点击手势添加到手势描述中
            gestureBuilder.addStroke(gestureStroke)
            //调试查看手势描述是否被正确创建
            // Log.d(TAG, "手势描述：${gestureBuilder.build()}")

            // 通过无障碍服务分发手势，执行实际的点击操作
            val result = service.dispatchGesture(
                gestureBuilder.build(),
                object : AccessibilityService.GestureResultCallback() {
                    // 手势执行完成的回调
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        Log.d(TAG, "点击手势执行完成：(${point.x}, ${point.y})")
                    }

                    // 手势被取消的回调
                    override fun onCancelled(gestureDescription: GestureDescription) {
                        Log.w(TAG, "点击手势被取消：(${point.x}, ${point.y})")
                    }
                },
                null // 不需要额外的处理器
            )

            return result

        } catch (e: Exception) {
            // 记录异常信息并返回失败状态
            Log.e(TAG, "执行点击操作时发生异常：${e.message}")
            return false
        }
    }
}