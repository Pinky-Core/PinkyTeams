package me.pinkycore.pinkyteams.CMDs;
import me.pinkycore.pinkyteams.PinkyTeams;import me.pinkycore.pinkyteams.Utils.*;import org.bukkit.command.CommandSender;import java.util.Map;
final class ClanReportCommand{private final PinkyTeams plugin;private final LangManager lang;ClanReportCommand(PinkyTeams p,LangManager l){plugin=p;lang=l;}
 void execute(CommandSender sender,String clan,String reason){if(reason==null||reason.isBlank()){send(sender,"user.report_no_reason");return;}if(!plugin.getStorageProvider().clanExists(clan)){send(sender,"user.report_clan_not_exist");return;}
  for(Map<String,Object> report:plugin.getStorageProvider().getClanReports(clan))if(reason.equals(report.get("reason"))){send(sender,"user.report_already_sent");return;}
  plugin.getStorageProvider().addClanReport(clan,sender.getName(),reason,System.currentTimeMillis());sender.sendMessage(MSG.color(lang.getMessageWithPrefix("user.report_success").replace("{clan}",clan).replace("{reason}",reason)));}
 private void send(CommandSender s,String key){s.sendMessage(MSG.color(lang.getMessageWithPrefix(key)));}}
