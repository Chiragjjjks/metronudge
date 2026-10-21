package io.github.chiragjjjks.metronudge.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Not now" on a nudge: just dismiss it. The per-station cooldown already applies. */
class NotNowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Notifier.cancelNudge(context)
}
