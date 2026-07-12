package me.pinkycore.pinkyteams.CMDs;
import me.pinkycore.pinkyteams.PinkyTeams;import me.pinkycore.pinkyteams.Utils.*;import me.pinkycore.pinkyteams.api.event.*;import me.pinkycore.pinkyteams.service.ClanSlotService;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;
final class ClanInvitationCommand{
 private final PinkyTeams plugin;private final LangManager lang;private final ClanEconomyCommand.PermissionCheck permission;
 ClanInvitationCommand(PinkyTeams p,LangManager l,ClanEconomyCommand.PermissionCheck c){plugin=p;lang=l;permission=c;}
 void invite(Player inviter,String targetName){String clan=plugin.getStorageProvider().getCachedPlayerClan(inviter.getName());if(clan==null){send(inviter,"user.no_clan");return;}
  plugin.getStorageProvider().cleanupExpiredInvites();if(!permission.test(inviter,clan,ClanPermission.INVITE))return;
  if(!plugin.getClanSlotService().hasSpace(clan)){slotsFull(inviter,clan);return;}if(targetName.equalsIgnoreCase(inviter.getName())){send(inviter,"user.cant_invite_self");return;}
  Player target=Bukkit.getPlayerExact(targetName);if(target==null||!target.isOnline()){send(inviter,"user.player_not_online");return;}
  if(plugin.getStorageProvider().getCachedPlayerClan(targetName)!=null){send(inviter,"user.player_in_other_clan");return;}
  if(plugin.getStorageProvider().isPlayerInvitedToClan(targetName,clan)){send(inviter,"user.invite_pending");return;}
  plugin.getStorageProvider().addClanInvite(clan,targetName);message(inviter,"user.invite_sent","{player}",targetName);message(target,"user.invite_received","{clan}",clan);
  target.sendMessage(MSG.color(lang.getMessage("user.invite_usage")));
 }
 void join(Player player,String input){String name=player.getName();if(plugin.getPlayerClan(name)!=null){send(player,"user.already_in_clan");return;}
  plugin.getStorageProvider().cleanupExpiredInvites();String clan=resolve(input);if(!plugin.getStorageProvider().clanExists(clan)){send(player,"user.clan_not_exist");return;}
  boolean allowed="public".equalsIgnoreCase(plugin.getStorageProvider().getClanPrivacy(clan))||plugin.getStorageProvider().isPlayerInvitedToClan(name,clan);
  if(!allowed){send(player,"user.clan_private");return;}if(!plugin.getClanSlotService().hasSpace(clan)){slotsFull(player,clan);return;}
  ClanJoinEvent event=new ClanJoinEvent(player,clan);Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())return;
  plugin.getClanMembershipService().join(name,clan);Bukkit.getPluginManager().callEvent(new ClanJoinedEvent(player,clan));PECMD.addClanToHistory(player,clan);message(player,"user.joined_clan","{clan}",clan);
 }
 void decline(Player player,String input){plugin.getStorageProvider().cleanupExpiredInvites();String clan=resolve(input);
  if(!plugin.getStorageProvider().clanExists(clan)){send(player,"user.clan_not_exist");return;}if(!plugin.getStorageProvider().isPlayerInvitedToClan(player.getName(),clan)){message(player,"user.invite_decline_no_pending","{clan}",clan);return;}
  plugin.getStorageProvider().removeClanInvite(clan,player.getName());message(player,"user.invite_declined","{clan}",clan);
 }
 private String resolve(String input){for(String clan:plugin.getStorageProvider().getCachedClanNames())if(clan.equalsIgnoreCase(input))return clan;return input;}
 private void slotsFull(Player p,String clan){message(p,"user.slots_full","{used}",plugin.getClanSlotService().memberCount(clan),"{limit}",ClanSlotService.formatLimit(plugin.getClanSlotService().limit(clan)));}
 private void send(Player p,String key){p.sendMessage(MSG.color(lang.getMessageWithPrefix(key)));}
 private void message(Player p,String key,Object... values){String m=lang.getMessageWithPrefix(key);for(int i=0;i+1<values.length;i+=2)m=m.replace(String.valueOf(values[i]),String.valueOf(values[i+1]));p.sendMessage(MSG.color(m));}
}
