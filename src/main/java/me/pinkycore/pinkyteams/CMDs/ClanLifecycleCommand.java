package me.pinkycore.pinkyteams.CMDs;
import me.pinkycore.pinkyteams.PinkyTeams;import me.pinkycore.pinkyteams.Utils.*;import me.pinkycore.pinkyteams.api.event.*;import me.pinkycore.pinkyteams.service.ClanMembershipService;
import org.bukkit.Bukkit;import org.bukkit.configuration.file.FileConfiguration;import org.bukkit.entity.Player;import java.util.*;import java.util.regex.Pattern;
final class ClanLifecycleCommand{
 private final PinkyTeams plugin;private final LangManager lang;private final ClanEconomyCommand.PermissionCheck permission;
 ClanLifecycleCommand(PinkyTeams p,LangManager l,ClanEconomyCommand.PermissionCheck c){plugin=p;lang=l;permission=c;}
 void create(Player player,String[] args){if(args.length<2){send(player,"user.create_usage");return;}String raw=args[1],name=ClanNameHandler.getVisibleName(raw);
  FileConfiguration c=plugin.getConfig();int min=c.getInt("clan-name.min-length",0),max=c.getInt("clan-name.max-length",ClanNameHandler.DEFAULT_MAX_VISIBLE_LENGTH);
  if(max>0&&name.length()>max){message(player,"user.create_name_too_long","{max}",max);return;}if(min>0&&name.length()<min){message(player,"user.create_name_too_short","{min}",min);return;}
  if(c.getBoolean("names-blocked.enabled",true)&&c.getStringList("names-blocked.blocked").stream().anyMatch(x->x.equalsIgnoreCase(name))){send(player,"user.create_name_blocked");return;}
  if(c.getBoolean("clan-name.regex.enabled",false)&&!matches(c.getString("clan-name.regex.pattern","^[A-Za-z0-9_]+$"),name)){send(player,"user.create_name_invalid_regex");return;}
  if(plugin.isClanBanned(name)){message(player,"msg.clan_name_banned","{clan}",name);return;}ClanCreateEvent event=new ClanCreateEvent(player,name);Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())return;
  String ip=player.getAddress()!=null&&player.getAddress().getAddress()!=null?player.getAddress().getAddress().getHostAddress():"unknown";
  Bukkit.getScheduler().runTaskAsynchronously(plugin,()->validateAndCharge(player,raw,name,ip));
 }
 private void validateAndCharge(Player player,String raw,String name,String ip){FileConfiguration c=plugin.getConfig();
  if(plugin.getStorageProvider().clanExists(name)){sync(()->send(player,"user.create_exists"));return;}int max=c.getInt("max-clans",0);
  if(max>0&&plugin.getStorageProvider().getAllClans().size()>=max){sync(()->message(player,"user.create_limit","{max}",max));return;}
  boolean anti=c.getBoolean("anti-multiaccount.enabled",false),only=c.getBoolean("anti-multiaccount.only-when-global-limit",true);int perIp=c.getInt("anti-multiaccount.max-clans-per-ip",1);
  if(anti&&perIp>0&&(!only||max>0)&&plugin.getIpClanTracker().getClanCountForIp(ip)>=perIp){sync(()->message(player,"user.create_ip_limit_reached","{limit}",perIp));return;}
  sync(()->chargeThenCreate(player,raw,name,ip));
 }
 private void chargeThenCreate(Player player,String raw,String name,String ip){FileConfiguration c=plugin.getConfig();Econo econ=PinkyTeams.getEcon();int cost=Math.max(0,c.getInt("economy.cost.create-clan",0));
  boolean charged=c.getBoolean("economy.enabled",true)&&cost>0;if(charged&&(!econ.has(player,cost)||!econ.withdraw(player,cost))){message(player,"user.create_no_money","{cost}",cost);return;}
  Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{String privacy="public".equalsIgnoreCase(c.getString("clan.default-privacy"))?"public":"private";
    plugin.getStorageProvider().createClan(name,raw,player.getName(),player.getName(),0,privacy);plugin.getStorageProvider().reloadCache();if(!plugin.getStorageProvider().clanExists(name))throw new IllegalStateException("not persisted");plugin.getIpClanTracker().addClan(ip,name);
    sync(()->{plugin.getDiscordNotifier().onClanCreated(name,player.getName(),ip);PECMD.addClanToHistory(player,name);Bukkit.getPluginManager().callEvent(new ClanCreatedEvent(player,name));message(player,"user.create_success","{clan}",MSG.color(raw));
      if(c.getBoolean("clan.creation-announcement.enabled",false))Bukkit.broadcastMessage(MSG.color(lang.getMessageWithPrefix("user.create_announcement").replace("{player}",player.getName()).replace("{clan}",MSG.color(raw))));});
   }catch(Exception e){plugin.getLogger().severe("Clan creation failed: "+e.getMessage());sync(()->{if(charged&&!econ.deposit(player,cost))plugin.getLogger().severe("Failed creation refund for "+player.getName());send(player,"user.create_error");});}});
 }
 void disband(Player player,String clan,String[] args){if(clan==null){send(player,"user.no_clan");return;}if(!permission.test(player,clan,ClanPermission.DISBAND))return;
  if(plugin.getConfig().getBoolean("clan.disband-confirmation.enabled",true)&&(args.length<2||!args[1].equalsIgnoreCase("confirm"))){send(player,"user.disband_confirm");return;}
  ClanDisbandEvent event=new ClanDisbandEvent(player,clan);Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())return;
  Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{plugin.getStorageProvider().deleteClan(clan);plugin.getStorageProvider().reloadCache();if(plugin.getStorageProvider().clanExists(clan))throw new IllegalStateException("still exists");
    sync(()->{int gain=Math.max(0,plugin.getConfig().getInt("economy.earn.delete-clan",0));boolean rewarded=gain>0&&plugin.getConfig().getBoolean("economy.enabled",true)&&PinkyTeams.getEcon().deposit(player,gain);
      plugin.notifyClanDeleted(clan);Bukkit.getPluginManager().callEvent(new ClanDisbandedEvent(player,clan));if(rewarded)message(player,"user.disband_success_earn","{money}",gain);else send(player,"user.disband_success");});
   }catch(Exception e){plugin.getLogger().severe("Disband failed: "+e.getMessage());sync(()->send(player,"user.disband_error"));}});
 }
 void leave(Player player,String clan){if(clan==null){send(player,"user.no_clan");return;}ClanLeaveEvent event=new ClanLeaveEvent(player,clan);Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())return;
  var result=plugin.getClanMembershipService().leave(player.getName(),clan);Bukkit.getPluginManager().callEvent(new ClanLeftEvent(player,clan,result.clanDeleted()));
  if(result.clanDeleted()){plugin.notifyClanDeleted(clan);send(player,"user.clan_deleted_empty");}else if(result.leaderChanged())message(player,"user.leader_left","{newLeader}",result.newLeader());else send(player,"user.left_clan");}
 void kick(Player player,String target){String clan=plugin.getPlayerClan(player.getName());if(clan==null){send(player,"user.no_clan");return;}if(!permission.test(player,clan,ClanPermission.KICK))return;
  if(target.equalsIgnoreCase(player.getName())){send(player,"user.kick_cant_kick_self");return;}if(!plugin.getStorageProvider().isPlayerInClan(target,clan)){send(player,"user.kick_player_not_member");return;}
  String leader=plugin.getStorageProvider().getClanLeader(clan);if(leader!=null&&leader.equalsIgnoreCase(target)){send(player,"user.kick_cant_kick_leader");return;}
  plugin.getStorageProvider().removePlayerFromClan(target,clan);message(player,"user.kick_success","{player}",target,"{clan}",clan);}
 void resign(Player player,String clan){if(clan==null){send(player,"user.no_clan");return;}String leader=plugin.getStorageProvider().getClanLeader(clan);if(leader==null||!leader.equalsIgnoreCase(player.getName())){send(player,"user.resign_not_leader");return;}
  List<String> members=new ArrayList<>(plugin.getStorageProvider().getClanMembers(clan));members.removeIf(x->x.equalsIgnoreCase(player.getName()));if(members.isEmpty()){plugin.getStorageProvider().deleteClan(clan);plugin.notifyClanDeleted(clan);send(player,"user.resign_clan_deleted");}
  else{String next=members.get(0);plugin.getStorageProvider().updateClanLeader(clan,next);message(player,"user.resign_success","{newLeader}",next);}}
 private boolean matches(String regex,String value){try{return Pattern.compile(regex).matcher(value).matches();}catch(Exception e){return false;}}
 private void sync(Runnable r){Bukkit.getScheduler().runTask(plugin,r);}private void send(Player p,String key){p.sendMessage(MSG.color(lang.getMessageWithPrefix(key)));}
 private void message(Player p,String key,Object... values){String m=lang.getMessageWithPrefix(key);for(int i=0;i+1<values.length;i+=2)m=m.replace(String.valueOf(values[i]),String.valueOf(values[i+1]));p.sendMessage(MSG.color(m));}
}
