package io.github.chiragjjjks.metronudge.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Opens the official Namma Metro app so you can buy the ticket there.
 *
 * BMRCL has no public ticketing API, so MetroNudge cannot buy or pay for you.
 * It finds the installed app by its name (no hard-coded package id) and copies
 * the destination so you can paste it into the station search.
 */
object Handoff {
    private val NAMES = listOf("namma metro")

    /** The installed Namma Metro app's launch intent, or null. */
    fun findNammaMetro(context: Context): Intent? {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(launcher, 0).firstOrNull { info ->
            info.activityInfo.packageName != context.packageName &&
                NAMES.any { info.loadLabel(pm).toString().lowercase().contains(it) }
        } ?: return null
        return pm.getLaunchIntentForPackage(match.activityInfo.packageName)
    }

    fun isNammaMetroInstalled(context: Context) = findNammaMetro(context) != null

    /** Copies [destinationName] and opens Namma Metro, or its store page. */
    fun buyInNammaMetro(context: Context, destinationName: String?) {
        destinationName?.let {
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("Destination station", it))
        }
        val intent = findNammaMetro(context)
            ?: Intent(Intent.ACTION_VIEW, "market://search?q=Namma%20Metro%20BMRCL%20official".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "https://play.google.com/store/search?q=Namma%20Metro%20BMRCL&c=apps".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
