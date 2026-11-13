package com.freekiosklite.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reserved for an explicit, administrator-approved recovery flow.
 * It is intentionally not registered until boot recovery behavior is designed.
 */
class BootRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
