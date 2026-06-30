package com.tinybill.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinybill.service.ForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ForegroundService.start(context)
        }
    }
}
