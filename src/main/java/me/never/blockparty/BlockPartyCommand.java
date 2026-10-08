package me.never.blockparty;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.List;

public final class BlockPartyCommand implements CommandExecutor, TabCompleter {
    private final Game game;
    public BlockPartyCommand(Game game){this.game=game;}
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Players only."); return true;}
        String sub=args.length==0?"join":args[0].toLowerCase();
        switch(sub){
            case "join" -> game.join(p);
            case "leave" -> game.leave(p);
            case "start" -> { if(p.hasPermission("neverblockparty.admin")) game.forceStart(); else p.sendMessage("§cNo permission."); }
            case "stop" -> { if(p.hasPermission("neverblockparty.admin")) game.stop(); else p.sendMessage("§cNo permission."); }
            case "status" -> p.sendMessage("§bBlockParty §8» §f"+game.state()+" §7| players "+game.players().size());
            default -> p.sendMessage("§e/bp join §7| §e/bp leave §7| §e/bp status");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args){
        if(args.length==1) return List.of("join","leave","status","start","stop");
        return List.of();
    }
}