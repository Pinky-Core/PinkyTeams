package me.pinkycore.pinkyteams;
import me.pinkycore.pinkyteams.Utils.PAPI; import org.bukkit.configuration.file.YamlConfiguration; import org.junit.jupiter.api.Test;
import java.io.File; import java.nio.file.*; import java.util.*; import java.util.regex.*; import static org.junit.jupiter.api.Assertions.*;
class MetadataAuditTest {
 @Test void everyCodePermissionIsDeclared() throws Exception {
  StringBuilder code=new StringBuilder();try(var files=Files.walk(Path.of("src/main/java"))){for(Path p:files.filter(x->x.toString().endsWith(".java")).toList())code.append(Files.readString(p));}
  Matcher matcher=Pattern.compile("(?:hasPermission|permission)\\(\\\"(pinkyteams\\.[a-z0-9_.-]+)\\\"").matcher(code);
  YamlConfiguration plugin=YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
  Set<String> missing=new TreeSet<>();while(matcher.find())if(!plugin.contains("permissions."+matcher.group(1)))missing.add(matcher.group(1));
  assertTrue(missing.isEmpty(),"Undeclared permissions: "+missing);
 }
 @Test void everyStaticPlaceholderIsDocumented() throws Exception {
  String config=Files.readString(Path.of("src/main/resources/config.yml"));Set<String> missing=new TreeSet<>();
  for(String id:PAPI.SUPPORTED_PLACEHOLDERS)if(!config.contains("%pinkyteams_"+id+"%"))missing.add(id);
  assertTrue(missing.isEmpty(),"Undocumented placeholders: "+missing);
 }
 @Test void publishedYamlLoads(){for(String path:List.of("plugin.yml","config.yml","lang/es.yml","lang/en.yml")){
  YamlConfiguration yaml=YamlConfiguration.loadConfiguration(new File("src/main/resources/"+path));assertTrue(yaml.getKeys(true).size()>5,path+" did not load");}}
}
