package me.pinkycore.pinkyteams.Utils;

import me.pinkycore.pinkyteams.PinkyTeams;
import org.jetbrains.annotations.NotNull;

/** Temporary PlaceholderAPI namespace retained for existing scoreboards and TAB configurations. */
public final class LegacyPAPI extends PAPI {
    public LegacyPAPI(PinkyTeams plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vanguardclans";
    }
}
