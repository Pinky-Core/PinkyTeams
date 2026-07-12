package me.pinkycore.pinkyteams.CMDs;
import me.pinkycore.pinkyteams.PinkyTeams;import me.pinkycore.pinkyteams.Utils.*;import me.pinkycore.pinkyteams.api.event.ClanAllianceCreatedEvent;import me.pinkycore.pinkyteams.service.ClanAllianceService;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;
final class ClanAllianceCommand{
 private final PinkyTeams plugin;private final LangManager lang;private final ClanEconomyCommand.PermissionCheck permission;
 ClanAllianceCommand(PinkyTeams p,LangManager l,ClanEconomyCommand.PermissionCheck c){plugin=p;lang=l;permission=c;}
 void execute(Player player,String clan,String[] args){if(clan==null){send(player,"user.no_clan");return;}if(!permission.test(player,clan,ClanPermission.ALLY))return;
  if(args.length<2){send(player,"command.ally_usage");return;}String action=args[1].toLowerCase();
  if(action.equals("ff")){if(args.length<3){send(player,"command.allyff_usage");return;}friendlyFire(player,clan,args[2]);return;}
  if(args.length<3){send(player,"command.ally_"+action+"_usage");return;}String other=args[2];ClanAllianceService.Result result=switch(action){
   case "request"->plugin.getClanAllianceService().request(clan,other);case "accept"->plugin.getClanAllianceService().accept(clan,other);
   case "decline"->plugin.getClanAllianceService().decline(clan,other);case "remove"->plugin.getClanAllianceService().remove(clan,other);default->ClanAllianceService.Result.CLAN_NOT_FOUND;};
  if(result!=ClanAllianceService.Result.SUCCESS){send(player,errorKey(action,result));return;}
  switch(action){case "request"->{message(player,"command.ally_request_sent","{target}",other);notifyClan(other,"command.ally_request_received","{clan}",clan);}
   case "accept"->{message(player,"command.ally_accept_success","{requester}",other);notifyClan(other,"command.ally_accepted_notify","{clan}",clan);Bukkit.getPluginManager().callEvent(new ClanAllianceCreatedEvent(player,other,clan));}
   case "decline"->{message(player,"command.ally_decline_success","{requester}",other);notifyClan(other,"command.ally_declined_notify","{clan}",clan);}
   case "remove"->message(player,"command.ally_remove_success","{target}",other);}
 }
 private void friendlyFire(Player player,String clan,String value){if(!permission.test(player,clan,ClanPermission.ALLY_FF))return;if(!value.equalsIgnoreCase("on")&&!value.equalsIgnoreCase("off")){send(player,"command.allyff_usage");return;}
  boolean enabled=value.equalsIgnoreCase("on");plugin.getClanAllianceService().setFriendlyFire(clan,enabled);message(player,"command.allyff_status","{status}",lang.getMessage(enabled?"status.enabled":"status.disabled"));}
 private String errorKey(String action,ClanAllianceService.Result result){if(result==ClanAllianceService.Result.SAME_CLAN)return "command.ally_"+(action.equals("request")?"same_clan":action+"_same_clan");
  if(result==ClanAllianceService.Result.CLAN_NOT_FOUND)return "command.ally_target_not_exist";if(result==ClanAllianceService.Result.NO_PENDING)return "command.ally_"+action+"_no_pending";
  if(result==ClanAllianceService.Result.NOT_ALLIED)return "command.ally_remove_none";return "command.ally_already_requested";}
 private void notifyClan(String clan,String key,String token,String value){for(Player p:Bukkit.getOnlinePlayers())if(clan.equalsIgnoreCase(plugin.getPlayerClan(p.getName())))message(p,key,token,value);}
 private void send(Player p,String key){p.sendMessage(MSG.color(lang.getMessageWithPrefix(key)));}
 private void message(Player p,String key,String token,String value){p.sendMessage(MSG.color(lang.getMessageWithPrefix(key).replace(token,value)));}
}
