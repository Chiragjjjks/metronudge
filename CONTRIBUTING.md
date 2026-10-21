# Contributing to MetroNudge

Thanks for helping! A few ground rules keep the project true to its goals.

## Non-negotiables

- **No backend, no accounts, no cloud storage.** All state lives on the device.
- **No analytics, ads, Firebase, crash reporting or telemetry SDKs** — including
  transitive dependencies. PRs adding them will be closed.
- **No automating other apps** (no scraping, no auto-sending WhatsApp messages,
  no initiating UPI payments). MetroNudge only opens other apps; the user acts.
- **Battery first:** use OS geofencing, never background GPS polling.

## Workflow

1. Open an issue describing the change (bugs, new cities, new features).
2. Fork, branch from `main`, and keep PRs small and focused.
3. Use [Conventional Commits](https://www.conventionalcommits.org/):
   `feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`.
4. Before pushing:
   ```sh
   cd app
   dart format .
   flutter analyze
   flutter test
   ```
5. CI must be green before review.

## Adding a city

See `docs/adding-a-city.md` (coming in a later milestone). Station data comes
from OpenStreetMap (ODbL) and is regenerated with `tools/fetch_stations.py`.

## Licence

By contributing you agree that your contributions are licensed under the
Apache License 2.0 (see [LICENSE](LICENSE)).
