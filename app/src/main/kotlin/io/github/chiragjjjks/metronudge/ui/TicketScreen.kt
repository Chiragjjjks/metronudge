package io.github.chiragjjjks.metronudge.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.unit.dp
import io.github.chiragjjjks.metronudge.core.CityPack
import io.github.chiragjjjks.metronudge.data.Ticket
import androidx.compose.material3.MaterialTheme.typography as T

/** Full-screen ticket at max brightness, for the gate scanner. */
@Composable
fun TicketScreen(pack: CityPack, ticket: Ticket, onBack: () -> Unit, onDelete: () -> Unit) {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val w = activity?.window ?: return@DisposableEffect onDispose { }
        val before = w.attributes.screenBrightness
        w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            w.attributes = w.attributes.apply { screenBrightness = before }
            w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val bmp = rememberTicketBitmap(ticket.imagePath, maxPx = 2000)
    val src = ticket.sourceId?.let { pack.stationById(it) }
    val dst = ticket.destinationId?.let { pack.stationById(it) }
    Column(
        Modifier.fillMaxSize().background(Color(0xFF09090B)).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("← Back", style = T.labelLarge, color = Color(0xFFA1A1AA), modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
            Spacer(Modifier.weight(1f))
            Text("Delete", style = T.labelLarge, color = Color(0xFFA1A1AA), modifier = Modifier.clickable(onClick = onDelete).padding(vertical = 8.dp))
        }
        Text(
            if (src != null && dst != null) "${shortName(src)} → ${shortName(dst)}" else "Namma Metro ticket",
            style = T.headlineSmall,
            color = Color.White,
        )
        Text(
            if (ticket.isValidToday) "Valid today until end of service" else "Expired · ${ticket.date}",
            style = T.bodyMedium,
            color = if (ticket.isValidToday) Color(0xFF4ADE80) else Color(0xFFFBBF24),
        )
        if (bmp != null) {
            Image(
                bmp.asImageBitmap(),
                contentDescription = "Ticket QR",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun rememberTicketBitmap(path: String, maxPx: Int): Bitmap? = remember(path, maxPx) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxPx || bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
