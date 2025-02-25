## Sandook

Sandook is the db for KRAIL app.

## Migrations

Add migration in .sqm files in `migrations` folder.

### Android

- Update in SandookCallback

### iOS

- Create a new class for after version migration for `SandookMigration`
- Update in `IosSandookDriverFactory` and add `SandookMigrationAfterX` to `getMigrationCallbacks()`
  method.
