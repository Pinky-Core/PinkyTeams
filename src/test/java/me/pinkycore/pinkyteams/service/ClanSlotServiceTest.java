package me.pinkycore.pinkyteams.service;
import me.pinkycore.pinkyteams.Database.StorageProvider;import me.pinkycore.pinkyteams.PinkyTeams;import org.bukkit.configuration.file.YamlConfiguration;import org.junit.jupiter.api.Test;
import java.util.Map;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
class ClanSlotServiceTest{
 @Test void calculatesAndPurchasesUpgrade(){PinkyTeams plugin=mock(PinkyTeams.class);StorageProvider storage=mock(StorageProvider.class);YamlConfiguration config=new YamlConfiguration();
  config.set("clan-slots.enabled",true);config.set("clan-slots.use-points",true);config.set("clan-slots.base-slots",5);config.set("clan-slots.upgrades",java.util.List.of(Map.of("cost",10,"slots",2)));
  when(plugin.getConfig()).thenReturn(config);when(plugin.getStorageProvider()).thenReturn(storage);when(storage.getClanPoints("Alpha")).thenReturn(10);when(storage.getClanSlotUpgrades("Alpha")).thenReturn(0,0,1);
  ClanSlotService service=new ClanSlotService(plugin);assertEquals(5,service.limit("Alpha"));var result=service.buy("Alpha");assertEquals(ClanSlotService.Result.SUCCESS,result.result());assertEquals(7,result.newLimit());
  verify(storage).setClanPoints("Alpha",0);verify(storage).setClanSlotUpgrades("Alpha",1);}
}
