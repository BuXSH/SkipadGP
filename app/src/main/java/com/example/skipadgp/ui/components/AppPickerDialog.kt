package com.example.skipadgp.ui.components

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.skipadgp.utils.WhitelistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppPickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    whitelistManager: WhitelistManager,
    launcherApps: List<ApplicationInfo>
) {
    if (!showDialog) return

    var selectedApps by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            selectedApps = emptySet()
        },
        title = { Text("选择应用") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                LazyColumn {
                    items(
                        items = launcherApps,
                        key = { it.packageName }
                    ) { appInfo ->
                        AppPickerItem(
                            appInfo = appInfo,
                            whitelistManager = whitelistManager,
                            isSelected = appInfo.packageName in selectedApps,
                            onSelectionChanged = { selected ->
                                selectedApps = if (selected) {
                                    selectedApps + appInfo.packageName
                                } else {
                                    selectedApps - appInfo.packageName
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(selectedApps) },
                    enabled = selectedApps.isNotEmpty()
                ) {
                    Text("添加选中项(${selectedApps.size})")
                }
            }
        }
    )
}

@Composable
private fun AppPickerItem(
    appInfo: ApplicationInfo,
    whitelistManager: WhitelistManager,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    val packageName = appInfo.packageName
    val isWhitelisted = whitelistManager.isPackageWhitelisted(packageName)
    val appName by produceState(initialValue = "") {
        value = withContext(Dispatchers.IO) {
            whitelistManager.getAppName(packageName)
        }
    }
    val icon by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = packageName
    ) {
        value = withContext(Dispatchers.IO) {
            whitelistManager.getAppIcon(packageName)?.toBitmap()?.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isWhitelisted,
                onClick = { onSelectionChanged(!isSelected) }
            )
            .padding(0.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isWhitelisted || isSelected,
            onCheckedChange = { if (!isWhitelisted) onSelectionChanged(it) },
            enabled = !isWhitelisted,
            modifier = Modifier.padding(start = 0.dp)
        )
        
        icon?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "应用图标",
                modifier = Modifier.size(40.dp)
            )
        }
        
        Column {
            Text(text = appName)
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}