package com.example.skipadgp.ui.activities

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.skipadgp.ui.theme.SkipadGPTheme
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.remember
import com.example.skipadgp.utils.WhitelistManager
import androidx.activity.enableEdgeToEdge 
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowBack
import android.app.Activity
import android.content.ContextWrapper
import android.content.Context
import com.example.skipadgp.ui.components.AppPickerDialog
import com.example.skipadgp.ui.components.WhitelistedAppItem


/**
 * 白名单管理界面的主Activity
 * 负责显示和管理应用白名单，允许用户添加或删除白名单中的应用
 */
class WhitelistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启用边缘到边缘的显示效果
        enableEdgeToEdge()
        setContent {
            SkipadGPTheme {
                WhitelistScreen() 
            }
        }
    }
}

// 在文件顶部添加这个扩展函数
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 白名单管理界面的主要内容
 * 
 * 该界面包含一个顶部应用栏，显示标题和添加按钮，以及一个可滚动的应用列表。
 * 用户可以通过点击添加按钮来添加新的应用到白名单中，也可以通过点击删除按钮移除已添加的应用。
 * 
 * @OptIn(ExperimentalMaterial3Api::class) 标注表明使用了实验性的Material3 API
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen() {
    val context = LocalContext.current
    val whitelistManager = remember { WhitelistManager.getInstance(context) }
    var whitelistedApps by remember { mutableStateOf(whitelistManager.getWhitelistedPackages()) }
    var showAppPicker by remember { mutableStateOf(false) }

    val launcherApps by produceState<List<ApplicationInfo>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            whitelistManager.getInstalledUserApps()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "白名单管理",
                        style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { context.findActivity()?.finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAppPicker = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加应用")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                items = whitelistedApps.toList(),
                key = { it }
            ) { packageName ->
                WhitelistedAppItem(
                    packageName = packageName,
                    onRemove = {
                        whitelistManager.removeFromWhitelist(packageName)
                        whitelistedApps = whitelistManager.getWhitelistedPackages()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        AppPickerDialog(
            showDialog = showAppPicker,
            onDismiss = { showAppPicker = false },
            onConfirm = { selectedApps ->
                selectedApps.forEach { packageName ->
                    whitelistManager.addToWhitelist(packageName)
                }
                whitelistedApps = whitelistManager.getWhitelistedPackages()
                showAppPicker = false
            },
            whitelistManager = whitelistManager,
            launcherApps = launcherApps
        )
    }
}

/**
 * 白名单应用列表项
 * @param packageName 应用包名
 * @param onRemove 删除回调
 */
@Composable
fun WhitelistedAppItem(
    packageName: String,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    // val packageManager: PackageManager = context.packageManager
    val whitelistManager = remember { WhitelistManager.getInstance(context) }
    // 尝试获取应用信息
    // val appInfo = try {
    //     packageManager.getApplicationInfo(packageName, 0)
    // } catch (e: PackageManager.NameNotFoundException) {
    //     null
    // }
    // 异步加载应用图标
    val icon by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = packageName
    ) {
        value = withContext(Dispatchers.IO) {
            whitelistManager.getAppIcon(packageName)?.toBitmap()?.asImageBitmap()
        }
    }
    // 获取应用名称
    val appName = whitelistManager.getAppName(packageName)

    // 使用Card组件创建一个卡片式的列表项
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 使用Row布局来水平排列应用信息和删除按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // 设置水平布局为两端对齐，使删除按钮靠右显示
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically  // 改为居中对齐
        ) {
            // 添加水平排列的应用信息区域
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),  // 设置图标和文本之间的间距
                verticalAlignment = Alignment.CenterVertically
            ){
                // 显示应用图标
                icon?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = "应用图标",
                        modifier = Modifier.size(50.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f)  // 添加权重，让文本占据剩余空间
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true  // 允许文本换行
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.padding(start = 8.dp)  // 添加左边距，与文本保持间距
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "从白名单移除")
                }
            }
        }
    }
}