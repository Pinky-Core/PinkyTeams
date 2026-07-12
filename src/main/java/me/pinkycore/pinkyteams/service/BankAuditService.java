package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.PinkyTeams;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File; import java.io.IOException; import java.util.*;

public final class BankAuditService {
 private final PinkyTeams plugin; private final File file; private final YamlConfiguration data;
 public BankAuditService(PinkyTeams plugin){this.plugin=plugin;file=new File(plugin.getDataFolder(),"bank-audit.yml");data=YamlConfiguration.loadConfiguration(file);}
 public synchronized void record(String clan,String actor,Type type,double amount,double balance){
  String id=System.currentTimeMillis()+"-"+UUID.randomUUID().toString().substring(0,8),p="transactions."+id;
  data.set(p+".clan",clan);data.set(p+".actor",actor);data.set(p+".type",type.name());data.set(p+".amount",amount);
  data.set(p+".balance",balance);data.set(p+".timestamp",System.currentTimeMillis());save();
 }
 public synchronized List<Transaction> recent(String clan,int limit){
  var section=data.getConfigurationSection("transactions"); if(section==null)return List.of(); List<Transaction> out=new ArrayList<>();
  for(String id:section.getKeys(false)){String p="transactions."+id;if(!clan.equalsIgnoreCase(data.getString(p+".clan","")))continue;
   try{out.add(new Transaction(data.getString(p+".actor","SYSTEM"),Type.valueOf(data.getString(p+".type","DEPOSIT")),
    data.getDouble(p+".amount"),data.getDouble(p+".balance"),data.getLong(p+".timestamp")));}catch(IllegalArgumentException ignored){} }
  out.sort(Comparator.comparingLong(Transaction::timestamp).reversed());return List.copyOf(out.subList(0,Math.min(Math.max(0,limit),out.size())));
 }
 private void save(){try{data.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save bank audit: "+e.getMessage());}}
 public enum Type{DEPOSIT,WITHDRAW,WAR_REWARD,ADMIN_ADJUSTMENT}
 public record Transaction(String actor,Type type,double amount,double balance,long timestamp){}
}
