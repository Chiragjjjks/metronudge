package io.github.chiragjjjks.metronudge.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.chiragjjjks.metronudge.core.CityPack
import io.github.chiragjjjks.metronudge.core.StationResolver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** What the watcher last saw, shown on the home screen. */
data class WatchStatus(
    val stationId: String,
    val distanceMeters: Int,
    val closedInMeters: Int?,
    val checkedAtMillis: Long,
    val nextCheckMillis: Long,
)

/** A ticket imported from the Namma Metro app (screenshot), valid for one day. */
data class Ticket(
    val imagePath: String,
    val date: LocalDate,
    val sourceId: String?,
    val destinationId: String?,
) {
    val isValidToday: Boolean get() = date == LocalDate.now()
}

/** All app state. On-device only: SharedPreferences + files in app storage. */
class Store private constructor(private val context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("metronudge", Context.MODE_PRIVATE)

    val pack: CityPack by lazy {
        CityPack.parse(context.assets.open("cities/bengaluru.json").bufferedReader().use { it.readText() })
    }
    val resolver: StationResolver by lazy { StationResolver(pack) }

    var homeStationId: String?
        get() = prefs.getString(KEY_HOME, null)
        set(v) = prefs.edit { putString(KEY_HOME, v) }

    var watching: Boolean
        get() = prefs.getBoolean(KEY_WATCHING, false)
        set(v) = prefs.edit { putBoolean(KEY_WATCHING, v) }

    var recentDestinations: List<String>
        get() = prefs.getString(KEY_RECENTS, "")!!.split(',').filter { it.isNotBlank() }
        set(v) = prefs.edit { putString(KEY_RECENTS, v.distinct().take(5).joinToString(",")) }

    fun addRecent(stationId: String) {
        recentDestinations = listOf(stationId) + recentDestinations
    }

    var status: WatchStatus?
        get() = prefs.getString(KEY_STATUS, null)?.let {
            val o = JSONObject(it)
            WatchStatus(
                stationId = o.getString("s"),
                distanceMeters = o.getInt("d"),
                closedInMeters = if (o.has("c")) o.getInt("c") else null,
                checkedAtMillis = o.getLong("t"),
                nextCheckMillis = o.getLong("n"),
            )
        }
        set(v) = prefs.edit {
            if (v == null) remove(KEY_STATUS)
            else putString(
                KEY_STATUS,
                JSONObject().apply {
                    put("s", v.stationId)
                    put("d", v.distanceMeters)
                    v.closedInMeters?.let { put("c", it) }
                    put("t", v.checkedAtMillis)
                    put("n", v.nextCheckMillis)
                }.toString(),
            )
        }

    fun lastNudgedAt(): Map<String, Long> =
        prefs.getString(KEY_NUDGED, null)?.let { s ->
            val o = JSONObject(s)
            o.keys().asSequence().associateWith { o.getLong(it) }
        } ?: emptyMap()

    fun markNudged(stationId: String, at: Long) {
        val o = JSONObject(prefs.getString(KEY_NUDGED, "{}")!!)
        o.put(stationId, at)
        prefs.edit { putString(KEY_NUDGED, o.toString()) }
    }

    // --- Ticket wallet -----------------------------------------------------

    private val ticketDir get() = File(context.filesDir, "tickets").apply { mkdirs() }

    var ticket: Ticket?
        get() = prefs.getString(KEY_TICKET, null)?.let {
            val o = JSONObject(it)
            Ticket(
                imagePath = o.getString("img"),
                date = LocalDate.parse(o.getString("date")),
                sourceId = o.optString("src").ifBlank { null },
                destinationId = o.optString("dst").ifBlank { null },
            )
        }?.takeIf { File(it.imagePath).exists() }
        private set(v) = prefs.edit {
            if (v == null) remove(KEY_TICKET)
            else putString(
                KEY_TICKET,
                JSONObject().apply {
                    put("img", v.imagePath)
                    put("date", v.date.toString())
                    put("src", v.sourceId ?: "")
                    put("dst", v.destinationId ?: "")
                }.toString(),
            )
        }

    /** Copies the shared/picked image into app storage as today's ticket. */
    fun importTicket(uri: android.net.Uri, sourceId: String?, destinationId: String?): Ticket {
        val date = LocalDate.now()
        val file = File(ticketDir, "ticket-$date-${System.currentTimeMillis()}.img")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        ticket?.let { old -> if (old.imagePath != file.path) File(old.imagePath).delete() }
        return Ticket(file.path, date, sourceId, destinationId).also { ticket = it }
    }

    fun deleteTicket() {
        ticket?.let { File(it.imagePath).delete() }
        ticket = null
    }

    /** Emits whenever any stored value changes, so the UI can refresh. */
    fun changes(): Flow<Unit> = callbackFlow {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        prefs.registerOnSharedPreferenceChangeListener(l)
        trySend(Unit)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(l) }
    }

    companion object {
        private const val KEY_HOME = "home_station"
        private const val KEY_WATCHING = "watching"
        private const val KEY_RECENTS = "recent_destinations"
        private const val KEY_STATUS = "watch_status"
        private const val KEY_NUDGED = "last_nudged"
        private const val KEY_TICKET = "ticket"

        @Volatile private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }
}
