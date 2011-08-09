/*
 * Revenge for Bukkit
 * Copyright (C) 2011 simplyianm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.simplyian.mc.revenge;

import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Methods;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

/**
 * Revenge Main Class
 * 
 * @author simplyianm
 * @since 0.1
 */
public class Revenge extends JavaPlugin {
    /**
     * The list of possible revenges.
     * 
     * @since 0.1
     */
    public static HashMap<String, RevengeType> revengeList = new HashMap<String, RevengeType>();
    
    /**
     * Logger
     * 
     * @since 0.1
     */
    public static final Logger log = Logger.getLogger("Minecraft");
    
    /**
     * Config file
     */
    public File configFile = new File("plugins" + File.separator + "Revenge" + File.separator + "config.yml"); //50.22.36.159:25610 = killersmurf
    
    /**
     * Map of mob names to CreatureTypes.
     */
    public static HashMap<String, CreatureType> mobMap = new HashMap<String, CreatureType>();
    
    /**
     * Map of player revenges.
     */
    public static HashMap<String, List<String>> playerRevenges = new HashMap<String, List<String>>();
    
    /**
     * Register hook method
     */
    public Method registerMethod = null;
    
    /**
     * Triggered on the enabling of the plugin.
     * 
     * @since 0.1
     */
    @Override
    public void onEnable() {
        //Initialize mob map
        mobMap.put("chicken", CreatureType.CHICKEN);
        mobMap.put("cow", CreatureType.COW);
        mobMap.put("creeper", CreatureType.CREEPER);
        mobMap.put("ghast", CreatureType.GHAST);
        mobMap.put("giant", CreatureType.GIANT);
        mobMap.put("pig", CreatureType.PIG);
        mobMap.put("pigzombie", CreatureType.PIG_ZOMBIE);
        mobMap.put("sheep", CreatureType.SHEEP);
        mobMap.put("skeleton", CreatureType.SKELETON);
        mobMap.put("slime", CreatureType.SLIME);
        mobMap.put("spider", CreatureType.SPIDER);
        mobMap.put("squid", CreatureType.SQUID);
        mobMap.put("zombie", CreatureType.ZOMBIE);
        mobMap.put("monster", CreatureType.MONSTER);
        mobMap.put("wolf", CreatureType.WOLF);
        
        //Register Events
        PluginManager pm = this.getServer().getPluginManager();
        RVEL el = new RVEL();
        RVSL sl = new RVSL();
        pm.registerEvent(Event.Type.ENTITY_DEATH, el, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, sl, Event.Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLUGIN_DISABLE, sl, Event.Priority.Monitor, this);
        
        //Generate config if does not exist; otherwise load it
        new File("plugins" + File.separator + "Revenge").mkdir();
        if (!this.configFile.exists()) {
            toConsole("Config file not found, generating one right now...");
            try {
                if (configFile.createNewFile()) toConsole("File created!");
            } catch (IOException ex) {
                toConsole("File not created!");
            }
        }
        Configuration config = new Configuration(this.configFile);
        config.load();
        List<String> revengeCheck = config.getKeys("revenges");
        config.save();
        List<String> revenges = new ArrayList<String>();
        if (revengeCheck == null) {
            config.load();
            //Zombie Mayhem
            List<String> zmEffects = new ArrayList<String>();
            zmEffects.add("mobfromcorpse:zombie.5");
            config.setProperty("revenges.zm.name", "Zombie Mayhem");
            config.setProperty("revenges.zm.description", "Spawns 5 zombies from your death spot.");
            config.setProperty("revenges.zm.cost", 200);
            config.setProperty("revenges.zm.effects", zmEffects);
            //Call from Zeus
            List<String> czEffects = new ArrayList<String>();
            czEffects.add("msgradius:20.Zeus is angered!");
            czEffects.add("lightning");
            config.setProperty("revenges.cz.name", "Call of Zeus");
            config.setProperty("revenges.cz.description", "Call on Zeus to send lightning down to your death spot.");
            config.setProperty("revenges.cz.cost", 40);
            config.setProperty("revenges.cz.effects", czEffects);
            //Kamikaze
            List<String> kkEffects = new ArrayList<String>();
            kkEffects.add("explosion:10");
            config.setProperty("revenges.kk.name", "Kamikaze");
            config.setProperty("revenges.kk.description", "Go out with a BANG!");
            config.setProperty("revenges.kk.cost", 2000);
            config.setProperty("revenges.kk.effects", kkEffects);
            config.setProperty("revenges.kk.multiple", true);
            config.save();
            config.load();
            revenges = config.getKeys("revenges");
            config.save();
        } else {
            revenges = revengeCheck;
        }
        config.load(); 
        //Load revenges into RAM
        Iterator<String> it = revenges.iterator();
        while (it.hasNext()) {
            String revengeName = it.next();
            if (revengeName.matches("\\s")) {
                log.warning("[Revenge] The revenge identifier \"" + revengeName + "\" contains a space. It will not be included in the loaded Revenges.");
            } else {
                revengeList.put(revengeName, this.new RevengeType(revengeName));
            }
        }
        
        //Load player revenges into RAM
        List<String> playersBought = config.getKeys("players");
        if (playersBought != null) {
            Iterator<String> playerIt = playersBought.iterator();
            while (playerIt.hasNext()) {
                String playerPurchaserName = playerIt.next();
                List<String> ppRevenges = config.getStringList("players." + playerPurchaserName + "revenges", new ArrayList<String>());
                playerRevenges.put(playerPurchaserName, ppRevenges);
            }
        }
        config.save();
        toConsole("Plugin enabled.");
    }
    
