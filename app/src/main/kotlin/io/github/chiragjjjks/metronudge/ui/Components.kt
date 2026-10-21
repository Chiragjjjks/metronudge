package io.github.chiragjjjks.metronudge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.chiragjjjks.metronudge.core.CityPack
import io.github.chiragjjjks.metronudge.core.Station
import androidx.compose.material3.MaterialTheme.typography as T

val CardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(12.dp)

@Composable
fun MnCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Int = 18,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Mn.colors.card)
            .border(BorderStroke(1.dp, Mn.colors.border), CardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(padding.dp),
        content = content,
    )
}

enum class ButtonKind { Primary, Secondary, Ghost }

@Composable
fun MnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    large: Boolean = false,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Mn.colors
    val (bg, fg, border) = when (kind) {
        ButtonKind.Primary -> Triple(c.primary, c.primaryForeground, c.primary)
        ButtonKind.Secondary -> Triple(c.card, c.foreground, c.border)
        ButtonKind.Ghost -> Triple(Color.Transparent, c.foreground, Color.Transparent)
    }
    Row(
        modifier
            .height(if (large) 58.dp else 46.dp)
            .clip(ButtonShape)
            .background(bg)
            .border(1.dp, border, ButtonShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        Text(text, style = if (large) T.titleMedium else T.labelLarge, color = fg)
    }
}

@Composable
fun Badge(text: String, color: Color, background: Color, dot: Boolean = false) {
    Row(
        Modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text, style = T.labelMedium, color = color)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), style = T.labelSmall, color = Mn.colors.mutedForeground, modifier = modifier)

@Composable
fun LineDots(station: Station, pack: CityPack, size: Int = 10) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        station.lines.forEach { id ->
            val color = pack.lineById(id)?.color?.let { Color(it) } ?: Color.Gray
            Box(Modifier.size(size.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
fun StationRow(
    station: Station,
    pack: CityPack,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LineDots(station, pack)
        Column(Modifier.weight(1f)) {
            Text(station.name, style = T.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            station.nameKn?.let {
                Text(it, style = T.bodyMedium, color = Mn.colors.mutedForeground, maxLines = 1)
            }
        }
        trailing?.let { Text(it, style = T.labelMedium, color = Mn.colors.mutedForeground) }
    }
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Mn.colors.border))

fun shortName(s: Station): String = when (s.id) {
    "rashtreeya-vidyalaya-road" -> "RV Road"
    "nadaprabhu-kempegowda-station-majestic" -> "Majestic"
    "sir-m-visvesvaraya-stn-central-college" -> "Central College"
    "dr-b-r-ambedkar-station-vidhana-soudha" -> "Vidhana Soudha"
    "krantivira-sangolli-rayanna-railway-station" -> "KSR Railway Stn"
    "mahatma-gandhi-road" -> "MG Road"
    "sri-balagangadharanatha-swamiji-station-hosahalli" -> "Hosahalli"
    else -> s.name
}

fun Modifier.screenPadding() = this.padding(horizontal = 20.dp)
