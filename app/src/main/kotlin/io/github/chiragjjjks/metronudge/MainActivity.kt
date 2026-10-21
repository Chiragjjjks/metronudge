package io.github.chiragjjjks.metronudge

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.chiragjjjks.metronudge.core.TripPlanner
import io.github.chiragjjjks.metronudge.data.Store
import io.github.chiragjjjks.metronudge.platform.Handoff
import io.github.chiragjjjks.metronudge.platform.Notifier
import io.github.chiragjjjks.metronudge.platform.WatchService
import io.github.chiragjjjks.metronudge.ui.HomeScreen
import io.github.chiragjjjks.metronudge.ui.MetroNudgeTheme
import io.github.chiragjjjks.metronudge.ui.PickerScreen
import io.github.chiragjjjks.metronudge.ui.TicketScreen
import io.github.chiragjjjks.metronudge.ui.TripScreen
import kotlinx.coroutines.flow.MutableStateFlow

private sealed interface Screen {
    data object Home : Screen
    data class Trip(val sourceId: String?, val destinationId: String?) : Screen
    data class Pick(val title: String, val onPicked: (String) -> Screen) : Screen
    data object Ticket : Screen
}

class MainActivity : ComponentActivity() {
    private val incoming = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.createChannels(this)
        // Only on a fresh start: after rotation/theme change the same intent
        // would otherwise be handled again.
        if (savedInstanceState == null) incoming.value = intent
        setContent { MetroNudgeTheme { App() } }
    }

    override fun onResume() {
        super.onResume()
        // Android may have killed the watcher (reboot, app update, low memory).
        // If it's meant to be on, restart it now that we're in the foreground.
        val store = Store.get(this)
        if (store.watching) {
            if (WatchService.hasLocationPermission(this)) WatchService.start(this) else store.watching = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incoming.value = intent
    }

    @Composable
    private fun App() {
        val store = remember { Store.get(this) }
        val pack = store.pack
        // Re-read state whenever prefs change (the watcher writes from the service).
        val tick by remember { store.changes() }.collectAsState(0L)
        var stack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
        val screen = stack.last()

        fun push(s: Screen) { stack = stack + s }
        fun replaceTop(s: Screen) { stack = stack.dropLast(1) + s }
        fun pop() { if (stack.size > 1) stack = stack.dropLast(1) else finish() }

        BackHandler(enabled = stack.size > 1) { pop() }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                importTicket(store, uri)
                stack = listOf(Screen.Home, Screen.Ticket)
            }
        }
        fun pickTicketImage() = importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (WatchService.hasLocationPermission(this)) WatchService.start(this)
            else Toast.makeText(this, "Location is needed to spot stations near you", Toast.LENGTH_LONG).show()
        }

        // Notification taps and "Share → MetroNudge".
        val pending by incoming.collectAsState()
        pending?.let { intent ->
            incoming.value = null
            when (intent.action) {
                ACTION_TRIP -> {
                    Notifier.cancelNudge(this)
                    stack = listOf(
                        Screen.Home,
                        Screen.Trip(intent.getStringExtra(Notifier.EXTRA_SOURCE), intent.getStringExtra(Notifier.EXTRA_DESTINATION)),
                    )
                }
                ACTION_SHOW_TICKET -> if (store.ticket != null) stack = listOf(Screen.Home, Screen.Ticket)
                Intent.ACTION_SEND -> sharedImage(intent)?.let {
                    importTicket(store, it)
                    stack = listOf(Screen.Home, Screen.Ticket)
                }
            }
        }

        @Suppress("UNUSED_EXPRESSION") tick
        val home = store.homeStationId?.let { pack.stationById(it) }

        when (val s = screen) {
            Screen.Home -> HomeScreen(
                pack = pack,
                home = home,
                watching = store.watching,
                status = store.status,
                ticket = store.ticket,
                channel = store.channel,
                onChannel = { store.channel = it },
                channelInstalled = when (store.channel) {
                    io.github.chiragjjjks.metronudge.platform.Channel.WHATSAPP -> Handoff.isWhatsAppInstalled(this)
                    io.github.chiragjjjks.metronudge.platform.Channel.APP -> Handoff.isNammaMetroInstalled(this)
                },
                onToggleWatching = { on ->
                    if (!on) WatchService.stop(this)
                    else {
                        val perms = buildList {
                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                },
                onPickHome = {
                    push(Screen.Pick("Your home station") { id -> store.homeStationId = id; Screen.Home })
                },
                onOpenTicket = { push(Screen.Ticket) },
                onImportTicket = { pickTicketImage() },
                onPlanTrip = {
                    val near = store.status?.stationId
                    push(Screen.Trip(near, home?.id?.takeIf { it != near }))
                },
                onTestNudge = {
                    val src = store.status?.stationId?.let { pack.stationById(it) }
                        ?: pack.stationById("rashtreeya-vidyalaya-road")!!
                    Notifier.showNudge(this, TripPlanner.plan(src, home), 140)
                    Toast.makeText(this, "Test nudge sent — check your notifications", Toast.LENGTH_SHORT).show()
                },
            )

            is Screen.Trip -> {
                val src = s.sourceId?.let { pack.stationById(it) }
                val dst = s.destinationId?.let { pack.stationById(it) }
                val ticket = store.ticket
                TripScreen(
                    pack = pack,
                    source = src,
                    destination = dst,
                    hasTicketToday = ticket?.isValidToday == true,
                    onChangeSource = { push(Screen.Pick("Starting from") { id -> s.copy(sourceId = id) }) },
                    onChangeDestination = { push(Screen.Pick("Going to") { id -> store.addRecent(id); s.copy(destinationId = id) }) },
                    channel = store.channel,
                    onBuy = {
                        if (src != null && dst != null) {
                            store.lastTrip = listOf(src.id, dst.id)
                        }
                        dst?.let { store.addRecent(it.id) }
                        Handoff.buy(this, store.channel, src?.name, dst?.name)
                        if (src != null && dst != null) {
                            Toast.makeText(this, "Copied \"${src.name} to ${dst.name}\" — paste it if asked", Toast.LENGTH_LONG).show()
                        }
                    },
                    onImport = {
                        if (src != null && dst != null) store.lastTrip = listOf(src.id, dst.id)
                        pickTicketImage()
                    },
                    onShowTicket = { push(Screen.Ticket) },
                    onBack = { pop() },
                )
            }

            is Screen.Pick -> PickerScreen(
                title = s.title,
                pack = pack,
                recents = store.recentDestinations.mapNotNull { pack.stationById(it) },
                onPick = { station ->
                    val next = s.onPicked(station.id)
                    stack = stack.dropLast(1)
                    if (next is Screen.Home) Unit else replaceTop(next)
                },
                onBack = { pop() },
            )

            Screen.Ticket -> {
                val t = store.ticket
                if (t == null) pop()
                else TicketScreen(pack, t, onBack = { pop() }, onDelete = { store.deleteTicket(); pop() })
            }
        }
    }

    private fun sharedImage(intent: Intent): Uri? =
        intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)

    private fun importTicket(store: Store, uri: Uri) {
        try {
            val ids = store.lastTrip
            store.importTicket(uri, ids?.getOrNull(0), ids?.getOrNull(1))
            Toast.makeText(this, "Ticket saved for today", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.w("MetroNudge", "Ticket import failed", e)
            Toast.makeText(this, "Couldn't read that image", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val ACTION_TRIP = "io.github.chiragjjjks.metronudge.TRIP"
        const val ACTION_SHOW_TICKET = "io.github.chiragjjjks.metronudge.SHOW_TICKET"
    }
}
