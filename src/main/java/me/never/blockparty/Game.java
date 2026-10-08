package me.never.blockparty;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public final class Game {
    public enum State { WAITING, STARTING, PLAYING, ENDING }
    private final NeverBlockParty plugin;
    private final Arena arena;
    private final LinkedHashSet<UUID> players=new LinkedHashSet<>();
    private final LinkedHashSet<UUID> alive=new LinkedHashSet<>();
    private BukkitTask task;
    private State state=State.WAITING;
    private int countdown, round, roundSeconds;
    private Material target;
    private Economy economy;
    private final Random random=new Random();

    public Game(NeverBlockParty plugin,Arena arena){
        this.plugin=plugin; this.arena=arena;
        var registration=Bukkit.getServicesManager().getRegistration(Economy.class);
        if(registration!=null) economy=registration.getProvider();
    }
    public Set<UUID> players(){return Collections.unmodifiableSet(players);}
    public State state(){return state;}

    public void join(Player p){
        if(players.contains(p.getUniqueId())){p.sendMessage("§cYou are already in Block Party.");return;}
        if(state==State.PLAYING||state==State.ENDING){p.sendMessage("§cA game is already running.");return;}
        int max=plugin.getConfig().getInt("arena.max-players",16);
        if(players.size()>=max){p.sendMessage("§cArena is full.");return;}
        players.add(p.getUniqueId());
        p.setGameMode(GameMode.ADVENTURE);
        p.teleport(arena.lobby());
        p.sendMessage("§b§lBlock Party §8» §fJoined §7("+players.size()+"/"+max+")");
        if(players.size()>=plugin.getConfig().getInt("arena.min-players",2)&&state==State.WAITING) startCountdown();
    }

    public void leave(Player p){
        UUID id=p.getUniqueId(); if(!players.remove(id)) return;
        alive.remove(id);
        p.setGameMode(GameMode.ADVENTURE);
        p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        if(state==State.STARTING && players.size()<plugin.getConfig().getInt("arena.min-players",2)) stop();
    }

    public void forceStart(){
        if(state==State.WAITING && players.size()>=plugin.getConfig().getInt("arena.min-players",2)) startCountdown();
    }

    private void startCountdown(){
        cancel(); state=State.STARTING;
        countdown=plugin.getConfig().getInt("game.countdown-seconds",10);
        task=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            if(players.size()<plugin.getConfig().getInt("arena.min-players",2)){stop();return;}
            if(countdown<=0){startGame();return;}
            broadcast("§eGame starts in §f"+countdown+"§e...");
            for(UUID id:players){Player p=Bukkit.getPlayer(id); if(p!=null) p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HAT,0.6f,1.0f+Math.min(countdown,10)*0.03f);}
            countdown--;
        },0,20);
    }

    private void startGame(){
        cancel(); state=State.PLAYING; alive.clear(); alive.addAll(players); round=0;
        int i=0,total=players.size();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null)p.teleport(arena.playerSpawn(i++,total));}
        nextRound();
    }

    private void nextRound(){
        if(alive.size()<=1){finish();return;}
        round++;
        List<String> colors=plugin.getConfig().getStringList("game.colors");
        try{target=Material.valueOf(colors.get(random.nextInt(colors.size())));}catch(Exception e){target=Material.BLUE_WOOL;}
        int base=plugin.getConfig().getInt("game.round-seconds",5);
        int speed=plugin.getConfig().getInt("game.speed-up-after-round",3);
        int min=plugin.getConfig().getInt("game.min-round-seconds",1);
        roundSeconds=Math.max(min, base - Math.max(0,(round-speed)/2));
        broadcastTitle("§f§lSTAND ON","§b"+pretty(target));
        countdown=roundSeconds;
        task=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            if(countdown<=0){resolve();return;}
            String bar="§eTime §f"+countdown+"s §8| §7Round §f"+round;
            for(UUID id:alive){Player p=Bukkit.getPlayer(id);if(p!=null){p.sendActionBar(bar); if(countdown<=3)p.playSound(p.getLocation(),Sound.UI_BUTTON_CLICK,0.7f,1.4f);}}
            countdown--;
        },0,20);
    }

    private void resolve(){
        cancel();
        for(Block b:arena.floor()) if(b.getType()!=target) b.setType(Material.AIR,false);
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            Set<UUID> eliminated=new HashSet<>();
            for(UUID id:new HashSet<>(alive)){
                Player p=Bukkit.getPlayer(id);
                if(p==null||p.getLocation().getY()<arena.deathY()){eliminated.add(id);continue;}
                Block below=p.getLocation().clone().subtract(0,0.15,0).getBlock();
                if(below.getType()!=target) eliminated.add(id);
            }
            for(UUID id:eliminated){
                alive.remove(id); Player p=Bukkit.getPlayer(id);
                if(p!=null){p.setGameMode(GameMode.SPECTATOR);p.sendTitle("§cELIMINATED","§7Round "+round,5,25,5);p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_HURT,1,0.7f);}
            }
            restore();
            if(alive.size()<=1) finish(); else nextRound();
        },plugin.getConfig().getLong("game.restore-delay-ticks",20));
    }

    private void restore(){arena.original().forEach((b,m)->b.setType(m,false));}

    private void finish(){
        cancel(); state=State.ENDING; UUID winner=alive.stream().findFirst().orElse(null);
        if(winner!=null){Player p=Bukkit.getPlayer(winner);if(p!=null){broadcastTitle("§6§lWINNER!","§e"+p.getName());p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,1,1);pay(p,players.size());}}
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            restore();
            for(UUID id:new HashSet<>(players)){Player p=Bukkit.getPlayer(id);if(p!=null){p.setGameMode(GameMode.ADVENTURE);p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());}}
            players.clear();alive.clear();state=State.WAITING;
        },80);
    }

    private void pay(Player p,int count){
        if(economy==null){p.sendMessage("§cVault economy was not found.");return;}
        double reward;
        if(count<=4)reward=100; else if(count<=8)reward=250; else if(count<=12)reward=500; else reward=1000;
        economy.depositPlayer(p,reward);
        p.sendMessage("§a+$"+String.format(Locale.US,"%.2f",reward)+" §7added to your balance.");
    }

    public void stop(){cancel();restore();for(UUID id:new HashSet<>(players)){Player p=Bukkit.getPlayer(id);if(p!=null){p.setGameMode(GameMode.ADVENTURE);p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());}}players.clear();alive.clear();state=State.WAITING;}
    public void shutdown(){stop();}
    private void broadcast(String msg){for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage("§b§lBlock Party §8» §r"+msg);}}
    private void broadcastTitle(String title,String sub){for(UUID id:alive){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendTitle(title,sub,5,25,5);}}
    private String pretty(Material m){return m.name().replace("_WOOL","").replace('_',' ');}
    private void cancel(){if(task!=null){task.cancel();task=null;}}
}