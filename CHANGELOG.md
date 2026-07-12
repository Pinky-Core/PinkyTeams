# Changelog

## 1.8.0

- Redesigned the clan GUI as a configurable 54-slot pink control center with live overview, navigation, wars, bank history, refresh actions and sounds.

- Completed the VanguardClans to PinkyTeams/PinkyCore rebrand.
- Added automatic legacy data, permission and PlaceholderAPI compatibility.
- Added a public API, immutable clan snapshots and Bukkit lifecycle events.
- Added atomic clan-bank operations with Vault result checking and rollback.
- Added storage-neutral membership, alliance and ally-friendly-fire services.
- Removed direct JDBC usage from the main clan command.
- Extracted economy and clan-home command handlers.
- Fixed MariaDB cache shadowing and asynchronous Bukkit chat access.
- Made clan creation charges refundable when persistence fails.
- Moved all Vault calls in create/disband flows to the Bukkit primary thread.
- Added Java 17 builds, automated CI and service regression tests.
- Added versioned configuration migration with automatic backups and default merging.
- Added post-persistence lifecycle events for creation, disband, membership and alliances.
- Made create, rename and disband economy flows main-thread safe with verified refunds/rewards.
- Repaired remaining UTF-8 mojibake in published Spanish messages.
- Added persistent UUID-to-name identity tracking and automatic name-change migration.
- Name changes now migrate memberships, leaders, founders, invitations and statistics transactionally.
- Added persistent clan wars with requests, acceptance, surrender, timed matches, kill scores and anti-farming.
- Added war commands and PlaceholderAPI values for opponent, scores and remaining time.
- Added persistent seasons with automatic/manual closure, historical rankings, point resets and console-command rewards.
- Added persistent clan-bank audit logs and `/clan economy history`.
- Added shared asynchronous leaderboard caching and real SQLite/H2 storage contract tests.
- Added build-time audits for permissions, placeholders and published YAML resources.
