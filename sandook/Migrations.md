# Sandook migrations

Sandook is KRAIL's SQLDelight database. Its schema migrations live in
`src/commonMain/sqldelight/migrations/`.

## Standard schema migration

1. Add the next numbered `.sqm` file with the required SQL.
2. Update the current `.sq` schema so it represents the resulting database.
3. Verify the migration with the Sandook compile/test checks.

That is all that is required for normal table, column, index, and SQL data changes.
SQLDelight generates `KrailSandook.Schema.migrate(...)` from the `.sqm` files and both
platform drivers invoke it. Existing installations run each missing `.sqm` migration in
order; fresh installations create the current schema directly.

Do **not** add a matching iOS `SandookMigrationAfterX` class or `AfterVersion(X)` entry
solely because a new `.sqm` file exists. The callbacks are not migration registration:
they are optional hooks that run only *after* SQLDelight has completed the migration to
that version. The no-op callbacks for versions 7 and 8 are historical and are not a
template for later migrations.

## When an iOS callback is needed

Add `AfterVersion(X)` in `IosSandookDriverFactory` only when an upgrade needs imperative
Kotlin/Native work that cannot be expressed in the `.sqm` SQL, and keep that work in a
dedicated `SandookMigrationAfterX` implementation. The callback runs after the SQL for
version `X`, so it may depend on the migrated schema.

If the work can be represented in SQL, keep it in the `.sqm` file instead. No Android
callback change is needed for a standard SQLDelight migration: `SandookCallback` delegates
to SQLDelight's schema migration via `super.onUpgrade(...)`.

## Saved-trip identity invariant

A saved trip is identified by its ordered `(fromStopId, toStopId)` pair. The reverse
direction is a different trip. Its internal `tripId` is the canonical string
`fromStopId->toStopId`; it is a storage and UI key, not user-facing text and not a value
that callers should parse.

This contract crosses SQL and Kotlin, so keep all of these layers aligned:

- `Trip.tripId` supplies the Compose list key and the IDs used by select, delete, and reorder.
- `RealSandook.insertOrReplaceTrip` derives the ID from the two stop IDs instead of trusting
  a caller-provided serialization. `FakeSandook` must behave the same way.
- `SavedTrip` enforces both `CHECK (tripId = fromStopId || '->' || toStopId)` and
  `UNIQUE (fromStopId, toStopId)`. Do not remove either constraint: the check keeps the ID
  canonical, while the unique constraint prevents two IDs from representing the same pair.

Never construct a saved-trip ID by concatenating stop IDs without a separator. That made
legacy and current database rows resolve to the same Compose key, and delete operations
could target a different ID from the stored row.

If this identity format changes, update the current `.sq` schema, `Trip`, both Sandook
implementations, and persisted preference keys together. Add a numbered migration that
canonicalizes existing rows and deterministically resolves duplicates, plus an executable
`KrailSandook.Schema.migrate(...)` regression test containing the legacy row shapes. Preserve
stop names, timestamps, and sort order unless the product requirement explicitly says otherwise.