    /**
     * Triggered on the disabling of the plugin.
     * 
     * @since 0.1
     */
    @Override
    public void onDisable() {
        toConsole("Plugin disabled.");
    }
    
    /**
     * Triggered on the execution of /revenge.
     * 
     * @param sender
     * @param cmd
     * @param currentAlias
     * @param args
     * @return boolean
     * 
     * @since 0.1
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String currentAlias, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                if (args[0].equals("list")) {
                    for (RevengeType rt : revengeList.values()) {
                        sender.sendMessage(ChatColor.GREEN + rt.getNiceName() + ": " + ChatColor.YELLOW + rt.getDescription());
                        sender.sendMessage("     --" + ChatColor.AQUA + "Buy with " + ChatColor.GOLD + "/revenge buy " + rt.getName() + ChatColor.AQUA + ".");
                    }
                } else if (args[0].equals("buy")) {
                    if (args.length >= 2) {
                        if (revengeList.containsKey(args[1])) {
                            RevengeType thert = revengeList.get(args[1]);
                            if (this.getRevenges(player.getName()).contains(args[1]) && !thert.canGetMultiple()) {
                                sender.sendMessage(ChatColor.RED + "You can't purchase multiple of the revenge " + ChatColor.GOLD + thert.getNiceName() + ChatColor.RED + ".");                                
                            } else {
                                if (this.registerMethod != null) {
                                    if (this.subtractMoney(player.getName(), thert.getCost())) {
                                        this.addRevenge(player.getName(), args[1]);
                                        sender.sendMessage("You have bought the revenge " + thert.getNiceName() + ".");
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "You don't have enough money to buy " + ChatColor.GOLD + thert.getNiceName() + ChatColor.RED + "." + ChatColor.DARK_RED + "(Requires " + thert.getCost() + ")");
                                    }
                                }
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Invalid Revenge Type. (Case sensitive)");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please specify which revenge you would like to buy.");
                    }
                } else if (args[0].equals("remove")) {
                    Configuration config = new Configuration(this.configFile);
                    config.load();
                    if (args.length > 1) {
                        if (this.getRevenges(player.getName()).contains(args[1])) {
                            this.removeRevenge(player.getName(), args[1]);
                            sender.sendMessage("You have removed the revenge " + args[1] + ".");
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't own that revenge!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please specify which revenge you would like to remove.");
                    }
                    config.save();
                } else if (args[0].equals("me")) {
                    sender.sendMessage(ChatColor.YELLOW + "===My Purchased " + ChatColor.RED + "Revenges" + ChatColor.YELLOW + "===");
                    List<String> myRevenges = this.getRevenges(player.getName());
                    if (myRevenges != null) {
                        HashMap<String, Integer> amountMap = new HashMap<String, Integer>();
                        Iterator<String> theIt = myRevenges.iterator();
                        while (theIt.hasNext()) {
                            String curElement = theIt.next();
                            Integer newValue = 1;
                            if (amountMap.containsKey(curElement)) {
                                newValue = amountMap.get(curElement).intValue() + 1;
                            }
                            amountMap.put(curElement, newValue);
                        }
                        for (Entry<String, Integer> aKey: amountMap.entrySet()) {
                            sender.sendMessage(ChatColor.GREEN + aKey.getValue().toString() + " x " + ChatColor.YELLOW + revengeList.get(aKey.getKey()).getNiceName());
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have not purchased any revenges.");
                    }
                } else if (args[0].equals("about")) {
                    sender.sendMessage("This server is running " + ChatColor.YELLOW + "Revenge " + ChatColor.GREEN + "v0.1" + ChatColor.GOLD + " by " + ChatColor.DARK_RED + "Albireo" + ChatColor.DARK_BLUE + "X" + ChatColor.LIGHT_PURPLE + " who apparently had fun with ChatColors...");
                    sender.sendMessage("Please contact me on the Bukkit forums if you have any ideas for this crappy excuse for a plugin. Thank you. :D");
                } else if (args[0].equals("help") || args[0].equals("?")) {
                    sender.sendMessage(ChatColor.GREEN + "/revenge list - " + ChatColor.YELLOW + "Lists revenges for you to buy.");
                    sender.sendMessage(ChatColor.GREEN + "/revenge buy - " + ChatColor.YELLOW + "Buys a revenge which is triggered when you die.");
                    sender.sendMessage(ChatColor.GREEN + "/revenge remove - " + ChatColor.YELLOW + "Removes a revenge.");
                    sender.sendMessage(ChatColor.GREEN + "/revenge me - " + ChatColor.YELLOW + "Shows what revenges you own.");
                    sender.sendMessage(ChatColor.GREEN + "/revenge help - " + ChatColor.YELLOW + "Displays this message.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Please use  " + ChatColor.YELLOW + "/revenge help " + ChatColor.RED + "for help on how to use this command.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Please use  " + ChatColor.YELLOW + "/revenge help " + ChatColor.RED + "for help on how to use this command.");
            }
        } else {
            toConsole("This command is in-game only.");
        }
        return true;
    }
    
    /**
     * Subtracts money from the player playerName.
     * 
     * @param playerName
     * @param amount
     * @return Did it work?
     */
    public boolean subtractMoney(String playerName, double amount) {
        double balance = this.registerMethod.getAccount(playerName).balance();
        return this.registerMethod != null && amount <= balance ? this.registerMethod.getAccount(playerName).subtract(amount) : false;
    }
    
