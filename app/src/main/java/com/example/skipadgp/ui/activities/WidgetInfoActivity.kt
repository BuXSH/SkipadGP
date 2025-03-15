package com.example.skipadgp.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.skipadgp.ui.theme.SkipadGPTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.json.JSONArray
import java.io.File
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import android.content.Context
import org.json.JSONObject
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import com.example.skipadgp.utils.FileUtils
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

class WidgetInfoActivity : ComponentActivity() {
    private lateinit var importLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        importLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (FileUtils.handleImportResult(this, uri, "nodes.json")) {
                    // 刷新列表
                    setContent {
                        SkipadGPTheme {
                            WidgetInfoScreen(importLauncher)
                        }
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            SkipadGPTheme {
                WidgetInfoScreen(importLauncher)
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetInfoScreen(importLauncher: ActivityResultLauncher<Intent>) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    // 使用 remember 和 mutableStateOf 来存储节点信息
    var nodeInfoList by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    LaunchedEffect(Unit) {
        nodeInfoList = loadNodesFromFile(context)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "控件信息",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        context.findActivity()?.finish() 
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "更多选项"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("导入") },
                                onClick = {
                                    showMenu = false
                                    // TODO: 处理导入功能
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "application/json"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                    importLauncher.launch(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出") },
                                onClick = {
                                    showMenu = false
                                    // TODO: 处理导出功能
                                    FileUtils.exportJsonFile(context, "nodes.json", "nodes")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(nodeInfoList) { nodeInfo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 第一行：图标和删除按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧应用图标
                            val icon by produceState<ImageBitmap?>(initialValue = null) {
                                value = withContext(Dispatchers.IO) {
                                    try {
                                        context.packageManager.getApplicationIcon(nodeInfo["packageName"].toString())
                                            .toBitmap()
                                            .asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            
                            icon?.let { bitmap ->
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "应用图标",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            // 右侧删除按钮
                            IconButton(
                                onClick = {
                                    val packageName = nodeInfo["packageName"].toString()
                                    val timestamp = nodeInfo["timestamp"].toString()
                                    
                                    try {
                                        val file = File(context.getExternalFilesDir(null), "nodes.json")
                                        val jsonObject = JSONObject(file.readText())
                                        
                                        // 获取该包名下的节点数组
                                        val packageNodes = jsonObject.getJSONArray(packageName)
                                        
                                        // 找到并删除指定时间戳的节点
                                        for (i in 0 until packageNodes.length()) {
                                            val node = packageNodes.getJSONObject(i)
                                            if (node.getString("timestamp") == timestamp) {
                                                // 从数组中移除该节点
                                                val newArray = JSONArray()
                                                for (j in 0 until packageNodes.length()) {
                                                    if (j != i) {
                                                        newArray.put(packageNodes.get(j))
                                                    }
                                                }
                                                
                                                // 如果该包名下没有节点了，删除整个包名
                                                if (newArray.length() == 0) {
                                                    jsonObject.remove(packageName)
                                                } else {
                                                    jsonObject.put(packageName, newArray)
                                                }
                                                
                                                // 保存更新后的文件
                                                file.writeText(jsonObject.toString(2))
                                                break
                                            }
                                        }
                                        
                                        // 更新界面显示
                                        nodeInfoList = loadNodesFromFile(context)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        // 第二行：信息内容
                        Column {
                            Text(
                                text = "时间: ${nodeInfo["timestamp"]}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("包名: ${nodeInfo["packageName"]}")
                            Text("类名: ${nodeInfo["className"]}")
                            Text("文本: ${nodeInfo["text"]}")
                            Text("描述: ${nodeInfo["contentDescription"]}")
                            Text("位置: ${nodeInfo["bounds"]}")
                        }
                    }
                }
            }
        }
    }
}

private fun loadNodesFromFile(context: Context): List<Map<String, Any>> {
    val file = File(context.getExternalFilesDir(null), "nodes.json")
    if (!file.exists()) return emptyList()

    return try {
        val jsonObject = JSONObject(file.readText())
        val list = mutableListOf<Map<String, Any>>()
        
        // 遍历所有包名
        jsonObject.keys().forEach { packageName ->
            val nodesArray = jsonObject.getJSONArray(packageName)
            // 遍历每个包名下的节点
            for (i in 0 until nodesArray.length()) {
                val nodeObject = nodesArray.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                // 添加包名到节点信息中
                map["packageName"] = packageName
                // 添加其他节点信息
                nodeObject.keys().forEach { key ->
                    map[key] = nodeObject.get(key)
                }
                list.add(map)
            }
        }
        // 按时间戳排序，最新的在前面
        list.sortedByDescending { it["timestamp"].toString() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}