package me.pinkycore.pinkyteams.CMDs;

import me.pinkycore.pinkyteams.PinkyTeams;
import me.pinkycore.pinkyteams.Utils.LangManager;
import me.pinkycore.pinkyteams.Utils.MSG;
import me.pinkycore.pinkyteams.service.ClanWarService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.pinkycore.pinkyteams.api.event.ClanWarStartEvent;

final class ClanWarCommand {
    private final PinkyTeams plugin; private final LangManager lang;
    ClanWarCommand(PinkyTeams plugin,LangManager lang){this.plugin=plugin;this.lang=lang;}
    void execute(Player player,String clan,String[] args){
        if(!plugin.getConfig().getBoolean("wars.enabled",true)){send(player,"user.war_disabled");return;}
        if(clan==null){send(player,"user.no_clan");return;}
        if(args.length<2){status(player,clan);return;}
        ClanWarService service=plugin.getClanWarService(); String action=args[1].toLowerCase();
        if(action.equals("status")){status(player,clan);return;}
        if(action.equals("surrender")){
            var result=service.surrender(clan); if(result.isEmpty()){send(player,"user.war_none");return;}
            plugin.completeWar(result.get()); return;
        }
        if(args.length<3){send(player,"user.war_usage");return;}
        String other=args[2]; ClanWarService.Result result=switch(action){
            case "request" -> service.request(clan,other);
            case "accept" -> service.accept(clan,other);
            case "decline" -> service.decline(clan,other);
            default -> ClanWarService.Result.INVALID;
        };
        if(result!=ClanWarService.Result.SUCCESS){
            player.sendMessage(MSG.color(lang.getMessageWithPrefix("user.war_error").replace("{result}",result.name())));return;
        }
        if(action.equals("accept")) {
            Bukkit.getPluginManager().callEvent(new ClanWarStartEvent(player,clan,other));
            broadcast("user.war_started","{clan1}",clan,"{clan2}",other);
        }
        else player.sendMessage(MSG.color(lang.getMessageWithPrefix("user.war_"+action+"_success").replace("{clan}",other)));
    }
    private void status(Player player,String clan){
        var war=plugin.getClanWarService().findWar(clan); if(war.isEmpty()){send(player,"user.war_none");return;}
        var w=war.get(); player.sendMessage(MSG.color(lang.getMessageWithPrefix("user.war_status")
            .replace("{opponent}",w.opponentOf(clan)).replace("{score}",String.valueOf(w.scoreFor(clan)))
            .replace("{opponent_score}",String.valueOf(w.scoreFor(w.opponentOf(clan))))));
    }
    private void broadcast(String key,String k1,String v1,String k2,String v2){
        Bukkit.broadcastMessage(MSG.color(lang.getMessageWithPrefix(key).replace(k1,v1).replace(k2,v2)));
    }
    private void send(Player p,String key){p.sendMessage(MSG.color(lang.getMessageWithPrefix(key)));}
}
