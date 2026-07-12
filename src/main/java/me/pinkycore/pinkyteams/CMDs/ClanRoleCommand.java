package me.pinkycore.pinkyteams.CMDs;
import me.pinkycore.pinkyteams.PinkyTeams; import me.pinkycore.pinkyteams.Utils.*; import org.bukkit.entity.Player;
import java.util.*; import java.util.stream.Collectors;
final class ClanRoleCommand{
 private final PinkyTeams plugin;private final LangManager lang;private final ClanEconomyCommand.PermissionCheck permission;
 ClanRoleCommand(PinkyTeams p,LangManager l,ClanEconomyCommand.PermissionCheck c){plugin=p;lang=l;permission=c;}
 void execute(Player player,String clan,String[] args){
  if(clan==null||clan.isEmpty()){send(player,"user.no_clan");return;}if(!permission.test(player,clan,ClanPermission.RANK_MANAGE))return;
  if(args.length<2){if(plugin.getGuiManager()!=null){plugin.getGuiManager().openRolesMenu(player);return;}send(player,"user.rank_usage");return;}
  switch(args[1].toLowerCase(Locale.ROOT)){
   case "create"->{if(args.length<3){send(player,"user.rank_usage_create");return;}String role=normalize(args[2]);
    if(reserved(role)){send(player,"user.rank_cant_edit_role");return;}if(exists(clan,role)){send(player,"user.rank_role_exists");return;}
    plugin.getRoleManager().createRole(clan,role);send(player,"user.rank_role_created","{role}",display(role));}
   case "delete"->{if(args.length<3){send(player,"user.rank_usage_delete");return;}String role=normalize(args[2]);
    if(reserved(role)){send(player,"user.rank_cant_edit_role");return;}if(!exists(clan,role)){send(player,"user.rank_role_not_found");return;}
    for(String member:plugin.getStorageProvider().getClanMembers(clan))if(role.equalsIgnoreCase(plugin.getRoleManager().getPlayerRole(clan,member)))
      plugin.getRoleManager().setPlayerRole(clan,member,ClanRoleManager.ROLE_MEMBER);
    plugin.getRoleManager().deleteRole(clan,role);send(player,"user.rank_role_deleted","{role}",display(role));}
   case "set"->{if(args.length<4){send(player,"user.rank_usage_set");return;}String target=args[2],role=normalize(args[3]);
    if(!plugin.getStorageProvider().isPlayerInClan(target,clan)){send(player,"user.rank_target_not_in_clan");return;}
    if(ClanRoleManager.ROLE_LEADER.equalsIgnoreCase(role)){send(player,"user.rank_cant_edit_leader");return;}
    if(!exists(clan,role)){send(player,"user.rank_role_not_found");return;}String leader=plugin.getStorageProvider().getClanLeader(clan);
    if(leader!=null&&leader.equalsIgnoreCase(target)){send(player,"user.rank_cant_edit_leader");return;}
    plugin.getRoleManager().setPlayerRole(clan,target,role);player.sendMessage(MSG.color(lang.getMessageWithPrefix("user.rank_role_set").replace("{player}",target).replace("{role}",display(role))));}
   case "perms"->permissions(player,clan,args);
   case "list"->{Set<String> names=new HashSet<>(plugin.getRoleManager().getClanRolePermissions(clan).keySet());names.add(ClanRoleManager.ROLE_MEMBER);names.add(ClanRoleManager.ROLE_CO_LEADER);
    List<String> sorted=new ArrayList<>(names);Collections.sort(sorted);send(player,"user.rank_role_list","{roles}",String.join(", ",sorted));}
   default->send(player,"user.rank_usage");
  }
 }
 private void permissions(Player player,String clan,String[] args){if(args.length<4){send(player,"user.rank_usage_perms");return;}String role=normalize(args[2]);
  if(reserved(role)){send(player,"user.rank_cant_edit_role");return;}if(!exists(clan,role)){send(player,"user.rank_role_not_found");return;}
  String action=args[3].toLowerCase(Locale.ROOT);Set<ClanPermission> perms=new HashSet<>(plugin.getRoleManager().getClanRolePermissions(clan).getOrDefault(role.toLowerCase(Locale.ROOT),Set.of()));
  if(action.equals("list")){send(player,"user.rank_permissions_list","{role}",display(role),"{perms}",permissionList(perms));return;}
  if(action.equals("clear")){plugin.getRoleManager().setRolePermissions(clan,role,Set.of());send(player,"user.rank_permissions_cleared","{role}",display(role));return;}
  if((action.equals("add")||action.equals("remove"))&&args.length>=5){ClanPermission value=ClanPermission.fromKey(args[4]).orElse(null);if(value==null){send(player,"user.rank_invalid_permission");return;}
   boolean changed=action.equals("add")?perms.add(value):perms.remove(value);if(changed)plugin.getRoleManager().setRolePermissions(clan,role,perms);
   send(player,action.equals("add")?"user.rank_permission_added":"user.rank_permission_removed","{role}",display(role),"{permission}",value.getKey());return;}
  send(player,"user.rank_usage_perms");
 }
 private boolean exists(String clan,String role){return role.equals(ClanRoleManager.ROLE_MEMBER)||role.equals(ClanRoleManager.ROLE_CO_LEADER)||plugin.getRoleManager().getClanRolePermissions(clan).containsKey(role);}
 private boolean reserved(String role){return role.equals(ClanRoleManager.ROLE_LEADER)||role.equals(ClanRoleManager.ROLE_CO_LEADER);}
 private String normalize(String role){String value=role.trim().toLowerCase(Locale.ROOT).replace("_","-");return value.equals("coleader")||value.equals("colider")||value.equals("co-lider")?ClanRoleManager.ROLE_CO_LEADER:value;}
 private String display(String role){return Arrays.stream(role.split("-")).filter(s->!s.isEmpty()).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(Collectors.joining(" "));}
 private String permissionList(Set<ClanPermission> p){return p.isEmpty()?lang.getMessage("user.rank_permissions_none"):p.stream().map(ClanPermission::getKey).sorted().collect(Collectors.joining(", "));}
 private void send(Player p,String key,String... replacements){String m=lang.getMessageWithPrefix(key);for(int i=0;i+1<replacements.length;i+=2)m=m.replace(replacements[i],replacements[i+1]);p.sendMessage(MSG.color(m));}
}
