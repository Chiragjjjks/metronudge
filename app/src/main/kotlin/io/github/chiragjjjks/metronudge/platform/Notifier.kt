package io.github.chiragjjjks.metronudge.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.chiragjjjks.metronudge.MainActivity
import io.github.chiragjjjks.metronudge.R
import io.github.chiragjjjks.metronudge.core.Trip
import io.github.chiragjjjks.metronudge.ui.shortName

object Notifier {
    const val CHANNEL_NUDGE = "nudge"
    const val CHANNEL_WATCH = "watch"
    const val ID_WATCH = 1
    const val ID_NUDGE = 2

    const val EXTRA_SOURCE = "source"
    const val EXTRA_DESTINATION = "destination"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_NUDGE, "Buy-ticket nudges", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "When you're walking into a metro station"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_WATCH, "Watching for stations", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while MetroNudge checks your location"
                setShowBadge(false)
            },
        )
    }

    fun tripIntent(context: Context, trip: Trip): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_TRIP)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SOURCE, trip.source.id)
            .putExtra(EXTRA_DESTINATION, trip.destination?.id)

    fun buyIntent(context: Context, sourceId: String?, destinationId: String?): Intent =
        Intent(context, BuyActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(EXTRA_SOURCE, sourceId)
            .putExtra(EXTRA_DESTINATION, destinationId)

    /**
     * "RV Road → Rajajinagar · 97 m. Buy your ticket?" with
     * [Buy → Rajajinagar] [Change] [Not now].
     */
    fun showNudge(context: Context, trip: Trip, distanceMeters: Int) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openTrip = PendingIntent.getActivity(context, 10, tripIntent(context, trip), flags)
        val notNow = PendingIntent.getBroadcast(context, 11, Intent(context, NotNowReceiver::class.java), flags)
        val dst = trip.destination
        val title = dst?.let { "${shortName(trip.source)} → ${shortName(it)}" } ?: "Near ${shortName(trip.source)}"
        val text = if (dst != null) "You're $distanceMeters m from the station. Buy your ticket?"
        else "You're $distanceMeters m from the station. Where to?"

        val b = Notification.Builder(context, CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_stat_metro)
            .setContentTitle("🚇 $title")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setGroup("nudge")
            .setColor(0xFF8E3A96.toInt())
            .setContentIntent(openTrip)
            .setAutoCancel(true)
        if (dst != null) {
            val buy = PendingIntent.getActivity(context, 12, buyIntent(context, trip.source.id, dst.id), flags)
            b.addAction(Notification.Action.Builder(null, "Buy → ${shortName(dst)}", buy).build())
            b.addAction(Notification.Action.Builder(null, "Change", openTrip).build())
        } else {
            b.addAction(Notification.Action.Builder(null, "Choose destination", openTrip).build())
        }
        b.addAction(Notification.Action.Builder(null, "Not now", notNow).build())
        context.getSystemService(NotificationManager::class.java).notify(ID_NUDGE, b.build())
    }

    fun cancelNudge(context: Context) =
        context.getSystemService(NotificationManager::class.java).cancel(ID_NUDGE)

    fun watchNotification(context: Context, text: String): Notification {
        val open = PendingIntent.getActivity(
            context, 20,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            context, 21,
            Intent(context, WatchService::class.java).setAction(WatchService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, CHANNEL_WATCH)
            .setSmallIcon(R.drawable.ic_stat_metro)
            .setContentTitle("Watching for metro stations")
            .setContentText(text)
            .setOngoing(true)
            .setGroup("watch")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
    }
}
