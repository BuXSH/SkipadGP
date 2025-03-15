package com.example.skipadgp.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.skipadgp.service.AccessibilityService

/**
 * 无障碍服务管理类
 * 负责管理应用的无障碍服务状态检查和查询功能
 */
class AccessibilityServiceManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AccessibilityServiceManager? = null

        fun getInstance(context: Context): AccessibilityServiceManager {
            return instance ?: synchronized(this) {
                instance ?: AccessibilityServiceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 检查无障碍服务是否已启用
     * @return Boolean 返回无障碍服务的启用状态
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        // 构建完整的组件名称，格式为：包名/服务类名
        val componentName = context.packageName + "/" + AccessibilityService::class.java.canonicalName
        // 获取系统中已启用的无障碍服务列表
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        // 检查我们的服务是否在已启用列表中
        return enabledServicesSetting?.contains(componentName) == true
    }

    /**
     * 查询无障碍服务状态
     * 向AccessibilityService发送查询请求
     */
    fun queryServiceState() {
        val intent = Intent(context, AccessibilityService::class.java).apply {
            action = "com.example.skipadgp.QUERY_SERVICE_STATE"
        }
        context.startService(intent)
    }
}