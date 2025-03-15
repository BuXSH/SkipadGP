package com.example.skipadgp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.skipadgp.utils.WhitelistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WhitelistedAppItem(
    packageName: String,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val whitelistManager = remember { WhitelistManager.getInstance(context) }
    
    val icon by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = packageName
    ) {
        value = withContext(Dispatchers.IO) {
            whitelistManager.getAppIcon(packageName)?.toBitmap()?.asImageBitmap()
        }
    }
    val appName = whitelistManager.getAppName(packageName)

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = "应用图标",
                        modifier = Modifier.size(50.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "从白名单移除")
                }
            }
        }
    }
}