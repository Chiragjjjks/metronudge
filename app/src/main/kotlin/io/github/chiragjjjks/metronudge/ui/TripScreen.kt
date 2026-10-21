package io.github.chiragjjjks.metronudge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.chiragjjjks.metronudge.core.CityPack
import io.github.chiragjjjks.metronudge.core.Station
import io.github.chiragjjjks.metronudge.platform.Channel
import androidx.compose.material3.MaterialTheme.typography as T

@Composable
fun TripScreen(
    pack: CityPack,
    source: Station?,
    destination: Station?,
    hasTicketToday: Boolean,
    channel: Channel,
    onChangeSource: () -> Unit,
    onChangeDestination: () -> Unit,
    onBuy: () -> Unit,
    onImport: () -> Unit,
    onShowTicket: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Mn.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding().screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("← Back", style = T.labelLarge, color = c.mutedForeground, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
        SectionLabel("Your trip")
        Text(if (destination == null) "Where to?" else "Ready to buy?", style = T.headlineLarge)

        MnCard(padding = 4) {
            Stop("From", source, pack, onChangeSource, isFirst = true)
            Box(Modifier.padding(start = 27.dp).width(2.dp).height(18.dp).background(c.border))
            Stop("To", destination, pack, onChangeDestination, isFirst = false)
        }

        Spacer(Modifier.weight(1f))

        if (hasTicketToday) {
            MnButton("Show today's ticket", onShowTicket, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
        }
        Text(
            when (channel) {
                Channel.WHATSAPP -> "Opens BMRCL's official WhatsApp bot. \"From to To\" is copied so you can paste it when the bot asks. You pay with UPI as usual."
                Channel.APP -> "Opens the official Namma Metro app. \"From to To\" is copied so you can paste it into the search. You pay with UPI as usual."
            },
            style = T.bodyMedium,
            color = c.mutedForeground,
        )
        MnButton(
            if (channel == Channel.WHATSAPP) "Buy on WhatsApp" else "Buy in Namma Metro",
            onBuy,
            Modifier.fillMaxWidth(),
            large = true,
        )
        MnButton("Got the ticket? Import it", onImport, Modifier.fillMaxWidth().padding(bottom = 16.dp), kind = ButtonKind.Ghost)
    }
}

@Composable
private fun Stop(label: String, station: Station?, pack: CityPack, onClick: () -> Unit, isFirst: Boolean) {
    val c = Mn.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val dot = station?.lines?.firstOrNull()?.let { pack.lineById(it)?.color }?.let { Color(it) } ?: c.mutedForeground
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .then(if (isFirst) Modifier.background(dot) else Modifier.border(4.dp, dot, CircleShape)),
        )
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), style = T.labelSmall, color = c.mutedForeground)
            Text(station?.let { shortName(it) } ?: "Choose station", style = T.headlineSmall, color = if (station == null) c.accent else c.foreground)
            station?.nameKn?.let { Text(it, style = T.bodyMedium, color = c.mutedForeground) }
        }
        Text("Change", style = T.labelMedium, color = c.mutedForeground)
    }
}
