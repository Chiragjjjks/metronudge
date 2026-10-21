package io.github.chiragjjjks.metronudge.platform

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import io.github.chiragjjjks.metronudge.core.ApproachDetector
import io.github.chiragjjjks.metronudge.core.Fix
import io.github.chiragjjjks.metronudge.core.LatLng
import io.github.chiragjjjks.metronudge.core.TripPlanner
import io.github.chiragjjjks.metronudge.data.Store
import io.github.chiragjjjks.metronudge.data.WatchStatus

/**
 * Checks your location every 5 minutes (every minute when you're within
 * ~1 km of a station) and nudges you when you walk to within 150 m of one.
 *
 * Runs as a location foreground service, so it keeps working with the app
 * closed while only needing "while using the app" location permission. The
 * ongoing notification is required by Android for this.
 */
class WatchService : Service(), LocationListener {
    private lateinit var store: Store
    private lateinit var detector: ApproachDetector
    private lateinit var lm: LocationManager
    private var previous: Fix? = null
    private var intervalMillis = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = Store.get(this)
        detector = ApproachDetector(store.resolver)
        lm = getSystemService(LocationManager::class.java)
        Notifier.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !hasLocationPermission(this)) {
            store.watching = false
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val n = Notifier.watchNotification(this, "Checking every 5 minutes")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(Notifier.ID_WATCH, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(Notifier.ID_WATCH, n)
            }
        } catch (e: RuntimeException) {
            // Android refuses to start a location service from the background
            // (e.g. a restart after the process was killed). The user can
            // switch watching back on from the app.
            store.watching = false
            stopSelf()
            return START_NOT_STICKY
        }
        store.watching = true
        requestUpdates(store.status?.nextCheckMillis ?: FIRST_INTERVAL)
        return START_NOT_STICKY
    }

    private fun provider(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.hasProvider(LocationManager.FUSED_PROVIDER)) {
            LocationManager.FUSED_PROVIDER
        } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }

    @Suppress("MissingPermission") // checked in onStartCommand
    private fun requestUpdates(interval: Long) {
        if (interval == intervalMillis) return
        intervalMillis = interval
        lm.removeUpdates(this)
        lm.requestLocationUpdates(provider(), interval, 0f, this, Looper.getMainLooper())
        // Get a first answer quickly instead of waiting a full interval.
        if (previous == null) {
            lm.getLastKnownLocation(provider())?.let { if (System.currentTimeMillis() - it.time < 2 * 60_000) onLocationChanged(it) }
        }
    }

    override fun onLocationChanged(location: Location) {
        val fix = Fix(LatLng(location.latitude, location.longitude), System.currentTimeMillis(), location.accuracy)
        val decision = detector.evaluate(previous, fix, store.lastNudgedAt()) ?: return
        previous = fix

        store.status = WatchStatus(
            stationId = decision.station.id,
            distanceMeters = decision.distanceMeters.toInt(),
            closedInMeters = decision.closedInMeters?.toInt(),
            checkedAtMillis = fix.timeMillis,
            nextCheckMillis = decision.nextCheckMillis,
        )

        if (decision.shouldNudge) {
            store.markNudged(decision.station.id, fix.timeMillis)
            val home = store.homeStationId?.let { store.pack.stationById(it) }
            Notifier.showNudge(this, TripPlanner.plan(decision.station, home), decision.distanceMeters.toInt())
        }

        val every = if (decision.nextCheckMillis < 5 * 60_000L) "every minute" else "every 5 minutes"
        val text = "${decision.station.name}: ${decision.distanceMeters.toInt()} m · checking $every"
        getSystemService(android.app.NotificationManager::class.java)
            .notify(Notifier.ID_WATCH, Notifier.watchNotification(this, text))

        requestUpdates(decision.nextCheckMillis)
    }

    @Deprecated("Required on API < 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit

    override fun onDestroy() {
        lm.removeUpdates(this)
        store.watching = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "io.github.chiragjjjks.metronudge.STOP"
        private const val FIRST_INTERVAL = 60_000L

        fun hasLocationPermission(context: Context) =
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        fun start(context: Context) = context.startForegroundService(Intent(context, WatchService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, WatchService::class.java))
            Store.get(context).watching = false
        }
    }
}
