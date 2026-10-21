package io.github.chiragjjjks.metronudge.ui

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.chiragjjjks.metronudge.core.CityPack
import io.github.chiragjjjks.metronudge.core.Station
import io.github.chiragjjjks.metronudge.data.Ticket
import io.github.chiragjjjks.metronudge.data.WatchStatus
import io.github.chiragjjjks.metronudge.platform.Channel
import androidx.compose.material3.MaterialTheme.typography as T

@Composable
fun HomeScreen(
    pack: CityPack,
    home: Station?,
    watching: Boolean,
    status: WatchStatus?,
    ticket: Ticket?,
    channel: Channel,
    onChannel: (Channel) -> Unit,
    channelInstalled: Boolean,
    onToggleWatching: (Boolean) -> Unit,
    onPickHome: () -> Unit,
    onOpenTicket: () -> Unit,
    onImportTicket: () -> Unit,
    onPlanTrip: () -> Unit,
    onTestNudge: () -> Unit,
) {
    val c = Mn.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MetroNudge", style = T.headlineLarge)
                Text("Your ticket, ready before the gate.", style = T.bodyMedium, color = c.mutedForeground)
            }
            if (watching) Badge("Live", c.success, c.successSoft, dot = true)
            else Badge("Paused", c.mutedForeground, c.muted, dot = true)
        }
        Spacer(Modifier.height(2.dp))

        // --- Today's ticket -------------------------------------------------
        if (ticket != null && ticket.isValidToday) {
            TicketCard(pack, ticket, onOpenTicket)
        }

        // --- Watching -------------------------------------------------------
        MnCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Watch for stations", style = T.titleMedium)
                    Text(
                        if (watching) "Checking every 5 min · every 1 min near a station"
                        else "Turn on to get a nudge 150 m before a station",
                        style = T.bodyMedium,
                        color = c.mutedForeground,
                    )
                }
                Switch(
                    checked = watching,
                    onCheckedChange = onToggleWatching,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = c.primary,
                        checkedThumbColor = c.primaryForeground,
                        uncheckedTrackColor = c.muted,
                        uncheckedBorderColor = c.border,
                        uncheckedThumbColor = c.mutedForeground,
                    ),
                )
            }
            if (watching && status != null) {
                val s = pack.stationById(status.stationId)
                if (s != null) {
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Nearest station")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineDots(s, pack)
                        Spacer(Modifier.size(10.dp))
                        Text(shortName(s), style = T.headlineSmall, modifier = Modifier.weight(1f))
                        Text(formatDistance(status.distanceMeters), style = T.headlineSmall, color = c.accent)
                    }
                    Spacer(Modifier.height(6.dp))
                    val trend = when {
                        status.closedInMeters == null -> "First check"
                        status.closedInMeters >= 15 -> "Getting closer"
                        status.closedInMeters <= -15 -> "Moving away"
                        else -> "Not moving"
                    }
                    Text(
                        "$trend · checked ${checkedAgo(status.checkedAtMillis)}",
                        style = T.bodyMedium,
                        color = c.mutedForeground,
                    )
                }
            } else if (watching) {
                Spacer(Modifier.height(12.dp))
                Text("Waiting for your first location…", style = T.bodyMedium, color = c.mutedForeground)
            }
        }

        // --- Home station ---------------------------------------------------
        MnCard(onClick = onPickHome) {
            SectionLabel("Home station · default destination")
            Spacer(Modifier.height(10.dp))
            if (home == null) {
                Text("Choose your home station", style = T.titleMedium, color = c.accent)
                Text("Nudges will offer a ticket to this station.", style = T.bodyMedium, color = c.mutedForeground)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineDots(home, pack)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(home.name, style = T.titleMedium)
                        home.nameKn?.let { Text(it, style = T.bodyMedium, color = c.mutedForeground) }
                    }
                    Text("Change", style = T.labelMedium, color = c.mutedForeground)
                }
            }
        }

        // --- Actions --------------------------------------------------------
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MnButton("Plan a trip", onPlanTrip, Modifier.weight(1f), kind = ButtonKind.Secondary)
            MnButton("Import ticket", onImportTicket, Modifier.weight(1f), kind = ButtonKind.Secondary)
        }

        // --- Where to buy -----------------------------------------------------
        MnCard {
            SectionLabel("Buy tickets with")
            Spacer(Modifier.height(10.dp))
            Segmented(Channel.entries, channel, { it.label }, onChannel)
            Spacer(Modifier.height(10.dp))
            Text(
                when (channel) {
                    Channel.WHATSAPP -> "Opens BMRCL's official WhatsApp bot (+91 81055 56677). Your stations are copied so you can paste them; you send the messages and pay."
                    Channel.APP -> "Opens BMRCL's official Namma Metro app. Your stations are copied so you can paste them; you pay there as usual."
                },
                style = T.bodyMedium,
                color = c.mutedForeground,
            )
            if (!channelInstalled) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${if (channel == Channel.WHATSAPP) "WhatsApp" else "Namma Metro"} isn't installed on this phone.",
                    style = T.bodyMedium,
                    color = c.warning,
                )
            }
        }

        MnCard {
            Text("Add the widget", style = T.titleMedium)
            Text(
                "Long-press your home screen → Widgets → MetroNudge. It shows the nearest station, a Buy button and today's ticket.",
                style = T.bodyMedium,
                color = c.mutedForeground,
            )
        }

        MnButton("Send a test nudge", onTestNudge, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost)

        Text(
            "Everything stays on this phone. No account, no server, no tracking.\nStation data © OpenStreetMap contributors (ODbL).",
            style = T.labelMedium,
            color = c.mutedForeground,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun TicketCard(pack: CityPack, ticket: Ticket, onOpen: () -> Unit) {
    val c = Mn.colors
    val bmp = rememberTicketBitmap(ticket.imagePath, maxPx = 320)
    MnCard(onClick = onOpen, padding = 0) {
        Row(
            Modifier.background(c.accentSoft).fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Today's ticket")
                Spacer(Modifier.height(6.dp))
                val src = ticket.sourceId?.let { pack.stationById(it) }
                val dst = ticket.destinationId?.let { pack.stationById(it) }
                Text(
                    if (src != null && dst != null) "${shortName(src)} → ${shortName(dst)}" else "Namma Metro QR",
                    style = T.titleMedium,
                )
                Text("Valid until end of service · tap to show", style = T.bodyMedium, color = c.mutedForeground)
            }
            if (bmp != null) {
                Image(
                    bmp.asImageBitmap(),
                    contentDescription = "Ticket",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}

private fun checkedAgo(at: Long): String =
    if (System.currentTimeMillis() - at < 60_000) "just now"
    else DateUtils.getRelativeTimeSpanString(at).toString()

fun formatDistance(m: Int): String = if (m < 1000) "$m m" else "%.1f km".format(m / 1000.0)

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val c = Mn.colors
    Row(
        Modifier.fillMaxWidth().clip(ButtonShape).background(c.muted).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.card else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = T.labelLarge, color = if (on) c.foreground else c.mutedForeground)
            }
        }
    }
}
