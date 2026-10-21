package io.github.chiragjjjks.metronudge.platform

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import io.github.chiragjjjks.metronudge.MainActivity
import io.github.chiragjjjks.metronudge.R
import io.github.chiragjjjks.metronudge.data.Store
import io.github.chiragjjjks.metronudge.ui.formatDistance
import io.github.chiragjjjks.metronudge.ui.shortName

/**
 * Home-screen widget: nearest station → home, distance, a Buy button, and
 * "Show ticket" once today's ticket is imported. Refreshed by [Store]
 * whenever state changes.
 */
class MetroWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MetroWidget::class.java))
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, build(context))
        }

        private fun build(context: Context): RemoteViews {
            val store = Store.get(context)
            val pack = store.pack
            val v = RemoteViews(context.packageName, R.layout.widget_metro)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            val status = store.status?.takeIf { store.watching }
            val near = status?.let { pack.stationById(it.stationId) }
            val home = store.homeStationId?.let { pack.stationById(it) }
            val dst = home?.takeIf { it.id != near?.id }
            val ticket = store.ticket?.takeIf { it.isValidToday }

            v.setTextViewText(R.id.w_status, if (store.watching) "● Live" else "○ Paused")
            v.setTextViewText(
                R.id.w_route,
                when {
                    near != null && dst != null -> "${shortName(near)} → ${shortName(dst)}"
                    near != null -> "Near ${shortName(near)}"
                    else -> "Open to start watching"
                },
            )
            v.setTextViewText(
                R.id.w_detail,
                when {
                    status != null -> "${formatDistance(status.distanceMeters)} away · via ${store.channel.label}"
                    home != null -> "Home: ${shortName(home)}"
                    else -> "Pick your home station"
                },
            )

            val open = PendingIntent.getActivity(
                context, 30, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags,
            )
            v.setOnClickPendingIntent(R.id.w_root, open)

            if (near != null && dst != null) {
                v.setViewVisibility(R.id.w_buy, View.VISIBLE)
                v.setTextViewText(R.id.w_buy, "Buy ticket")
                v.setOnClickPendingIntent(
                    R.id.w_buy,
                    PendingIntent.getActivity(context, 31, Notifier.buyIntent(context, near.id, dst.id), flags),
                )
            } else {
                v.setViewVisibility(R.id.w_buy, View.GONE)
            }

            if (ticket != null) {
                v.setViewVisibility(R.id.w_ticket, View.VISIBLE)
                v.setOnClickPendingIntent(
                    R.id.w_ticket,
                    PendingIntent.getActivity(
                        context, 32,
                        Intent(context, MainActivity::class.java)
                            .setAction(MainActivity.ACTION_SHOW_TICKET)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        flags,
                    ),
                )
            } else {
                v.setViewVisibility(R.id.w_ticket, View.GONE)
            }
            return v
        }
    }
}
