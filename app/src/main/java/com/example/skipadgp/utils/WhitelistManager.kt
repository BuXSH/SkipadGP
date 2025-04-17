package com.example.skipadgp.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

/**
 * 白名单管理工具类，用于管理不需要被无障碍服务监听的应用
 * 使用单例模式和SharedPreferences持久化存储数据
 */
class WhitelistManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val sharedPreferences: SharedPreferences = appContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    // 添加内存缓存
    private var whitelistCache: Set<String>? = null

    companion object {
        private const val PREF_NAME = "whitelist_prefs"
        private const val KEY_WHITELIST = "whitelist_packages"
        
        @Volatile
        private var instance: WhitelistManager? = null

        /**
         * 获取WhitelistManager单例实例
         * @param context 应用程序上下文
         */
        fun getInstance(context: Context): WhitelistManager {
            return instance ?: synchronized(this) {
                instance ?: WhitelistManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 获取白名单中的所有包名
     * @return 包名集合，为空时返回空集合
     */
    fun getWhitelistedPackages(): Set<String> {
        // 优先使用缓存
        whitelistCache?.let { return it }
        val whitelist = sharedPreferences.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
        // 更新缓存
        whitelistCache = whitelist.toSet()
        // 添加调试日志，输出当前白名单内容
        Log.d("WhitelistManager", "当前白名单列表:")
        whitelist.forEach { packageName ->
            Log.d("WhitelistManager", "- ${getAppName(packageName)} ($packageName)")
        }
        return whitelist
    }

    /**
     * 添加包名到白名单
     * @return 添加成功返回true，包名已存在或保存失败返回false
     */
    fun addToWhitelist(packageName: String): Boolean {
        val currentSet = getWhitelistedPackages().toMutableSet()
        if (currentSet.add(packageName)) {
            val result = sharedPreferences.edit()
                .putStringSet(KEY_WHITELIST, currentSet)
                .commit()
            if (result) {
                // 更新缓存
                whitelistCache = currentSet
            }
            return result
        }
        return false
    }

    /**
     * 从白名单中移除包名
     * @return 移除成功返回true，包名不存在或保存失败返回false
     */
    fun removeFromWhitelist(packageName: String): Boolean {
        val currentSet = getWhitelistedPackages().toMutableSet()
        if (currentSet.remove(packageName)) {
            val result = sharedPreferences.edit()
                .putStringSet(KEY_WHITELIST, currentSet)
                .commit()
            if (result) {
                // 更新缓存
                whitelistCache = currentSet
            }
            return result
        }
        return false
    }

    /**
     * 检查包名是否在白名单中
     */
    fun isPackageWhitelisted(packageName: String): Boolean {
        return getWhitelistedPackages().contains(packageName)
    }

    /**
     * 清空白名单
     * @return 清空成功返回true，失败返回false
     */
    fun clearWhitelist(): Boolean {
        val result = sharedPreferences.edit()
            .remove(KEY_WHITELIST)
            .commit()
        if (result) {
            // 清空缓存
            whitelistCache = emptySet()
        }
        return result
    }

    /**
     * 获取所有已安装的非系统应用列表，按名称排序
     */
    fun getInstalledUserApps(): List<ApplicationInfo> {
        val packageManager = appContext.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0 &&
                !isExcludedPackage(appInfo.packageName) &&
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }

    /**
     * 获取应用名称，获取失败时返回包名
     */
    fun getAppName(packageName: String): String {
        return try {
            val appInfo = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * 获取应用图标，获取失败时返回null
     */
    fun getAppIcon(packageName: String): android.graphics.drawable.Drawable? {
        return try {
            val appInfo = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationIcon(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 获取应用信息，获取失败时返回null
     */
    fun getAppInfo(packageName: String): ApplicationInfo? {
        return try {
            appContext.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 检查应用包名是否应该被排除
     */
    private fun isExcludedPackage(packageName: String): Boolean {
        val excludePrefixes = listOf("com.android.", "com.meizu.", "android.")
        return excludePrefixes.any { packageName.startsWith(it) }
    }
}