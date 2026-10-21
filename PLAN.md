# MetroNudge — Project Plan

> An open-source, on-device app that notices you are approaching a metro station and gets your ticket ready before you reach the gate.
> First city: **Bengaluru (Namma Metro)**. Platforms: **Android first, then iOS**.

**Instructions for the coding agent (Claude Code):** Work through the milestones below in order. Commit after each completed task with a clear conventional-commit message (`feat:`, `fix:`, `docs:`, `chore:`). Do not add any backend, analytics, crash reporting or tracking SDK. When something below is marked **VERIFY**, check it against current official docs before implementing — do not guess. Ask the owner before anything that needs a decision listed under "Open questions".

---

## 1. Problem

The owner commutes by metro and keeps forgetting to buy a ticket until reaching the station gate, then queues or fumbles with the phone. Goal: when the phone detects the owner approaching a station, the ticket purchase should already be set up and need only one tap plus the UPI PIN.

## 2. Hard constraints

1. **No server.** No backend, no database in the cloud, no accounts. All state lives on the phone.
2. **No user data leaves the device.** The only outbound actions are (a) the OS location services, (b) optional map tiles, (c) the user-initiated hand-off to a ticketing channel.
3. **No analytics, ads, Firebase, crash reporters or telemetry SDKs.**
4. **Open source** under Apache-2.0.
5. **Battery-friendly.** Use OS geofencing, never continuous GPS polling in the background.

## 3. Reality check (what the app can and cannot do)

- BMRCL has **no public ticketing API**. QR tickets are sold via the Namma Metro app, the official WhatsApp chatbot (+91 81055 56677), Paytm, and ONDC buyer apps (Namma Yatri, Rapido, Redbus, Tummoc, etc.).
- ONDC buyer integration requires registration and a backend — **out of scope** (violates constraint 1).
- UPI payments always require the user's PIN — **silent auto-purchase is impossible and must not be attempted.**
- The app must **not** automate WhatsApp messages or scrape/automate other apps. It only opens them via deep link; the user taps send/pay.

**So the product is "one-tap", not "zero-tap":** detect station → notify with source + destination pre-filled → tap → purchase channel opens → user pays with PIN → QR ready before the gate.

