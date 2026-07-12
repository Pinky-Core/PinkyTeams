package me.pinkycore.pinkyteams.CMDs;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClanHomeCommandTest {
    @Test
    void identifiesDangerousBlocks() {
        assertTrue(ClanHomeCommand.isDangerous(Material.LAVA));
        assertTrue(ClanHomeCommand.isDangerous(Material.SOUL_CAMPFIRE));
        assertTrue(ClanHomeCommand.isDangerous(Material.POWDER_SNOW));
    }

    @Test
    void acceptsOrdinarySolidBlocks() {
        assertFalse(ClanHomeCommand.isDangerous(Material.STONE));
        assertFalse(ClanHomeCommand.isDangerous(Material.GRASS_BLOCK));
    }
}