    /**
     * Outputs a message to the toConsole.
     * 
     * @param message 
     * 
     * @since 0.1
     */
    public static void toConsole(String message) {
        log.info("[Revenge] " + message);
    }
    
    /**
     * Revenge Entity Listener
     * 
     * @author simplyianm
     * @since 0.1
     */
    public class RVEL extends EntityListener {
        /**
         * Triggered when an entity dies.
         * 
         * @param event 
         * 
         * @since 0.1
         */
        @Override
        public void onEntityDeath(EntityDeathEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Revenge.this.takeRevenge((Player) entity);
            }
        }
    }
    
    /**
     * Revenge Server Listener
     * 
     * @author simplyianm
     * @since 0.1
     */
    public class RVSL extends ServerListener {
        /**
         * Methods hook into Register
         */
        private Methods mmeth = new Methods();
        
        /**
         * Triggered when a plugin is enabled.
         * 
         * @param event 
         */
        @Override
        public void onPluginEnable(PluginEnableEvent event) {
            if (this.mmeth.hasMethod() == false) { //If we don't have a method yet
                if(this.mmeth.setMethod(event.getPlugin()) == true) {
                    Revenge.this.registerMethod = this.mmeth.getMethod();
                    Revenge.toConsole("[Revenge] Payment Method Found: " + Revenge.this.registerMethod.getName() + " version " + Revenge.this.registerMethod.getVersion() + ".");
                }
            }
        }
        
        /**
         * Triggered when a plugin is disabled.
         * 
         * @param event
         */
        @Override
        public void onPluginDisable(PluginDisableEvent event) {
            // Check to see if the plugin thats being disabled is the one we are using
            if (this.mmeth != null && this.mmeth.hasMethod()) {
                if(this.mmeth.checkDisabled(event.getPlugin()) == true) {
                    Revenge.this.registerMethod = null;
                    Revenge.toConsole("[Revenge] The payment system of choice was disabled. Hopefully you're shutting down or reloading.");
                }
            }
        }
    }
    
    /**
     * Revenge Types
     * 
     * @since 0.1
     */
    class RevengeType {
        /**
         * Revenge name
         */
        private String name;
        
        /**
         * Revenge display name
         */
        private String niceName;
        
        /**
         * Revenge description
         */
        private String desc;
        
        /**
         * Revenge cost
         */
        private int cost;
        
        /**
         * Revenge effects
         */
        private List<String> effects;
        
        /**
         * Can we get multiple of the same RevengeType?
         */
        private boolean multiple;

        /**
         * Constructor
         * 
         * @param revengeName 
         */
        public RevengeType(String revengeName) {
            Configuration config = new Configuration(Revenge.this.configFile);
            config.load();
            this.name = revengeName;
            this.niceName = config.getString("revenges." + revengeName + ".name", revengeName);
            this.desc = config.getString("revenges." + revengeName + ".description", "None");
            this.cost = config.getInt("revenges." + revengeName + ".cost", 0);
            this.effects = config.getStringList("revenges." + revengeName + ".effects", null);
            this.multiple = config.getBoolean("revenges." + revengeName + ".multiple", false);
            config.save();
        }

        /**
         * Gets the name (unique identifer) of the RevengeType.
         * 
         * @return String
         */
        public String getName() {
            return this.name;
        }

        /**
         * Gets the nice name of the RevengeType.
         * 
         * @return String
         */
        public String getNiceName() {
            return this.niceName;
        }

        /**
         * Gets the description of the RevengeType.
         * 
         * @return String
         */
        public String getDescription() {
            return this.desc;
        }

        /**
         * Gets the cost of the RevengeType.
         * 
         * @return int
         */
        public int getCost() {
            return this.cost;
        }

        /**
         * Gets the effects of the RevengeType as a List.
         * 
         * @return List<String>
         */
        public List<String> getEffects() {
            return this.effects;
        }
        
        /**
         * Checks if we can get multiple of the same RevengeType.
         * 
         * @return boolean
         */
        public boolean canGetMultiple() {
            return this.multiple;
        }
    }
    
    /**
     * Parses the activated revenges of the killed player
     * 
     * @param killed 
     * 
     * @since 0.1
     */
    public void takeRevenge(Player killed) {
        System.out.print("Taking revenge..");
        Configuration config = new Configuration(this.configFile);
        config.load();
        Location deathLocation = killed.getLocation();
        List<String> purchasedRevenges = this.getRevenges(killed.getName());
        if (purchasedRevenges != null) {
            Iterator it = purchasedRevenges.iterator();
            while (it.hasNext()) {
                String revenge = (String) it.next();
                RevengeType rt = revengeList.get(revenge);
                this.parseRevenge(rt, killed);
            }
        }
        config.save();
        purchasedRevenges.clear();
    }
    
    /**
     * Adds a revenge to a player.
     * 
     * @param playerName
     * @param revengeTypeName 
     * 
     * @since 0.1
     */
    public void addRevenge(String playerName, String revengeTypeName) {
        List<String> pRevs = this.getRevenges(playerName);
        pRevs.add(revengeTypeName);
        Configuration config = new Configuration(configFile);
        config.load();
        config.setProperty("players." + playerName + ".revenges", pRevs);
        config.save();
    }  
    
    /**
     * Removes a revenge from a player.
     * 
     * @param playerName
     * @param revengeTypeName 
     * 
     * @since 0.1
     */
    public void removeRevenge(String playerName, String revengeTypeName) {
        List<String> pRevs = this.getRevenges(playerName);
        pRevs.remove(revengeTypeName);
        Configuration config = new Configuration(configFile);
        config.load();
        config.setProperty("players." + playerName + ".revenges", pRevs);
        config.save();
    }
    
    /**
     * Gets all of the revenges a player has purchased. (Or hacked to get)
     * 
     * @param playerName
     * @return List of revenges
     * 
     * @since 0.1
     */
    public List<String> getRevenges(String playerName) {
        if (playerRevenges.containsKey(playerName)) {
            return playerRevenges.get(playerName);
        } else {
            List<String> thePRevs = new ArrayList<String>();
            playerRevenges.put(playerName, thePRevs);
            return thePRevs;
        }
    }
    
    /**
     * Clears a player's revenges.
     * 
     * @param playerName 
     */
    public void clearRevenges(String playerName) {
        if (playerRevenges.containsKey(playerName)) {
            playerRevenges.get(playerName).clear();
        }
    }
    
    /**
     * Parses the revenge of a player.
     * 
     * @param revenge
     * @param player
     * 
     * @since 0.1
     */
    public void parseRevenge(RevengeType revenge, Player player) {
        List<String> efs = revenge.getEffects();
        Iterator it = efs.iterator();
        while (it.hasNext()) {
            String curEffect = (String) it.next();
            String[] revengeParts = curEffect.split(":", 2);
            String revengeEffect = revengeParts[0];
            String[] revengeArgs = new String[0];
            if (revengeParts.length > 1) revengeArgs = revengeParts[1].split("\\.");
            if (revengeEffect.equalsIgnoreCase("msgradius")) { //msgradius:radius.message
                int radiusSendTo;
                if (revengeArgs.length > 1) {
                    try {
                        radiusSendTo = Integer.parseInt(revengeArgs[0]);
                    } catch (NumberFormatException ex) {
                        log.warning("[Revenge] \"" + revengeArgs[0] + "\" is not a valid integer! Defaulting to 20 radius.");
                        radiusSendTo = 20;
                    }
                } else {
                    radiusSendTo = 20;
                }
                String theMessage;
                if (revengeArgs.length > 0) {
                    theMessage = revengeArgs[1];
                } else {
                    theMessage = "Player " + player.getName() + " takes the revenge " + revenge.getNiceName() + "!";
                }
                for (Player playa : this.getServer().getOnlinePlayers()) {
                    if (playa.getLocation().distanceSquared(player.getLocation()) <= radiusSendTo * radiusSendTo) {
                        playa.sendMessage(theMessage);
                    }
                }
            } else if (revengeEffect.equalsIgnoreCase("explosion")) { //explosion:explosionpower
                Long power;
                if (revengeArgs.length > 0) {
                    try {
                        power = Long.parseLong(revengeArgs[0]);
                    } catch(NumberFormatException ex) {
                        log.warning("[Revenge] \"" + revengeArgs[0] + "\" is not a valid long! Defaulting to a standard 4L explosion.");
                        power = 4L;
                    }
                } else {
                    power = 4L;
                }
                if (power > 20L) power = 20L;
                player.getWorld().createExplosion(player.getLocation(), power, true);
            } else if (revengeEffect.equalsIgnoreCase("lightning")) { //lightning:strikeamount
                int strikeAmount;
                if (revengeArgs.length > 0) {
                    try {
                        strikeAmount = Integer.parseInt(revengeArgs[0]);
                    } catch(NumberFormatException ex) {
                        log.warning("[Revenge] \"" + revengeArgs[0] + "\" is not a valid integer! Defaulting to a standard 1 strike.");
                        strikeAmount = 1;
                    }
                } else {
                    strikeAmount = 1;
                }
                for (int strikesDone = 0; strikesDone < strikeAmount; strikesDone++) {
                    player.getWorld().strikeLightning(player.getLocation());
                }
            } else if (revengeEffect.equalsIgnoreCase("mobfromcorpse")) { //mobfromcorpse:mobname.mobamount
                CreatureType ct;
                if (revengeArgs.length > 0) {
                    ct = mobMap.get(revengeArgs[0].toLowerCase());
                } else {
                    ct = CreatureType.ZOMBIE;
                }
                if (ct == null) {
                    log.warning("[Revenge] Invalid creature type '" + revengeArgs[0] + "'; defaulting to zombie.");
                }
                int amount;
                if (revengeArgs.length > 1) {
                    try {
                        amount = Integer.parseInt(revengeArgs[1]);
                    } catch(NumberFormatException ex) {
                        log.warning("[Revenge] \"" + revengeArgs[1] + "\" is not a valid integer for mobfromcorpse! Defaulting to 1 spawned mob.");
                        amount = 1;
                    }
                } else {
                    amount = 1;
                }
                int mobcount = 0;
                while (mobcount < amount) {
                    player.getWorld().spawnCreature(player.getLocation(), ct);
                    mobcount++;
                }
            } else {
                log.warning("[Revenge] The effect '" + curEffect + "' is not valid; skipping.");
            }
        }
    }
}