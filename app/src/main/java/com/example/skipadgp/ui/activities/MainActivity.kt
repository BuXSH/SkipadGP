package com.example.skipadgp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.skipadgp.ui.theme.SkipadGPTheme
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.example.skipadgp.utils.AccessibilityServiceManager
import com.example.skipadgp.service.FloatingButtonService
import com.example.skipadgp.ui.activities.WhitelistActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
/**
 * 主活动类，负责管理应用的主界面和无障碍服务状态
 * 使用Jetpack Compose构建UI，提供无障碍服务的开启状态显示和设置入口
 */
class MainActivity : ComponentActivity() {
    // 使用可变状态来存储无障碍服务的状态
    private var isServiceEnabled by mutableStateOf(false)
    // 添加AccessibilityServiceManager实例
    private lateinit var accessibilityServiceManager: AccessibilityServiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 添加启动日志（位置：在生命周期方法的最先调用处）
        Log.d("AppLifecycle", "应用启动了 (MainActivity.onCreate)")
        // 初始化AccessibilityServiceManager
        accessibilityServiceManager = AccessibilityServiceManager.getInstance(this)
        // 启用边缘到边缘的显示效果
        enableEdgeToEdge()
        // 初始化无障碍服务状态
        isServiceEnabled = accessibilityServiceManager.isAccessibilityServiceEnabled()
        // 设置Compose界面内容
        setContent {
            SkipadGPTheme {
                // 使用Scaffold作为基础布局
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(
                        isAccessibilityServiceEnabled = isServiceEnabled,
                        onOpenAccessibilitySettings = {
                            // 打开系统的无障碍服务设置页面
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /**
     * Activity 生命周期回调方法，在 Activity 变为前台可见状态时调用
     * 用于更新无障碍服务的状态显示
     */
    override fun onResume() {
        // 调用父类的 onResume 方法
        super.onResume()
        // 检查并更新无障碍服务的启用状态
        isServiceEnabled = accessibilityServiceManager.isAccessibilityServiceEnabled()
        // 向 AccessibilityService 发送查询请求，获取服务的最新状态
        accessibilityServiceManager.queryServiceState()
    }

    /**
     * Activity 生命周期回调方法，在 Activity 被销毁前调用
     * 用于清理资源和执行必要的销毁操作
     */
    override fun onDestroy() {
        // 调用父类的 onDestroy 方法，确保完成基类的清理工作
        super.onDestroy()
    }
}

/**
 * 主界面内容组件
 * @param isAccessibilityServiceEnabled Boolean 无障碍服务是否已启用
 * @param onOpenAccessibilitySettings Function0<Unit> 打开无障碍设置的回调函数
 * @param modifier Modifier 组件的修饰符
 */
@OptIn(ExperimentalMaterial3Api::class) //ModalBottomSheet 是 Material3 的实验性 API。解决警告，需要添加 @OptIn 注解
@Composable
fun MainContent(
    isAccessibilityServiceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取当前Context
    val context = LocalContext.current
    // 添加悬浮窗状态
    var isFloatingButtonActive by remember { mutableStateOf(false) }
    // 使用Column布局来垂直排列UI元素
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SkipadGP",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 34.sp,
                color = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp)
                .align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(32.dp))
        // 显示无障碍服务状态
        // 状态显示
        Text(
            text = if (isAccessibilityServiceEnabled) "无障碍服务已开启" else "无障碍服务未开启",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 24.sp,
                color = if (isAccessibilityServiceEnabled) Color(0xFF248067) else Color.Red
            )
        )
        // 添加垂直间距
        Spacer(modifier = Modifier.height(16.dp))
        // 按钮文本样式
        val textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            color = Color.Black
        )
        // 按钮描述文本样式
        val descriptionStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 16.sp,
            color = Color(0xFF5D5C71)
        )
        // 统一按钮样式
        val buttonModifier = Modifier
            .fillMaxWidth(0.9f)
            .height(100.dp)
            .padding(horizontal = 16.dp)
        // 定义方形按钮形状
        val buttonShape = RoundedCornerShape(8.dp)
        // 定义按钮颜色
        val buttonColors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        )
        // 底部弹窗状态
        var showBottomSheet by remember { mutableStateOf(false) }
        // 无障碍服务按钮
        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = buttonModifier,
            shape = buttonShape,
            colors = buttonColors
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "无障碍服务",
                    style = textStyle
                )
                Text(
                    text = "请点击：无障碍服务；开启无障碍服务才能使用全部功能",
                    style = descriptionStyle
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 白名单管理按钮
        Button(
            onClick = {
                context.startActivity(Intent(context, WhitelistActivity::class.java))
            },
            modifier = buttonModifier,
            shape = buttonShape,
            colors = buttonColors,
            enabled = isAccessibilityServiceEnabled
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "白名单管理",
                    style = textStyle
                )
                Text(
                    text = "设置不需要跳过广告的应用",
                    style = descriptionStyle
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 悬浮窗按钮
        Button(
            onClick = {
                // 调试日志
                Log.d("FloatingButton", "按钮点击")
                Log.d("FloatingButton", "当前状态：$isFloatingButtonActive")
                if(!isFloatingButtonActive){
                    // 检查是否有悬浮窗权限
                    if (Settings.canDrawOverlays(context)) {
                        // 调试日志
                        Log.d("FloatingButton", "尝试启动悬浮窗服务")
                        // 有权限，直接启动悬浮窗服务
                        context.startService(Intent(context, FloatingButtonService::class.java))
                        isFloatingButtonActive = true
                    } else {
                        // 没有权限，跳转到权限设置页面
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }else{
                    // 关闭悬浮窗服务
                    context.stopService(Intent(context, FloatingButtonService::class.java))
                    isFloatingButtonActive = false
                }
            },
            modifier = buttonModifier,
            shape = buttonShape,
            colors = buttonColors,
            enabled = isAccessibilityServiceEnabled
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isFloatingButtonActive) "关闭控件采集" else "开启控件采集",
                    style = textStyle
                )
                Text(
                    text = "开启后可以采集需要跳过的广告控件",
                    style = descriptionStyle
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 控件信息按钮
        Button(
            onClick = {
                context.startActivity(Intent(context, WidgetInfoActivity::class.java))
            },
            modifier = buttonModifier,
            shape = buttonShape,
            colors = buttonColors,
            enabled = isAccessibilityServiceEnabled
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "控件信息",
                    style = textStyle
                )
                Text(
                    text = "查看、管理已采集到的控件信息",
                    style = descriptionStyle
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 帮助按钮
        Button(
            onClick = { showBottomSheet = true },
            modifier = buttonModifier,
            shape = buttonShape,
            colors = buttonColors
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "帮助",
                    style = textStyle
                )
                Text(
                    text = "使用文档，学会如何使用",
                    style = descriptionStyle
                )
            }
        }
        // 添加底部弹窗
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = rememberModalBottomSheetState(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 0.dp) 
                ) {
                    Text(
                        text = "使用说明",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "无障碍服务\n" +
                            "   • 点击\"无障碍服务\"\n" +
                            "   • 在系统设置中找到并启用\"SkipadGP\"\n" +
                            "   • 授予无障碍权限\n" +
                            "   • 即可自动点击其他应用的开屏广告\n\n" +
                            "白名单管理\n" +
                            "   • 选择不需要自动点击开屏广告的应用\n\n" +
                            "可选功能：针对不能自动点击开屏广告的情况时使用\n\n" +
                            "开启控件采集\n" +
                            "   • 首次使用需要授予悬浮窗权限\n" +
                            "   • 进入需要自动点击开屏广告的应用\n" +
                            "   • 当开屏广告出现时，点击\"眼睛\"按钮\n" +
                            "   • 选中开屏广告的\"跳过\"按钮\n" +
                            "   • 点击\"+\"保存控件信息\n\n" +
                            "控件信息\n" +
                            "   • 管理已保存的广告按钮\n" +
                            "   • 被保存的广告按钮会被自动点击\n" +
                            "   • 支持导入导出控件配置\n\n" +
                            "注意事项\n" +
                            "   • 需要保持应用后台始终存活\n\n" +
                            "反馈\n" +
                            "   • GitHub Issues: github.com/BuXSH/SkipadGP/issues\n" +
                            "   • 邮箱: bzzyl@foxmail.com",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFF5D5C71)
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * 主界面内容的预览组件
 */
@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    SkipadGPTheme {
        MainContent(
            isAccessibilityServiceEnabled = false,
            onOpenAccessibilitySettings = {}
        )
    }
}






