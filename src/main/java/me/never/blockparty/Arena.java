package me.never.blockparty;

import org.bukkit.*;
import org.bukkit.block.Block;
import java.util.*;

public final class Arena {
    private final NeverBlockParty plugin;
    private final World world;
    private final Set<Block> floor = new LinkedHashSet<>();
    private final Map<Block, Material> original = new HashMap<>();
    private int minX, maxX, minZ, maxZ, y;

    public Arena(NeverBlockParty plugin, World world){ this.plugin=plugin; this.world=world; }

    public void build(){
        int size=plugin.getConfig().getInt("arena.size",17);
        y=plugin.getConfig().getInt("arena.y",80);
        minX = -size/2; maxX=minX+size-1; minZ=-size/2; maxZ=minZ+size-1;
        List<Material> mats=new ArrayList<>();
        for(String name:plugin.getConfig().getStringList("game.colors")){
            try { mats.add(Material.valueOf(name)); } catch (IllegalArgumentException ignored) {}
        }
        if(mats.isEmpty()) mats.add(Material.BLUE_WOOL);
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++){
            Block b=world.getBlockAt(x,y,z);
            original.putIfAbsent(b,b.getType());
            b.setType(mats.get(Math.floorMod(x*31+z*17,mats.size())), false);
            floor.add(b);
        }
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
        world.setTime(6000);
    }

    public Set<Block> floor(){return floor;}
    public Map<Block,Material> original(){return original;}
    public World world(){return world;}
    public Location lobby(){return new Location(world,0,y+3,0,0,0);}
    public Location playerSpawn(int index, int total){
        double cx=(minX+maxX)/2.0, cz=(minZ+maxZ)/2.0;
        double r=Math.max(2, Math.min((maxX-minX)/2.0-1, 6));
        double angle=(Math.PI*2*index)/Math.max(1,total);
        return new Location(world,cx+Math.cos(angle)*r,y+1,cz+Math.sin(angle)*r);
    }
    public double deathY(){ return y-10; }
}