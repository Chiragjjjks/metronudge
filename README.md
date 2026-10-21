# MetroNudge

An open-source, on-device app that notices you are approaching a metro station
and gets your ticket ready before you reach the gate.

- **First city:** Bengaluru (Namma Metro)
- **Platforms:** Android first, then iOS (Flutter)
- **Licence:** Apache-2.0

> Status: early development. See [PLAN.md](PLAN.md) for the full plan and milestones.

## What it does

1. You walk within ~150 m of a metro station.
2. MetroNudge (using the OS's battery-friendly geofencing) sends a notification:
   *"Near RV Road → Rajajinagar. Ready to buy?"*
3. One tap opens a trip card; another opens the official Namma Metro app to buy.
   You pay with your UPI PIN as usual — MetroNudge never touches payments.
4. Import the QR ticket into MetroNudge and show it full-screen at the gate.

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

## Development

The Flutter app lives in [`app/`](app/).

```sh
cd app
flutter pub get
flutter analyze
flutter test
```

See [CONTRIBUTING.md](CONTRIBUTING.md).
