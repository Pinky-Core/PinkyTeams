# PinkyTeams API

Add the PinkyTeams JAR as a `provided` dependency and declare `PinkyTeams` in `depend` or `softdepend`.

```java
PinkyTeams plugin = JavaPlugin.getPlugin(PinkyTeams.class);
PinkyTeamsAPI api = plugin.getApi();

api.getPlayerClan(player.getUniqueId()).ifPresent(clan -> {
    api.getClan(clan).ifPresent(snapshot ->
        getLogger().info(snapshot.name() + " has " + snapshot.members().size() + " members"));
});
```

`ClanSnapshot` is immutable. Its member list cannot be modified and represents the state at the moment of the query.

## Events

- `ClanCreateEvent` — cancellable, before creation starts.
- `ClanJoinEvent` — cancellable, after validation and before membership is written.
- `ClanLeaveEvent` — cancellable, before membership is removed.
- `ClanDisbandEvent` — cancellable, before asynchronous deletion starts.
- `ClanBankTransactionEvent` — notification after a successful deposit or withdrawal.
- `ClanCreatedEvent`, `ClanDisbandedEvent`, `ClanJoinedEvent`, `ClanLeftEvent` — emitted only after persistence succeeds.
- `ClanAllianceCreatedEvent` — emitted after an alliance request is accepted and stored.
- `ClanWarStartEvent`, `ClanWarEndEvent` — emitted for accepted and resolved wars, including timed results.

```java
@EventHandler
public void onClanCreate(ClanCreateEvent event) {
    if (event.getClanName().equalsIgnoreCase("blocked")) {
        event.setCancelled(true);
    }
}
```

All events are dispatched on the Bukkit primary thread. API snapshots may touch storage; avoid repeatedly requesting them every tick.
