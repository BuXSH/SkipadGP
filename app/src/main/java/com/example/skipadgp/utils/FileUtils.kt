package com.example.skipadgp.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    fun exportJsonFile(context: Context, sourceFileName: String, prefix: String = "export") {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportFile = File(context.getExternalFilesDir(null), "${prefix}_${timestamp}.json")
        
        val sourceFile = File(context.getExternalFilesDir(null), sourceFileName)
        if (sourceFile.exists()) {
            sourceFile.copyTo(exportFile, overwrite = true)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        exportFile
                    )
                )
            }
            
            context.startActivity(Intent.createChooser(intent, "分享文件"))
        }
    }

    fun importJsonFile(context: Context, targetFileName: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        context.startActivity(
            Intent.createChooser(
                intent,
                "选择要导入的 JSON 文件"
            )
        )
    }

    fun handleImportResult(context: Context, uri: android.net.Uri?, targetFileName: String): Boolean {
        if (uri == null) return false
        
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val targetFile = File(context.getExternalFilesDir(null), targetFileName)
            
            inputStream?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}