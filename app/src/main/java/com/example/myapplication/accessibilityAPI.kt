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

                    "com.facebook.katana" -> {
                        isFacebookReelsPlaying(rootNode)
                    }
                    // Add case for "com.instagram.android" here
                    "com.instagram.android" -> {
                        isInstagramReelsPlaying(rootNode)
                    }

                    else -> false
                }

                if (isShortsDetected) {
                    Log.d(
                        TAG_MAIN,
                        "Short-form video detected."
                    )
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
                delay(500)
            }
        }
    }

    private fun isYouTubeShorts(node: AccessibilityNodeInfo): Boolean {
        return findNodeByContentDescription(
            node,
            "See more videos using this sound"
        ) || findNodeByContentDescription(node, "Video Progress")
    }

    private fun isFacebookReelsPlaying(node: AccessibilityNodeInfo?): Boolean {
        return findNodeByContentDescription(node, "UP NEXT:") || findNodeByContentDescription(
            node,
            "Reel Found"
        )
    }

    private fun isInstagramReelsPlaying(node: AccessibilityNodeInfo?): Boolean {
        return findNodeByContentDescription(node, "Notes on Reels") || findNodeByContentDescription(
            node,
            "Double tap to play or pause."
        )
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
}