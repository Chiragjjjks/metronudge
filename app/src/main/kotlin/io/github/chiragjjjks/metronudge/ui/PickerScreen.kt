package io.github.chiragjjjks.metronudge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chiragjjjks.metronudge.core.CityPack
import io.github.chiragjjjks.metronudge.core.Station
import androidx.compose.material3.MaterialTheme.typography as T

@Composable
fun PickerScreen(
    title: String,
    pack: CityPack,
    recents: List<Station>,
    onPick: (Station) -> Unit,
    onBack: () -> Unit,
) {
    val c = Mn.colors
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val results = remember(q) {
        pack.stations
            .filter { q.isEmpty() || it.name.lowercase().contains(q) || shortName(it).lowercase().contains(q) || (it.nameKn?.contains(q) == true) }
            .sortedWith(compareBy({ !it.name.lowercase().startsWith(q) }, { it.name }))
    }
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(Modifier.screenPadding()) {
            Spacer(Modifier.height(4.dp))
            Text("← Back", style = T.labelLarge, color = c.mutedForeground, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
            Text(title, style = T.headlineSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search 83 stations · English or ಕನ್ನಡ") },
                singleLine = true,
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.foreground,
                    unfocusedBorderColor = c.border,
                    focusedContainerColor = c.card,
                    unfocusedContainerColor = c.card,
                ),
            )
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(Modifier.padding(horizontal = 8.dp)) {
            if (q.isEmpty() && recents.isNotEmpty()) {
                item { SectionLabel("Recent", Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)) }
                items(recents, key = { "r-" + it.id }) { StationRow(it, pack, { onPick(it) }) }
                item { SectionLabel("All stations", Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)) }
            }
            items(results, key = { it.id }) { StationRow(it, pack, { onPick(it) }) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
