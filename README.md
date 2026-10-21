# MetroNudge

An open-source, on-device app that notices you are approaching a metro station
and gets your ticket ready before you reach the gate.

- **First city:** Bengaluru (Namma Metro)
- **Platforms:** Android first (Kotlin + Jetpack Compose), iOS later
- **Licence:** Apache-2.0

> Status: early development. See [PLAN.md](PLAN.md) for the full plan and milestones.

## What it does

1. Turn on **Watch for stations** and allow location and notifications. It checks
   every 5 minutes, or every minute near a station, while the watching notification
   is active. Reopen the app after a restart to resume watching.
2. When successive location checks show you approaching a station within ~150 m,
   MetroNudge offers a trip to your home station.
3. **Buy** on the notification opens your selected channel: BMRCL's WhatsApp bot
   or the Namma Metro app. **Change** opens the trip card to choose another station.
   The route is copied to your clipboard; you choose stations and pay in that channel.
4. Share a ticket image to MetroNudge or use **Import ticket** to save it locally
   and display it full-screen at the gate. Importing an image does not verify that
   the ticket is valid; check the original ticket's date and conditions.
5. Long-press your Android home screen → **Widgets → MetroNudge** to add the nearest
   station, **Buy ticket**, and **Show ticket** shortcuts.

MetroNudge does not automatically book, pay, or retrieve a ticket. The WhatsApp
handoff opens the official chat with an unsent “Hi”; you complete the conversation.

## Privacy

- **No server, no accounts, no analytics, no ads, no crash reporters.**
- Everything (settings, trip history, tickets) stays on your phone.
- The only network activity is OS location services, optional map tiles, and
  apps *you* choose to open.

## Not affiliated

MetroNudge is an independent community project. It is not affiliated with or
endorsed by BMRCL (Bangalore Metro Rail Corporation Ltd).

## Data attribution

Station data © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright),
available under the [ODbL](https://opendatacommons.org/licenses/odbl/).

## Install

Download `MetroNudge.apk` from the
[latest release](../../releases/latest/download/MetroNudge.apk) and open it on
your phone (allow "install unknown apps" for your browser once).

## Development

Native Android: Kotlin, Jetpack Compose and Material 3. You don't need anything
installed locally — GitHub Actions runs the tests, builds the APK and publishes
it as a release on every push to `main`.

To build locally instead, install JDK 17 and the Android SDK (Platform 37,
build-tools 36), then:

```sh
./gradlew testDebugUnitTest lintDebug assembleRelease
```

See [CONTRIBUTING.md](CONTRIBUTING.md).
