package com.saro476.globaleffects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;


public class GlobalEffects extends JavaPlugin implements Listener {

    // private PotionEffect[] effects = new PotionEffect[2];
    private FileConfiguration config = null;
    private HashMap<PotionEffectType,PotionEffect> effects = new HashMap<PotionEffectType,PotionEffect>();

    private boolean debug = false;


    @Override
    public void onEnable() {
        getLogger().info("=========================");
        getLogger().info("      GlobalEffects"      );
        getLogger().info("=========================");

        debugLogger("Enabled");


        // Configuration file
        saveDefaultConfig();
        this.config = getConfig();
        this.debug = this.config.getBoolean("debug");

        // Register all effects
        registerEffects();

        // Register listener
        getServer().getPluginManager().registerEvents(this, this);
    }
    @Override
    public void onDisable() {
        this.effects.clear();
        debugLogger("Disabled");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        if ( event == null || event.getPlayer() == null || !checkWorld(event.getPlayer()) ) {
            return;
        }
        
        debugLogger(event.getPlayer().getName() + " joined the world, delaying effects " + this.config.getInt("join-delay") + " ticks" );

        HashMap<PotionEffectType,PotionEffect> effects = this.effects;

        new BukkitRunnable() {
            @Override
            public void run() {
                debugLogger( "Applying delayed effects for " + event.getPlayer().getName() );
                for ( Entry<PotionEffectType,PotionEffect> entry : effects.entrySet() ) {
                    resetEffect(event.getPlayer(), entry.getKey());
                }
            }
        }.runTaskLater(this, this.config.getInt("join-delay"));
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {

        if ( event == null || event.getPlayer() == null || !checkWorld(event.getPlayer()) ) {
            return;
        }

        debugLogger(event.getPlayer().getName() + " changed worlds, applying effects");

        for ( Entry<PotionEffectType,PotionEffect> entry : this.effects.entrySet() ) {
            resetEffect(event.getPlayer(), entry.getKey());
        }

    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {

        if ( event.isCancelled()
            || event.getCause() == EntityPotionEffectEvent.Cause.UNKNOWN 
            || event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN 
            || event.getCause() == EntityPotionEffectEvent.Cause.COMMAND ) {
            // Ignore events caused by this or other plugins
            return;
        }

        // Handle stacking new effects
        if ( event.getEntityType() == EntityType.PLAYER ) {
            Player p = (Player) event.getEntity();
                    
            boolean modified = false;
            // Handle stacking new effects
            if ( event.getNewEffect() != null ) {
                modified = addEffect(p, event.getNewEffect());
            }
            // Handle keeping default effects
            else {
                modified = resetEffect(p, event.getOldEffect().getType() );
            }

            if ( modified ) {
                event.setCancelled(true);
            }
        }
    }

    private boolean addEffect( Player p, PotionEffect e ) {

        if (p == null || e == null) {
            return false;
        }

        PotionEffect e2 = this.effects.get(e.getType());

        PotionEffect e3 = new PotionEffect(e.getType(), e.getDuration(), e.getAmplifier() + e2.getAmplifier(), e.isAmbient(), e.hasParticles(), e.hasIcon() );

        return applyEffect(p, e3);
    }

    private boolean resetEffect( Player p, PotionEffectType et ) {

        if (p == null || et == null) {
            return false;
        }
        PotionEffect e = this.effects.get(et);

        return applyEffect(p, e);
    }

    private boolean applyEffect( Player p, PotionEffect e ) {

        if (p == null || e == null) {
            return false;
        }

        boolean applied = false;

        if ( checkWorld(p) && e.getAmplifier() >= 0 ) {
            debugLogger("Applying effect " + e.getType() + "(" + e.getAmplifier() + ") to " + p.getName() + " for " + e.getDuration() + " ticks");
            p.removePotionEffect(e.getType());
            p.addPotionEffect(e);
            applied = true;
        }

        return applied;
    }

    private boolean checkWorld( Player p ) {
        if (p.getLocation().getWorld().getName().endsWith("_nether")) {
            return this.config.getBoolean( "worlds.nether");
        } else if (p.getLocation().getWorld().getName().endsWith("_the_end")) {
            return this.config.getBoolean( "worlds.the_end");
        } else {
            return this.config.getBoolean( "worlds.overworld");
        }
    }

    private void registerEffects() {
        infoLogger("Registering effects");

        ConfigurationSection section = this.config.getConfigurationSection("effects");

        Set<String> keys = section.getKeys(false);

        int duration = Math.max(this.config.getInt("duration"),20);

        int effectCounter = 0;
        for( String key : keys ) {

            if (section.getInt( key ) != 0) {
                effectCounter++;
            }

            this.effects.put( 
                PotionEffectType.getByName(key),
                new PotionEffect(
                    PotionEffectType.getByName(key),
                    duration,
                    section.getInt( key ) - 1,
                    false,
                    false,
                    false
                )
            );
        }

        infoLogger( "GlobalEffects: " + Integer.toString(effectCounter) + " active effects registered");
    }

    private void infoLogger(String s) {
        getLogger().info( "[GlobalEffects] " + s );
    }

    private void debugLogger(String s) {
        if (this.debug) {
            infoLogger( "(debug) " + s );
        }
    }

}