### 3.1 Better ways the app should also support (research, Oct 2026)
- **NCMC / RuPay contactless cards (best option for daily commuters).** Namma Metro gates accept RuPay NCMC cards on all lines (RBL Bank co-branded Namma Metro NCMC card; many banks' RuPay debit/credit cards carry the NCMC chip). Tap at the gate — no ticket purchase at all. App role in "NCMC mode": no buy prompt; instead a "Tap your card" reminder + optional low-balance top-up reminder that opens the user's UPI app / bank app. **VERIFY** whether the owner's bank card (Kotak) is NCMC-enabled and accepted at BMRCL gates.
- **Namma Metro smart card (CSC).** Can be recharged online (Namma Metro app, WhatsApp bot, Paytm). Same "NCMC mode" style: reminder + top-up hand-off.
- **Mobile QR unlimited passes (1/3/5-day)** sold in the Namma Metro app since Jan 2026 — app can suggest a pass when trip history shows many trips in a short window.
- **Single-journey QR tickets** (the original flow) remain the default for users with no card. QR tickets are valid until end of service that day and can be cancelled same day for a refund.

The app supports a per-user **Fare mode** setting: `qr_ticket` (default) | `ncmc_card` | `smart_card` | `qr_pass`. Notification text and actions depend on the mode.

## 4. User flows

### 4.1 Approaching a station (main flow)
1. User walks within ~150 m of a station (geofence ENTER).
2. App resolves source = that station; destination = per rules in 4.2.
3. Local notification: **"Near Rashtreeya Vidyalaya Road → Rajajinagar. Buy ticket?"** with actions **[Buy]** **[Change destination]** **[Not now]**.
4. **Buy** → opens the user's preferred purchase channel (v0.1: Namma Metro-BMRCL official app — see 5.3).
5. Trip is saved to local history (source, destination, time, channel). No payment data stored.

### 4.2 Destination rules
- **Default destination = Home station** (set during onboarding; owner's is **Rajajinagar**). Changeable anytime in Settings.
- If the source station **is** the Home station (user is leaving home) → do not assume; the notification asks "Where to?" and offers recent/favourite stations first.
- "Change destination" action → opens a quick station picker (search + favourites + recents).
- Assumption: "the source is always the station I'm currently at". (Owner to confirm — see Open questions.)

### 4.3 Suppression rules (avoid spam)
- Don't re-notify for the same station within a cooldown (default 30 min).
- Quiet hours (default off; configurable).
- "Snooze today" action.
- Optional: only notify on selected days (e.g. weekdays).
- Optional: ignore a station (e.g. one the user walks past daily but never uses).

### 4.4 Onboarding
1. Explain what the app does and that no data leaves the phone.
2. Pick city (only Bengaluru for now).
3. Pick Home station.
4. Check the **Namma Metro-BMRCL official app** is installed (offer Play Store / App Store link if not). It is the only purchase channel in v0.1.
5. Optional: "Add tickets to Google Wallet" toggle (Android only, see 5.5).
6. Request permissions in the right order: notifications → location (while in use) → background/"Always" location, each with a plain-language reason screen.

## 5. Architecture

```
┌──────────────────────────── Phone ────────────────────────────┐
│                                                               │
│  OS Geofencing (Android GeofencingClient / iOS CLLocation-    │
│  Manager region monitoring)                                   │
│        │ ENTER event (app woken in background)                │
│        ▼                                                      │
│  GeofenceHandler ──► StationResolver ──► TripPlanner          │
│                       (city pack JSON)    (destination rules, │
│                                            suppression rules) │
│                                                │              │
│                                                ▼              │
│                                       NotificationService     │
│                                                │ user taps    │
│                                                ▼              │
│                                       TripCard screen (in     │
│                                       MetroNudge)             │
│                                                │ "Buy"        │
│                                                ▼              │
│                                       HandoffService ─────────┼──► Namma Metro-BMRCL official app
│                                                               │    (user buys, pays via UPI PIN)
│                                                               │          │ share ticket / screenshot
│                                       TicketWallet ◄──────────┼──────────┘
│                                       (decode QR on-device,   │
│                                        widget, full-screen)   │
│                                                │ optional     │
│                                                ▼              │
│                                       Google Wallet "Add"  ───┼──► Google Wallet (write-only)
│                                       LocalStore (history,    │
│                                       settings, tickets)      │
│                                                               │
│  RegionManager: picks which stations to monitor (nearest N),  │
│  re-registers after reboot / app update / big location change │
└───────────────────────────────────────────────────────────────┘
```

### 5.1 Modules
| Module | Responsibility |
|---|---|
| `city_pack` | Load + validate bundled station data (`data/cities/<city>.json`). |
| `region_manager` | Decide which stations to monitor; register/unregister geofences; handle iOS 20-region limit; re-register after reboot. |
| `geofence_handler` | Receive ENTER events in the background, debounce, call planner. |
| `station_resolver` | Map a fence ID / coordinate to a station; nearest-station lookup (haversine). |
| `trip_planner` | Apply destination rules (4.2) and suppression rules (4.3). |
| `notification_service` | Show actionable local notifications; handle action taps. |
| `handoff_service` | Build and launch deep links for each purchase channel; fallback to store page if app missing. |
| `local_store` | Settings, favourites, recents, trip history (on-device only). |
| `ui` | Onboarding, home screen, station picker, settings, history, permissions help. |

### 5.2 Location strategy
- **Geofences, not 5-minute polling.** Background polling drains battery, is throttled on Android, and is not reliably possible on iOS.
- **Radius ~150 m** (configurable 100–300 m). 10–20 m is not achievable reliably; urban GPS error is 10–50 m and the user needs time to tap and pay.
- Fence centre = station entrance(s) where known, else station centroid. Multiple entrances → multiple fences sharing one station ID.
- **iOS:** max 20 monitored regions per app. RegionManager monitors the 20 nearest stations/entrances and refreshes the set on "significant location change" events.
- **Android:** max 100 geofences per app — all Bengaluru stations fit, but still use the nearest-N approach for consistency.
- Re-register geofences on: boot (`RECEIVE_BOOT_COMPLETED`), app update, permission re-grant, city change.
- Optional foreground "Commute mode" (user-started, persistent notification): higher-accuracy updates for a limited time window. Off by default.

### 5.3 Purchase channels (hand-off)
**Owner decision: the only channel is the "Namma Metro-BMRCL official app" (publisher BMRCL). No WhatsApp, no ONDC apps in v0.1.**

| Channel | Method | Notes |
|---|---|---|
| Namma Metro-BMRCL official app | Launch by Android package name / iOS URL scheme; store fallback | iOS App Store id `6740857835`. **VERIFY** Android package ID (owner can get it from the Play Store "Share" link: `...details?id=<package>`). **VERIFY** whether the app exposes any deep link into the QR-ticket screen; if not, plain launch. |

Keep channels in data (`data/channels/<city>.json`) so contributors can add others later, but ship only this one.

**Cutting steps inside the Namma Metro app** (the owner's main pain):
- MetroNudge cannot read or control another app's data. What it can do: show source/destination prominently on the TripCard and copy the destination station name to the clipboard before launching, so search fields can be pasted.
- **Experimental, Android only, off by default, not for Play Store builds:** an AccessibilityService that pre-selects source/destination in the Namma Metro app. Fragile (breaks on every app update) and Play Store restricts AccessibilityService for non-accessibility use. Build only as an opt-in sideload flavour after M5, and only if the owner wants it.

### 5.4 Payment flow ("Ready to buy?")
- Notification: **"Ready to buy? RV Road → Rajajinagar"** → tap → **MetroNudge opens on a TripCard** (not the other app directly): source, destination, [Change], big **[Buy in Namma Metro]** button, and **[Show my ticket]** if a valid ticket for today already exists.
- Buy → Namma Metro app opens → user selects stations → app launches the UPI app (Google Pay, Kotak…) → user authorises with UPI PIN or biometric where the UPI app supports it.
- When the user returns to MetroNudge, the TripCard shows "Got your ticket? Import it" (share sheet / pick latest screenshot).
- Every notification tap opens MetroNudge first, so the app is always the single entry point.
- **MetroNudge never initiates a UPI payment itself.** A metro ticket order must be created by an authorised seller (BMRCL app, WhatsApp bot, ONDC apps); a raw `upi://pay` intent from our app would send money with no ticket issued. Do not implement UPI intents for ticket purchase.
- Settings: "Preferred UPI app" is informational only (shown in the onboarding help text); the actual UPI picker belongs to the ticket channel.
- Fallback when the user doesn't complete purchase within ~3 min of the notification: one gentle follow-up "Still need a ticket?" (configurable, off by default).

### 5.5 Ticket wallet (store and recall tickets inside MetroNudge)
Goal: open MetroNudge (or its widget) at the gate and see today's QR immediately, no matter which app sold it.
- **Import:** user shares the ticket (screenshot or image) from the Namma Metro app into MetroNudge via the Android share sheet / iOS share extension. Optionally detect new screenshots and offer to import (opt-in).
- **Decode on-device** with a barcode scanner library (e.g. Google ML Kit barcode scanning, which runs on-device; **VERIFY** offline/bundled model option; ZXing as fallback for F-Droid). Store the decoded QR payload + source, destination, purchase time, expiry (end of service day) locally. Keep the original image too.
- **Display:** full-screen QR with max brightness, plus a home-screen widget and a lock-screen-friendly quick action showing today's active ticket. Expired tickets move to history automatically.
- **Gate compatibility:** re-rendering the QR from its decoded payload should scan identically, but this MUST be field-tested at a real gate before relying on it. If re-rendered codes fail, display the original image instead.
- **Google Wallet (Android, owner wants it):**
  - Direction is **one-way: MetroNudge → Google Wallet.** The Google Wallet API only lets an app *issue* passes; there is no API for any app to *read* passes out of a user's Wallet. So MetroNudge can never pull a QR from Wallet; Wallet is a second place to show the ticket.
  - Use the **Google Wallet Android SDK** with a **transit pass (QR code)** or Generic pass. Requires a **Google Wallet API Issuer account** (owner creates it with their Google account) and Google Play services. **VERIFY** whether the Android SDK's `savePasses` path works with unsigned JSON from the app (so no server / no private key in the repo). If a signed JWT or server is required, drop Wallet and keep the in-app wallet — do not embed signing keys in the open-source repo.
  - Pass content: QR payload, source → destination, date, "Valid until end of service today". Mark pass expired the next day.
  - Issuer ID goes in a local, git-ignored config (`app/android/wallet.properties`); forks use their own issuer account.
  - iOS Apple Wallet needs a pass-type certificate to sign `.pkpass` files — same key problem; out of scope for now.
- Tickets are never uploaded or shared; deleting the app deletes them.

## 6. Tech stack

- **Flutter (Dart)** — single codebase for Android + iOS.
- **UI: shadcn style.** Use the Flutter port of shadcn/ui (`shadcn_ui` by nank1ro, now also published as `shad`; or `shadcn_flutter` as alternative). **VERIFY** which is best maintained today and pick one. Use **Dribbble** only for visual inspiration (search "metro ticket app", "transit pass UI", "boarding pass"); do not copy designs. Design goals: dark + light themes, one huge QR screen, TripCard with large tap targets usable while walking, Namma Metro line colours as accents (no BMRCL logo).
- **Geofencing plugin** — evaluate options that wrap the *native* OS geofencing APIs (not a foreground-service polling loop). **VERIFY** maintenance status and iOS/Android support before choosing; if none is solid, write a thin platform channel (Kotlin `GeofencingClient` + Swift `CLLocationManager`).
- `flutter_local_notifications` — actionable notifications. **VERIFY** current API.
- `url_launcher` — deep links.
- Local storage: `sqflite` or `drift` (history) + `shared_preferences` (settings). Pick one approach and stay consistent.
- Map (optional, later): `flutter_map` with OpenStreetMap tiles — tiles are the only network fetch and must be clearly disclosed. App must fully work without the map.
- State management: Riverpod (or keep it simple with provider) — agent's choice, document it.

## 7. Station data

- Source: **OpenStreetMap** (via an Overpass query), licensed **ODbL** — attribute in app About screen and README.
- `tools/fetch_stations.py` (or `.dart`) regenerates `data/cities/bengaluru.json` from Overpass. Not run at app runtime.
- Schema (draft):

```json
{
  "city": "bengaluru",
  "system": "Namma Metro",
  "version": "2026-10-21",
  "source": "OpenStreetMap contributors (ODbL)",
  "lines": [{ "id": "green", "name": "Green Line", "color": "#2E7D32" }],
  "stations": [
    {
      "id": "rajajinagar",
      "name": "Rajajinagar",
      "name_kn": "ರಾಜಾಜಿನಗರ",
      "lines": ["green"],
      "lat": 0.0,
      "lng": 0.0,
      "entrances": [{ "lat": 0.0, "lng": 0.0, "label": "A" }]
    }
  ]
}
```

- Add a unit test that validates every station has an id, name, valid lat/lng inside the Bengaluru bounding box, and at least one line.
- **VERIFY** station list against BMRCL's current network (new lines/stations open regularly).

## 8. Permissions

**Android** (`AndroidManifest.xml`): `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `POST_NOTIFICATIONS` (13+), `RECEIVE_BOOT_COMPLETED`. Request background location in a separate step after foreground location, as Android requires. Play Store requires a background-location declaration + video; prepare text for it in `docs/play-store-background-location.md`.

**iOS** (`Info.plist`): `NSLocationWhenInUseUsageDescription`, `NSLocationAlwaysAndWhenInUseUsageDescription`, background mode `location` only if needed by the chosen approach (region monitoring generally does not require it — **VERIFY**). Notification permission requested at onboarding.

## 9. Repository layout

```
metronudge/
├── app/                      # Flutter app
│   ├── lib/
│   │   ├── core/             # city_pack, station_resolver, trip_planner
│   │   ├── platform/         # region_manager, geofence_handler, notifications, handoff
│   │   ├── data/             # local_store
│   │   └── ui/               # screens + widgets
│   ├── android/
│   ├── ios/
│   └── test/
├── data/
│   ├── cities/bengaluru.json
│   └── channels/bengaluru.json
├── tools/fetch_stations.py
├── docs/
│   ├── architecture.md
│   ├── privacy.md
│   ├── adding-a-city.md
│   └── play-store-background-location.md
├── .github/
│   ├── workflows/ci.yml       # analyze + test on PR
│   ├── workflows/release.yml  # build APK on tag, attach to GitHub Release
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
├── README.md
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── LICENSE                    # Apache-2.0
└── PLAN.md                    # this file
```

## 10. Milestones

### M0 — Repo bootstrap
- [ ] `git init`, add LICENSE (Apache-2.0), README skeleton, CONTRIBUTING, CODE_OF_CONDUCT, `.gitignore` for Flutter.
- [ ] Create Flutter project in `app/` (org id placeholder `io.github.OWNER.metronudge` — replace when owner gives GitHub ID).
- [ ] CI workflow: `flutter analyze` + `flutter test` on push/PR.
- **Done when:** CI is green on an empty app.

### M1 — Station data
- [ ] Overpass fetch script → `data/cities/bengaluru.json`.
- [ ] `city_pack` loader + validation tests.
- [ ] `station_resolver` with nearest-station lookup + tests.
- **Done when:** tests pass; every station has valid coordinates.

### M2 — Onboarding, settings, station picker (no location yet)
- [ ] Onboarding flow (4.4) minus permission prompts.
- [ ] Home station + preferred channel saved locally.
- [ ] Station picker with search (English + Kannada), favourites, recents.
- **Done when:** settings persist across restarts.

### M3 — Notifications + hand-off
- [ ] `notification_service` with actions Buy / Change destination / Not now.
- [ ] TripCard screen opened from every notification tap.
- [ ] `handoff_service` launching the Namma Metro-BMRCL official app with store fallback (+ copy destination to clipboard).

> ⚠️ The original plan text was cut off here. M3 "done when", M4, M5 and the
> "Open questions" section are still to be supplied by the owner.

## Appendix — Target experience (owner's evening commute)

1. **6:00 pm, leaving office.** Phone in pocket, MetroNudge closed. It doesn't need to be open.
2. **6:04 pm, ~150 m from RV Road station.** Phone buzzes: "🚇 Near RV Road → Rajajinagar. Ready to buy?" with **[Buy]** and **[Not now]**.
3. **Tap Buy.** MetroNudge opens a TripCard: RV Road → Rajajinagar, big **"Buy in Namma Metro"** button. Destination name already copied.
4. **Tap the button.** Namma Metro app opens; pick RV Road and Rajajinagar. The one manual step, ~10 seconds.
5. **Pay.** Google Pay or Kotak opens; PIN or fingerprint. QR ticket appears in the Namma Metro app.
6. **Bring the ticket into MetroNudge** via Share → MetroNudge, or "Import latest screenshot" inside MetroNudge. The app reads the QR and saves it as today's ticket. (Not automatic — one app can't read another app's tickets.)
7. **Optional:** **Add to Google Wallet**, so the ticket also sits in Wallet.
8. **6:07 pm, at the gate.** Open the MetroNudge widget or Google Wallet; the QR shows full-screen and bright; scan and walk through.
9. **Next morning,** yesterday's ticket moves to History automatically.

Steps 4 and 6 are the two manual taps left. Everything else is handled.
