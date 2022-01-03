//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ua.lokha.mobradiuslimiter;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    private WorldLimit defaultWorldLimit;
    private Map<String, WorldLimit> worldLimitMap;

    private Map<Integer, String> limitedLogs = new HashMap<>();

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                for (String log : limitedLogs.values()) {
                    this.getLogger().warning(log);
                }
            } finally {
                limitedLogs.clear();
            }
        }, 20, 20);

        this.saveDefaultConfig();
        this.loadConfigParams();
        this.getCommand("mobradiuslimiter").setExecutor(this);
    }

    public void loadConfigParams() {
        FileConfiguration config = this.getConfig();
        defaultWorldLimit = this.loadWorldLimit(config, "");
        this.getLogger().info("Загружен лимит по умолчанию " + defaultWorldLimit);
        ConfigurationSection section = config.getConfigurationSection("world-limits");
        worldLimitMap = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    WorldLimit worldLimit = this.loadWorldLimit(config, "world-limits." + key + ".");
                    worldLimitMap.put(key, worldLimit);
                    this.getLogger().info("Загружен лимит мира " + key + ": " + worldLimit);
                } catch (Exception e) {
                    this.getLogger().severe("Ошибка загрузки лимита мира " + key);
                    e.printStackTrace();
                }
            }
        }
    }

    public WorldLimit loadWorldLimit(FileConfiguration config, String prefixSection) {
        WorldLimit worldLimit = new WorldLimit();
        worldLimit.getMobLimitMap().clear();
        for (String mobType : config.getConfigurationSection(prefixSection + "limits").getKeys(false)) {
            try {
                EntityType entityType = EntityType.valueOf(mobType);
                Number limitNearby = (Number) config.get(prefixSection + "limits." + mobType + ".limitNearby");
                Number nearbyRadius = (Number) config.get(prefixSection + "limits." + mobType + ".nearbyRadius");
                Number limitWorld = (Number) config.get(prefixSection + "limits." + mobType + ".limitWorld");
                MobLimit mobLimit = new MobLimit(
                        limitNearby.intValue(),
                        nearbyRadius.intValue(),
                        limitWorld.intValue(),
                        entityType.getEntityClass()
                );
                worldLimit.getMobLimitMap().put(entityType, mobLimit);
            } catch (Exception e) {
                this.getLogger().severe("Ошибка обработки limits." + mobType);
                e.printStackTrace();
            }
        }

        try {
            // обратная совместимость
            Object globalLimits = config.get(prefixSection + "global-limits");
            if (globalLimits != null) {
                config.set(prefixSection + "common-limit", globalLimits);
            }

            worldLimit.setCommonLimitNearby(((Number) config.get(prefixSection + "common-limit.limitNearby")).intValue());
            worldLimit.setCommonNearbyRadius(((Number) config.get(prefixSection + "common-limit.nearbyRadius")).intValue());
        } catch (Exception e) {
            worldLimit.setCommonLimitNearby(400);
            worldLimit.setCommonNearbyRadius(800);
            this.getLogger().severe("Ошибка загрузки common-limit");
            e.printStackTrace();
        }

        try {
            worldLimit.getIgnoreEntityTypes().clear();
            this.getConfig().getStringList("ignore-entity-types").stream()
                    .map(name -> {
                        try {
                            return EntityType.valueOf(name);
                        } catch (Exception e) {
                            this.getLogger().severe("Моб " + name + " не найден.");
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(worldLimit.getIgnoreEntityTypes()::add);
        } catch (Exception e) {
            this.getLogger().severe("Ошибка загрузки ignore-entity-types");
            e.printStackTrace();
        }
        worldLimit.getIgnoreEntityTypes().add(EntityType.PLAYER);
        return worldLimit;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.reloadConfig();
        this.loadConfigParams();
        sender.sendMessage("§aКонфиг перезагружен.");
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAdd(EntityAddToWorldEvent event) {
        if(!event.getEntity().isValid()){
            return; // уже удален другим лимитом
        }

        Entity entity = event.getEntity();
        Location location = entity.getLocation();

        WorldLimit worldLimit = worldLimitMap.getOrDefault(location.getWorld().getName(), defaultWorldLimit);

        MobLimit mobLimit = worldLimit.getMobLimitMap().get(entity.getType());
        if(mobLimit != null){
            Collection<? extends Entity> mobs = location.getWorld().getEntitiesByClass(mobLimit.getEntityClass());
            if (mobs.size() > mobLimit.getLimitWorld()) {
                entity.remove();
                mobs.stream()
                        .skip(mobLimit.getLimitWorld())
                        .filter(Entity::isValid)
                        .forEach(Entity::remove);
                limitedLogs.put(hashCode(1, entity.getType(), location),
                        "Лимит " + entity.getType().name() + " в мире " + mobs.size() + "/" + mobLimit.getLimitWorld() + " локация спавна " +
                                location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
            } else {
                List<? extends Entity> nearbyEntities = mobs.stream()
                        .filter(wither -> {
                            Location loc = wither.getLocation();
                            return !hasDistance(loc, location, mobLimit.getNearbyRadius());
                        })
                        .collect(Collectors.toList());
                if (nearbyEntities.size() > mobLimit.getLimitNearby()) {
                    entity.remove();
                    nearbyEntities.stream()
                            .skip(mobLimit.getLimitNearby())
                            .filter(Entity::isValid)
                            .forEach(Entity::remove);
                    limitedLogs.put(hashCode(2, entity.getType(), location),
                            "Лимит " + entity.getType().name() + " в радиусе " + nearbyEntities.size() + "/" + mobLimit.getLimitNearby() + " локация спавна " +
                                    location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
                }
            }
        } else {
            Collection<Entity> entities = location.getWorld().getNearbyEntities(location, worldLimit.getCommonNearbyRadius(), 10000, worldLimit.getCommonNearbyRadius());
            int limit = worldLimit.getCommonLimitNearby();
            if (entities.size() > limit) {
                entity.remove();
                entities.stream()
                        .skip(limit)
                        .filter(Entity::isValid)
                        .filter(it -> !this.isIgnore(it, worldLimit))
                        .forEach(Entity::remove);
                limitedLogs.put(hashCode(3, null, location),
                        "Лимит мобов " + entities.size() + "/" + limit + " в радиусе от " +
                                location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
            }
        }
    }

    public static int hashCode(int typeMessage, EntityType type, Location chunkLocation) {
        int result = 1;

        result = 31 * result + typeMessage;
        result = 31 * result + (type == null ? -1 : type.ordinal());
        result = 31 * result + chunkLocation.getWorld().hashCode();
        result = 31 * result + chunkLocation.getBlockX() >> 4;
        result = 31 * result + chunkLocation.getBlockZ() >> 4;

        return result;
    }

    private boolean isIgnore(Entity entity, WorldLimit worldLimit) {
        return worldLimit.getIgnoreEntityTypes().contains(entity.getType());
    }

    public static boolean hasDistance(Location to, Location from, double distance) {
        return !to.getWorld().equals(from.getWorld()) ||
                (Math.abs(to.getX() - from.getX()) >= distance
                        || Math.abs(to.getY() - from.getY()) >= distance
                        || Math.abs(to.getZ() - from.getZ()) >= distance);
    }
}
