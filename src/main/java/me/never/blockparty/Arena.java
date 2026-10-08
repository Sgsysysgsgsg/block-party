package me.never.blockparty;
import org.bukkit.*;import org.bukkit.block.Block;import java.util.*;
public final class Arena{final NeverBlockParty p;final World w;final Map<Block,Material> original=new HashMap<>();final List<Block> floor=new ArrayList<>();int min,max,y;
 Arena(NeverBlockParty p,World w){this.p=p;this.w=w;}
 void build(){int s=p.getConfig().getInt("arena.size",17);y=p.getConfig().getInt("arena.y",80);min=-(s/2);max=min+s-1;List<Material> m=new ArrayList<>();for(String n:p.getConfig().getStringList("colors"))try{m.add(Material.valueOf(n));}catch(Exception ignored){}if(m.isEmpty())m.add(Material.BLUE_WOOL);for(int x=min;x<=max;x++)for(int z=min;z<=max;z++){Block b=w.getBlockAt(x,y,z);original.putIfAbsent(b,b.getType());b.setType(m.get(Math.floorMod(x*31+z*17,m.size())),false);if(!floor.contains(b))floor.add(b);}}
 void collapse(Material target){for(Block b:floor)if(b.getType()!=target)b.setType(Material.AIR,false);}
 void restore(){for(Map.Entry<Block,Material> e:original.entrySet())e.getKey().setType(e.getValue(),false);build();}
 Location spawn(int i,int total){double a=(Math.PI*2*i)/Math.max(1,total);return new Location(w,Math.cos(a)*5+0.5,y+1,Math.sin(a)*5+0.5);}
}