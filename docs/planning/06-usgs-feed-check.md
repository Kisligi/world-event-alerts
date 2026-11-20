# 06 — USGS earthquake feed check (2026-11-20 ~12:55 UTC, before writing the M2 adapter)

Goal: confirm the real format of the USGS feed instead of writing the adapter from memory (ground rule 9).

**How:** Claude's development environment blocks direct HTTP access to earthquake.usgs.gov, so the pages were read through a web-reading tool:
- Documentation: https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php
- Live sample: https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson

## Findings

- `FeatureCollection` with `metadata` (`generated`, `url`, `title`, `api`, `count`, `status`), `bbox` and `features`.
- Each feature has a top-level `id` (string), `properties`, and `geometry` (`Point`, coordinates in the order **longitude, latitude, depth**).
- Properties used by the adapter, with the documented type:
  - `mag` (decimal)
  - `place` (string)
  - `time` (epoch milliseconds)
  - `url` (string)
  - `tsunami` (integer)
  - `type` (string, e.g. `earthquake`)
- `title` (e.g. `"M 4.9 - 54 km W of Madang, Papua New Guinea"`) appears in the live data but **not** in the documented property list. The adapter uses it if present and builds its own title otherwise.
- Several properties documented as numbers or strings are `null` in live data (`tz`, `mmi`, `alert`, sometimes `felt`). The adapter ignores unknown and null fields.
- Feed URL pattern: `https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/{significant|4.5|2.5|1.0|all}_{hour|day|week|month}.geojson`, "updated every minute".
- The documentation page does not mention any API key or account, and the sample was fetched without one.

## Not verified

- The meaning of each property: the glossary page linked from the docs returned the docs page itself.
- Live behaviour from inside Docker: the adapter could not be run against the real feed from Claude's environment because of the network block. It is tested against a fixture built from the two real features below.

## Sample used for the test fixture

Two real features from the 4.5_week feed, reconstructed field by field (the reading tool could not return raw bytes):

| id | mag | place | time (epoch ms) | coordinates |
|----|-----|-------|-----------------|-------------|
| us6000uc6c | 4.9 | 54 km W of Madang, Papua New Guinea | 1795151865106 | 145.2974, -5.219, 97.583 |
| us6000uc5s | 4.8 | 24 km ESE of Calingasta, Argentina | 1795143438070 | -69.1883, -31.4302, 113.86 |
