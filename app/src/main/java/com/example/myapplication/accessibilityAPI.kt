package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.ServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    override fun onInterrupt() {
        Log.d("ShortsBlockerService", "Service interrupted.")

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()
        if (event == null) return
        if(packageName == "com.google.android.youtube"){
            val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
            if (rootNode == null) {
                Log.w("ShortsBlockerService", "Root node is null for $packageName. Cannot inspect window content.")
                return
            }
            Log.d("ShortsBlockerService","Root Node Id: ${rootNode.viewIdResourceName} ClassNameId: ${rootNode.className.toString()} ContentDescription: ${rootNode.contentDescription.toString()} Text: ${rootNode.text.toString()}")
            if(isYouTubeShorts(rootNode)){
                Log.d("ShortsBlockerService","Youtube Shorts Detected")
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }else if(packageName == "com.facebook.katana"){
            val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
            if(rootNode == null){
                return
            }
            if(isFacebookReelsPlaying(rootNode)){
                Log.d("ShortsBlockerService","Facebook shorts playing")
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun isYouTubeShorts(node: AccessibilityNodeInfo): Boolean {
        node.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_watch_fragment_root")
            ?.let {
                if (it.isNotEmpty()) {
                    return true
                }
            }
        node.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_recycler")
            ?.let {
                if (it.isNotEmpty()) {
                    return true
                }
            }
        node.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_watch_player")?.let{
            if(it.isNotEmpty()){
                return true
            }
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
                if (current.className == "android.widget.Button" && contentDesc.equals("Video details", ignoreCase = true)) {
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