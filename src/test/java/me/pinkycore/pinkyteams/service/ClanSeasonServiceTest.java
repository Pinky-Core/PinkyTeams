package me.pinkycore.pinkyteams.service;
import me.pinkycore.pinkyteams.Database.StorageProvider; import me.pinkycore.pinkyteams.PinkyTeams;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path; import java.util.Set; import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class ClanSeasonServiceTest {
 @TempDir Path dir;
 @Test void seasonPersistsSnapshotAndResetsPoints(){
  PinkyTeams plugin=mock(PinkyTeams.class); StorageProvider storage=mock(StorageProvider.class);
  when(plugin.getDataFolder()).thenReturn(dir.toFile()); when(plugin.getStorageProvider()).thenReturn(storage);
  when(plugin.getLogger()).thenReturn(Logger.getLogger("season-test")); when(storage.getAllClans()).thenReturn(Set.of("Alpha"));
  when(storage.getClanMembers("Alpha")).thenReturn(java.util.List.of()); when(storage.getClanPoints("Alpha")).thenReturn(42);
  ClanSeasonService service=new ClanSeasonService(plugin);
  assertTrue(service.start("S1",86400)); assertTrue(service.isActive()); assertEquals("S1",service.name());
  var result=service.end(true); assertTrue(result.isPresent()); assertFalse(service.isActive());
  assertEquals("Alpha",result.get().ranking().get(0).getClanName()); verify(storage).setClanPoints("Alpha",0);
 }
 @Test void cannotStartTwoSeasons(){
  PinkyTeams plugin=mock(PinkyTeams.class); when(plugin.getDataFolder()).thenReturn(dir.toFile());
  ClanSeasonService service=new ClanSeasonService(plugin); assertTrue(service.start("One",10)); assertFalse(service.start("Two",10));
 }
}
