package org.textshield.project.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles MMS messages when the app is set as the default SMS app
 */
class MmsReceiver : BroadcastReceiver() {
    private val TAG = "MmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "MMS received: ${intent.action}")
        
        // In a real implementation, we would process the MMS message here
        // For now, we just need this receiver to satisfy Android's requirements
        // for being a default SMS app
    }
} 