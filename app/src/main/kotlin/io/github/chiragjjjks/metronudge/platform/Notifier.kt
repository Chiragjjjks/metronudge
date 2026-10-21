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

    /** "Near RV Road → Rajajinagar. Buy your ticket?" with Buy / Not now. */
    fun showNudge(context: Context, trip: Trip, distanceMeters: Int) {
        val open = PendingIntent.getActivity(
            context, 10, tripIntent(context, trip),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notNow = PendingIntent.getBroadcast(
            context, 11,
            Intent(context, NotNowReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = trip.destination?.let { "${shortName(trip.source)} → ${shortName(it)}" } ?: "Near ${shortName(trip.source)}"
        val text = if (trip.destination != null) {
            "You're ${distanceMeters} m from the station. Buy your ticket now?"
        } else {
            "You're ${distanceMeters} m from the station. Where to?"
        }
        val n = Notification.Builder(context, CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_stat_metro)
            .setContentTitle("🚇 $title")
            .setContentText(text)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "Buy ticket", open).build())
            .addAction(Notification.Action.Builder(null, "Not now", notNow).build())
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_NUDGE, n)
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
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
    }
}
