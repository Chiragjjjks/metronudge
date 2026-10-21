package io.github.chiragjjjks.metronudge.platform

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import io.github.chiragjjjks.metronudge.data.Store

/**
 * Invisible step between a notification/widget "Buy" button and the ticket
 * channel. (Android doesn't let notifications open other apps via a
 * background receiver, so this activity does it and closes immediately.)
 */
class BuyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store.get(this)
        val src = intent.getStringExtra(Notifier.EXTRA_SOURCE)?.let { store.pack.stationById(it) }
        val dst = intent.getStringExtra(Notifier.EXTRA_DESTINATION)?.let { store.pack.stationById(it) }
        Notifier.cancelNudge(this)
        dst?.let { store.addRecent(it.id) }
        Handoff.buy(this, store.channel, src?.name, dst?.name)
        if (src != null && dst != null) {
            Toast.makeText(this, "Copied \"${src.name} to ${dst.name}\"", Toast.LENGTH_SHORT).show()
        }
        store.lastTrip = listOfNotNull(src?.id, dst?.id).takeIf { it.size == 2 }
        finish()
    }
}
