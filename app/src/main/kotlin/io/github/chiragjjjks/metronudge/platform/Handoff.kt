package io.github.chiragjjjks.metronudge.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Where the ticket is bought. Both are official BMRCL channels. */
enum class Channel(val label: String) {
    WHATSAPP("WhatsApp bot"),
    APP("Namma Metro app"),
}

/**
 * Hands the purchase to an official BMRCL channel. BMRCL has no public
 * ticketing API, so MetroNudge never buys or pays itself: it opens the
 * channel and copies the stations so you can paste them; you send/pay.
 */
object Handoff {
    /** BMRCL's official WhatsApp ticketing number (+91 81055 56677). */
    const val WHATSAPP_NUMBER = "918105556677"
    const val NAMMA_METRO_PACKAGE = "com.aum.nammametro"

    fun findNammaMetro(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(NAMMA_METRO_PACKAGE)

    fun isNammaMetroInstalled(context: Context) = findNammaMetro(context) != null

    fun isWhatsAppInstalled(context: Context) =
        listOf("com.whatsapp", "com.whatsapp.w4b").any { context.packageManager.getLaunchIntentForPackage(it) != null }

    /** Opens [channel] for a trip, copying "Source to Destination" first. */
    fun buy(context: Context, channel: Channel, sourceName: String?, destinationName: String?) {
        val route = listOfNotNull(sourceName, destinationName).joinToString(" to ")
        if (route.isNotEmpty()) {
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("Metro trip", route))
        }
        when (channel) {
            // "Hi" starts BMRCL's bot; it then asks for stations and payment.
            Channel.WHATSAPP -> open(context, "https://wa.me/$WHATSAPP_NUMBER?text=Hi".toUri().let { Intent(Intent.ACTION_VIEW, it) })
            Channel.APP -> open(
                context,
                findNammaMetro(context) ?: Intent(Intent.ACTION_VIEW, "market://details?id=$NAMMA_METRO_PACKAGE".toUri()),
                fallback = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$NAMMA_METRO_PACKAGE".toUri()),
            )
        }
    }

    private fun open(context: Context, intent: Intent, fallback: Intent? = null) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            fallback?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
