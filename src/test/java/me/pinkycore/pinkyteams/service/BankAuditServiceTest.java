package me.pinkycore.pinkyteams.service;
import me.pinkycore.pinkyteams.PinkyTeams; import org.junit.jupiter.api.Test; import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path; import java.util.logging.Logger; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class BankAuditServiceTest{
 @TempDir Path dir;
 @Test void recordsAndFiltersTransactions(){
  PinkyTeams plugin=mock(PinkyTeams.class);when(plugin.getDataFolder()).thenReturn(dir.toFile());when(plugin.getLogger()).thenReturn(Logger.getLogger("audit"));
  BankAuditService service=new BankAuditService(plugin);service.record("Alpha","Pinky",BankAuditService.Type.DEPOSIT,10,110);
  service.record("Beta","Other",BankAuditService.Type.WITHDRAW,5,20);
  var entries=service.recent("alpha",10);assertEquals(1,entries.size());assertEquals("Pinky",entries.get(0).actor());assertEquals(110,entries.get(0).balance());
 }
}
