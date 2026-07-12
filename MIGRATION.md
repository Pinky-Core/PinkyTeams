# Migrating from VanguardClans to PinkyTeams

PinkyTeams includes a one-time, non-destructive migration for existing installations.

## Automatic data migration

When `plugins/PinkyTeams` has no user data and `plugins/VanguardClans` exists, PinkyTeams copies the old directory contents into its new directory. It does not delete or modify the original directory. A `.vanguardclans-migrated` marker prevents the migration from running again.

Review the server log after the first startup and keep the old folder as a backup until the installation has been verified.

## Permissions

The canonical namespace is now `pinkyteams.*`. Legacy `vanguardclans.*` permissions are declared as compatibility aliases, so existing LuckPerms groups continue to grant their equivalent PinkyTeams nodes.

Migrate permission groups when convenient. The legacy aliases are transitional and can be removed in a future major release.

## PlaceholderAPI

Use `%pinkyteams_*%` for all new configurations. The old `%vanguardclans_*%` namespace remains enabled by default through:

```yaml
compatibility:
  legacy-placeholders: true
```

Disable it after updating TAB, scoreboards, holograms and chat formats. The complete placeholder list is documented at the top of `config.yml`.

## SQL installations

Table names are unchanged, so existing MySQL and MariaDB data remains compatible. Connection settings copied from the old configuration continue to point to the same database.
