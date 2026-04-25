package com.phoneagent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.phoneagent.data.remote.models.AgentAction
import com.phoneagent.data.remote.models.AgentActionType
import com.phoneagent.utils.AccessibilityUtils

class PhoneAgentAccessibilityService : AccessibilityService() {

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.phoneagent.EXECUTE_ACTION") {
                val actionType = intent.getStringExtra("ACTION_TYPE")
                val x = intent.getFloatExtra("X", -1f)
                val y = intent.getFloatExtra("Y", -1f)
                val text = intent.getStringExtra("TEXT")
                
                var success = false
                when (actionType) {
                    "TAP" -> success = performTap(x, y)
                    "TYPE" -> if (text != null) success = performTypeText(text)
                    // We can route others as needed
                }
                
                val resultIntent = Intent("com.phoneagent.ACTION_RESULT")
                resultIntent.putExtra("SUCCESS", success)
                sendBroadcast(resultIntent)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "PhoneAgentAccessibilityService Connected")
        
        val filter = IntentFilter("com.phoneagent.EXECUTE_ACTION")
        // registerReceiver requires RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED in Android 14+
        registerReceiver(actionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Minimal implementation as requested
        event?.let {
            Log.d(TAG, "Event: ${AccessibilityEvent.eventTypeToString(it.eventType)}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "PhoneAgentAccessibilityService Interrupted")
    }

    // CAPABILITY 1: TAP at coordinates
    fun performTap(x: Float, y: Float): Boolean {
        Log.d(TAG, "performTap: x=$x, y=$y")
        if (x < 0 || y < 0) return false
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    // CAPABILITY 2: LONG PRESS at coordinates
    fun performLongPress(x: Float, y: Float): Boolean {
        Log.d(TAG, "performLongPress: x=$x, y=$y")
        if (x < 0 || y < 0) return false
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, 1000)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    // CAPABILITY 3: SWIPE from point to point
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): Boolean {
        Log.d(TAG, "performSwipe: start($startX, $startY) to end($endX, $endY)")
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    // CAPABILITY 4: SCROLL in direction
    fun performScroll(direction: String, distance: Int): Boolean {
        Log.d(TAG, "performScroll: direction=$direction, distance=$distance")
        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val centerY = metrics.heightPixels / 2f
        
        var startX = centerX
        var startY = centerY
        var endX = centerX
        var endY = centerY
        
        val distFloat = distance.toFloat()
        
        when (direction.uppercase()) {
            "UP" -> { startY += distFloat / 2; endY -= distFloat / 2 }
            "DOWN" -> { startY -= distFloat / 2; endY += distFloat / 2 }
            "LEFT" -> { startX += distFloat / 2; endX -= distFloat / 2 }
            "RIGHT" -> { startX -= distFloat / 2; endX += distFloat / 2 }
            else -> return false
        }
        return performSwipe(startX, startY, endX, endY, 500L)
    }

    // CAPABILITY 5: TYPE TEXT
    fun performTypeText(text: String): Boolean {
        Log.d(TAG, "performTypeText: text=$text")
        val root = rootInActiveWindow ?: return false
        val focusedNode = findFocusedNode(root)
        
        if (focusedNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            if (success) return true
        }
        
        Log.w(TAG, "performTypeText: Failed via SET_TEXT, focused field not found or action failed.")
        return false
    }

    private fun findFocusedNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused) return node
        for (i in 0 until node.childCount) {
            val focusedChild = findFocusedNode(node.getChild(i))
            if (focusedChild != null) return focusedChild
        }
        return null
    }

    // CAPABILITY 6: PRESS BACK
    fun performPressBack(): Boolean {
        Log.d(TAG, "performPressBack")
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    // CAPABILITY 7: PRESS HOME
    fun performPressHome(): Boolean {
        Log.d(TAG, "performPressHome")
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    // CAPABILITY 8: PRESS RECENT APPS
    fun performRecentApps(): Boolean {
        Log.d(TAG, "performRecentApps")
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    // CAPABILITY 9: OPEN APP BY NAME
    fun openApp(appName: String): Boolean {
        Log.d(TAG, "openApp: appName=$appName")
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString()
            if (label.equals(appName, ignoreCase = true)) {
                val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return true
                }
            }
        }
        Log.w(TAG, "openApp: App not found: $appName")
        return false
    }

    // CAPABILITY 10: FIND ELEMENT BY TEXT AND TAP
    fun tapElementWithText(text: String): Boolean {
        Log.d(TAG, "tapElementWithText: text=$text")
        val root = rootInActiveWindow ?: return false
        val node = AccessibilityUtils.findNodeByText(root, text)
        if (node != null) {
            val (x, y) = AccessibilityUtils.getBoundsCenter(node)
            node.recycle()
            return performTap(x, y)
        }
        return false
    }

    companion object {
        private const val TAG = "PhoneAgentAccessibility"
        var instance: PhoneAgentAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun isSensitiveAction(action: AgentAction): Boolean {
            val text = action.params?.text?.lowercase() ?: ""
            if (action.action == AgentActionType.TYPE) {
                if (text.contains("password") || text.contains("pwd")) return true
                if (text.length > 100) return true // Long text might be a sensitive post
            }
            
            val appName = action.params?.appName?.lowercase() ?: ""
            val financialApps = listOf("phonepe", "gpay", "paytm", "bank", "paypal")
            if (financialApps.any { appName.contains(it) }) return true
            
            return false
        }

    }
}
