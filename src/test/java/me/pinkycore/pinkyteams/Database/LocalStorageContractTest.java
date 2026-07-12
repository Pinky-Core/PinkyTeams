package me.pinkycore.pinkyteams.Database;

import me.pinkycore.pinkyteams.PinkyTeams; import me.pinkycore.pinkyteams.Utils.LangManager;
import org.bukkit.configuration.file.YamlConfiguration; import org.junit.jupiter.api.*; import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Field; import java.nio.file.Path; import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;

class LocalStorageContractTest {
 @TempDir Path dir;
 @BeforeEach void pluginInstance() throws Exception {
  PinkyTeams plugin=mock(PinkyTeams.class);when(plugin.getDataFolder()).thenReturn(dir.toFile());
  when(plugin.getLangManager()).thenReturn(mock(LangManager.class));when(plugin.getLogger()).thenReturn(Logger.getLogger("storage-test"));
  Field field=PinkyTeams.class.getDeclaredField("instance");field.setAccessible(true);field.set(null,plugin);
 }
 @AfterEach void clear() throws Exception {Field field=PinkyTeams.class.getDeclaredField("instance");field.setAccessible(true);field.set(null,null);}
 @Test void sqliteContract() throws Exception {verifyContract(new SQLiteStorageProvider(new YamlConfiguration()));}
 @Test void h2Contract() throws Exception {verifyContract(new H2StorageProvider(new YamlConfiguration()));}
 private void verifyContract(StorageProvider storage) throws Exception {
  storage.initialize();
  try{
   storage.createClan("Alpha","&dAlpha","Founder","Leader",100,"private");
   assertTrue(storage.clanExists("Alpha"));assertEquals("Alpha",storage.getPlayerClan("Leader"));assertEquals(100,storage.getClanMoney("Alpha"),0.001);
   storage.addPlayerToClan("Member","Alpha");assertTrue(storage.getClanMembers("Alpha").contains("Member"));
   storage.setPlayerRole("Alpha","Member","co-leader");assertEquals("co-leader",storage.getPlayerRole("Alpha","Member"));
   storage.addClanInvite("Alpha","Invitee");assertTrue(storage.isPlayerInvitedToClan("Invitee","Alpha"));
   storage.createClan("Beta","Beta","B","B",0,"public");storage.createAlliance("Alpha","Beta",false);
   assertTrue(storage.areClansAllied("Alpha","Beta"));storage.setFriendlyFireAlliesEnabled("Alpha",true);assertTrue(storage.isFriendlyFireAlliesEnabled("Alpha"));
   storage.setClanPoints("Alpha",25);storage.addClanPoints("Alpha",5);assertEquals(30,storage.getClanPoints("Alpha"));
   storage.renamePlayerIdentity("Member","Renamed");assertTrue(storage.getClanMembers("Alpha").contains("Renamed"));
   storage.deleteClan("Alpha");assertFalse(storage.clanExists("Alpha"));
  } finally {storage.close();}
 }
}
