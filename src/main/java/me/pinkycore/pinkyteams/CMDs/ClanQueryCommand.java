package me.pinkycore.pinkyteams.CMDs;
import me.pinkycore.pinkyteams.PinkyTeams;import me.pinkycore.pinkyteams.Utils.*;import org.bukkit.Bukkit;import org.bukkit.command.CommandSender;import org.bukkit.entity.Player;
import java.util.*;
final class ClanQueryCommand{
 private final PinkyTeams plugin;private final LangManager lang;ClanQueryCommand(PinkyTeams p,LangManager l){plugin=p;lang=l;}
 void stats(CommandSender sender,String clan){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{var storage=plugin.getStorageProvider();List<Member> rows=new ArrayList<>();int kills=0,deaths=0;double kd=0;
  for(String name:storage.getClanMembers(clan)){int k=storage.getPlayerKills(name),d=storage.getPlayerDeaths(name);double ratio=d==0?k:(double)k/d;rows.add(new Member(name,ratio));kills+=k;deaths+=d;kd+=ratio;}
  int totalKills=kills,totalDeaths=deaths;double avg=rows.isEmpty()?0:kd/rows.size();Bukkit.getScheduler().runTask(plugin,()->{sender.sendMessage(MSG.color(lang.getMessage("user.stats_title").replace("{clan}",clan)));
   for(Member row:rows)sender.sendMessage(MSG.color(lang.getMessage("user.stats_member_line").replace("{member}",row.name).replace("{kd}",String.format("%.2f",row.kd))));
   sender.sendMessage(MSG.color(lang.getMessage("user.stats_kills").replace("{kills}",String.valueOf(totalKills))));sender.sendMessage(MSG.color(lang.getMessage("user.stats_deaths").replace("{deaths}",String.valueOf(totalDeaths))));
   sender.sendMessage(MSG.color(lang.getMessage("user.stats_avg_kd").replace("{avgKD}",String.format("%.2f",avg))));sender.sendMessage(MSG.color(lang.getMessage("user.stats_footer")));});});}
 void list(CommandSender sender){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{List<String> clans=new ArrayList<>();for(String clan:plugin.getStorageProvider().getAllClans())if(!plugin.isClanBanned(clan))clans.add(plugin.getStorageProvider().getClanColoredName(clan));
  Bukkit.getScheduler().runTask(plugin,()->{if(clans.isEmpty()){sender.sendMessage(MSG.color(lang.getMessageWithPrefix("user.no_clans")));return;}StringBuilder out=new StringBuilder(MSG.color(lang.getMessageWithPrefix("user.clans_header"))).append('\n');
   for(String clan:clans)out.append(MSG.color("&7- "+clan)).append('\n');out.append(MSG.color(lang.getMessage("user.clans_footer")));sender.sendMessage(out.toString());});});}
 void chat(String clan,Player sender,String message){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{String colored=plugin.getStorageProvider().getColoredClanName(clan);List<String> members=plugin.getStorageProvider().getClanMembers(clan);
  Bukkit.getScheduler().runTask(plugin,()->{for(String name:members){Player recipient=Bukkit.getPlayerExact(name);if(recipient!=null&&recipient.isOnline())recipient.sendMessage(MSG.color(lang.getMessage("user.chat_format")
   .replace("{clan}",colored).replace("{player}",sender.getName()).replace("{message}",message)));}});});}
 private record Member(String name,double kd){}
}
