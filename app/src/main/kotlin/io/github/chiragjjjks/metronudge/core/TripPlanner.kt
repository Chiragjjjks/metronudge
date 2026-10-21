package io.github.chiragjjjks.metronudge.core

data class Trip(val source: Station, val destination: Station?)

/** Destination rules: go home, unless you're already at the home station. */
object TripPlanner {
    fun plan(source: Station, homeStation: Station?): Trip =
        Trip(source, destination = homeStation?.takeIf { it.id != source.id })
}
