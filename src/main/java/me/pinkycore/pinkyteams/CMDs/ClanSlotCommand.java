package me.pinkycore.pinkyteams.CMDs;
import me.pinkycore.pinkyteams.PinkyTeams; import me.pinkycore.pinkyteams.Utils.*; import me.pinkycore.pinkyteams.service.ClanSlotService; import org.bukkit.entity.Player;
final class ClanSlotCommand{
 private final PinkyTeams plugin;private final LangManager lang;private final ClanEconomyCommand.PermissionCheck permission;
 ClanSlotCommand(PinkyTeams p,LangManager l,ClanEconomyCommand.PermissionCheck c){plugin=p;lang=l;permission=c;}
 void execute(Player player,String clan,String[] args){if(clan==null){send(player,"user.no_clan");return;}var service=plugin.getClanSlotService();
  if(!service.enabled()){send(player,"user.slots_disabled");return;}var status=service.status(clan);
  if(!service.usesPoints()){message(player,"user.slots_static_limit","{used}",status.used(),"{limit}",ClanSlotService.formatLimit(status.limit()));return;}
  if(args.length==1){message(player,"user.slots_status","{used}",status.used(),"{limit}",ClanSlotService.formatLimit(status.limit()));message(player,"user.slots_points","{points}",status.points());
   if(status.next()!=null)message(player,"user.slots_next_upgrade","{cost}",status.next().cost(),"{slots}",status.next().slots());else send(player,"user.slots_no_more_upgrades");return;}
  if(!args[1].equalsIgnoreCase("buy")){send(player,"user.slots_usage");return;}if(!permission.test(player,clan,ClanPermission.SLOTS_UPGRADE))return;
  var purchase=service.buy(clan);switch(purchase.result()){case NO_MORE->send(player,"user.slots_no_more_upgrades");case NOT_ENOUGH->message(player,"user.slots_not_enough_points","{cost}",status.next().cost(),"{points}",status.points());
   case SUCCESS->message(player,"user.slots_bought","{slots}",purchase.upgrade().slots(),"{limit}",ClanSlotService.formatLimit(purchase.newLimit()),"{cost}",purchase.upgrade().cost());}}
 private void send(Player p,String key){p.sendMessage(MSG.color(lang.getMessageWithPrefix(key)));}
 private void message(Player p,String key,Object... values){String m=lang.getMessageWithPrefix(key);for(int i=0;i+1<values.length;i+=2)m=m.replace(String.valueOf(values[i]),String.valueOf(values[i+1]));p.sendMessage(MSG.color(m));}
}
