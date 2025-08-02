package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myapplication.utils.TAG_MAIN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue

class MyAccessibilityService : AccessibilityService() {

    // optional:  this is one way to add/remove packages and make changes on runtime
//    override fun onServiceConnected() {
//        val info: AccessibilityServiceInfo = AccessibilityServiceInfo()
//        info.apply{
//            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
//            packageNames = arrayOf("com.google.android.youtube")
//            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
//            notificationTimeout = 100
//        }
//        super.onServiceConnected()
//        this.serviceInfo = info
//    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var scannerJob: Job? = null
    override fun onInterrupt() {
        Log.d(TAG_MAIN, "Service interrupted.")
    }

    // this is the main function called when "our" accessibility event is triggered
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d(TAG_MAIN, "Window STATE changed")
            handleAppChange(packageName)
        }
    }

    private fun handleAppChange(packageName: String?) {
        // Stop any previously running scanner job to avoid multiple scanners.
        scannerJob?.cancel()

        when (packageName) {
            "com.google.android.youtube", "com.facebook.katana", "com.instagram.android" -> {
                Log.d(TAG_MAIN, "Starting scanner")
                startCoroutineScanner(packageName)
            }

            else -> {
                Log.d(TAG_MAIN, "Non-target app detected. Scanner stopped.")
                // Not a target app, do nothing. The job is already cancelled.
            }
        }
    }

    private fun isYouTubeShorts(node: AccessibilityNodeInfo): Boolean {
        // STRATEGY 1: Check for Resource ID containing "reel" (Most Reliable)
        Log.d(TAG_MAIN, "No of children in the node: ${node.childCount}")

        // STRATEGY 2: Check for Content Description of "Remix" button (Good Fallback)
//        if (findNodeByContentDescription(node, "Remix")) {
//            return true
//        }
        if (findNodeByContentDescription(
                node,
                "See more videos using this sound"
            ) || findNodeByContentDescription(node, "Video Progress")
        ) {
            return true
        }
        return false
    }

    private fun startCoroutineScanner(packageName: String) {
        scannerJob = serviceScope.launch {
            while (isActive) { // This loop will automatically stop when the job is cancelled
                val rootNode = rootInActiveWindow ?: run {
                    Log.e(TAG_MAIN, "RootNode is null. Waiting and trying again.")
                    delay(500) // If rootNode is null, wait and try again
                    return@launch
                }

                val isShortsDetected = when (packageName) {
                    "com.google.android.youtube" -> {
                        isYouTubeShorts(rootNode)
                    }

                    "com.facebook.katana" -> isFacebookReelsPlaying(rootNode)
                    // Add case for "com.instagram.android" here
                    else -> false
                }

                if (isShortsDetected) {
                    Log.d(
                        TAG_MAIN,
                        "Short-form video detected."
                    )
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    // The job will be cancelled by the next call to handleAppChange,
                    // which is triggered by the window state change from the BACK action.
                    // This is a clean way to stop the loop.
                }

                delay(500) // Wait for 500ms before the next check
            }
        }
    }

    private fun findNodeByContentDescription(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) {
            Log.e(TAG_MAIN, "Node is null")
            return false
        }
        node.contentDescription?.let { contentDesc ->
            if (contentDesc.contains(text, ignoreCase = true)) {
                Log.d(TAG_MAIN, "Found Shorts in Content Description")
                return true
            } else {
                Log.e(TAG_MAIN, "$contentDesc")
            }
        }
        for (i in 0 until node.childCount) {
            if (findNodeByContentDescription(node.getChild(i), text)) return true
        }
        return false
    }

    private fun isFacebookReelsPlaying(node: AccessibilityNodeInfo): Boolean {
        val q: Queue<AccessibilityNodeInfo> = LinkedList()
        q.add(node)

        while (q.isNotEmpty()) {
            val current = q.poll()

            // Get content description and resource ID safely
            val contentDesc = current?.contentDescription?.toString()
            // val resourceId = current.viewIdResourceName // Many were (name removed) in your dump

            // --- Strong Positive Indicators (from your "Reel playing" dump) ---
            if (contentDesc != null) {
                // 1. Explicit mention of "Reels Sound" in content description (highly specific)
                if (contentDesc.contains("Reels Sound", ignoreCase = true)) {
                    Log.d("FacebookReels", "Indicator: 'Reels Sound' found.")
                    return true
                }
                // 2. "Get a videogram" content description (also highly specific to Reels)
                if (contentDesc.contains("Get a videogram", ignoreCase = true)) {
                    Log.d("FacebookReels", "Indicator: 'Get a videogram' found.")
                    return true
                }
                // 3. A button explicitly labeled "Video details" (often an overlay on the player)
                if (current.className == "android.widget.Button" && contentDesc.equals(
                        "Video details",
                        ignoreCase = true
                    )
                ) {
                    Log.d("FacebookReels", "Indicator: 'Video details' button found.")
                    return true
                }
                // 4. "UP NEXT:" combined with a time duration (typical for sequential video players)
                if (contentDesc.contains("UP NEXT:", ignoreCase = true) && current.childCount > 0) {
                    // Check if a child node indicates a time duration (e.g., "0:24")
                    // This is a more complex check, but could be very accurate.
                    // For simplicity, we can just rely on "UP NEXT:" being a strong enough signal
                    // in the context of the other checks.
                    Log.d("FacebookReels", "Indicator: 'UP NEXT:' found.")
                    return true
                }
            }

            // --- Negative Indicators (from this "not playing" dump) ---
            // While we primarily look for positive indicators,
            // you could also explicitly exclude if certain elements are present
            // that are characteristic of the general feed but not the player.
            // However, this makes the logic more complex and prone to breaking
            // if Facebook changes their general feed UI. Sticking to positive
            // indicators is usually more robust.
            // Example of a negative check (generally avoid unless necessary):
            // if (current.viewIdResourceName == "android:id/list" && current.className == "androidx.recyclerview.widget.RecyclerView") {
            //    Log.d("FacebookReels", "Negative indicator: General RecyclerView list found. Likely not full-screen Reel.")
            //    // return false // Or just don't add its children to the queue if it implies a different UI section
            // }


            // Traverse children
            if (current != null) {
                for (i in 0 until current.childCount) {
                    current.getChild(i)?.let { child ->
                        // Important: Add a check here to avoid processing too deep or specific
                        // branches that are known to not contain Reel player info
                        // e.g., if (child.className != "android.widget.TextView" && child.className != "android.widget.ImageView")
                        // This can prune the search tree.
                        q.add(child)
                    }
                }
            }
        }
        return false // No strong indicators of a full-screen Reel player found
    }
}