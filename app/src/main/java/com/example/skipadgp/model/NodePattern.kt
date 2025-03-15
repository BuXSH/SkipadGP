package com.example.skipadgp.model

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

data class NodePattern(
    val packageName: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val depth: Int
) {
    fun matches(node: AccessibilityNodeInfo): Boolean {
        val nodeText = node.text?.toString()
        val nodeDesc = node.contentDescription?.toString()
        val nodeBounds = Rect().apply { node.getBoundsInScreen(this) }

        return packageName == node.packageName?.toString() &&
               className == node.className?.toString() &&
               (text == null || text == nodeText) &&
               (contentDescription == null || contentDescription == nodeDesc) &&
               bounds == nodeBounds &&
               isClickable == node.isClickable
    }
}