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

    private int[] entityTypePriorities;

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

        this.loadEntityTypePriorities();
    }

    private void loadEntityTypePriorities() {
        EntityType[] values = EntityType.values();
        entityTypePriorities = new int[values.length];

        FileConfiguration config = this.getConfig();
        List<EntityType> entityTypes = config.getStringList("entity-type-priorities").stream()
                .map(name -> {
                    try {
                        return EntityType.valueOf(name);
                    } catch (Exception e) {
                        this.getLogger().severe("Моб " + name + " не найден.");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int priority = Integer.MAX_VALUE / 2;
        for (EntityType entityType : entityTypes) {
            entityTypePriorities[entityType.ordinal()] = priority--;
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
            config.getStringList(prefixSection + "ignore-entity-types").stream()
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
        return worldLimit;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.reloadConfig();
        this.loadConfigParams();
        sender.sendMessage("§aКонфиг перезагружен.");
        return true;
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.LOW)
    public void onAdd(EntityAddToWorldEvent event) {
        if(!event.getEntity().isValid()){
            return; // уже удален другим лимитом
        }
        if(event.getEntity().getType() == EntityType.PLAYER) {
            return;
        }
        Entity entity = event.getEntity();
        Location location = entity.getLocation();

        WorldLimit worldLimit = worldLimitMap.getOrDefault(location.getWorld().getName(), defaultWorldLimit);

        MobLimit mobLimit = worldLimit.getMobLimitMap().get(entity.getType());
        if(mobLimit != null){
            Collection<Entity> mobs = (Collection<Entity>)location.getWorld().getEntitiesByClass(mobLimit.getEntityClass());
            if (mobs.size() > mobLimit.getLimitWorld()) {
                Collection<Entity> filteredMobs = filterAndSort(mobs);
                if (filteredMobs.size() > mobLimit.getLimitWorld()) {
                    int doRemove = filteredMobs.size() - mobLimit.getLimitWorld();
                    Iterator<Entity> iterator = filteredMobs.iterator();
                    for (int i = 0; i < doRemove; i++) {
                        iterator.next().remove();
                    }
                    limitedLogs.put(hashCode(1, entity.getType(), location),
                            "Лимит " + entity.getType().name() + " в мире " + filteredMobs.size() + "/" + mobLimit.getLimitWorld() + " локация спавна " +
                                    location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
                    return;
                }
            }

            if (mobs.size() > mobLimit.getLimitNearby()) {
                List<Entity> nearbyEntities = mobs.stream()
                        .filter(wither -> {
                            Location loc = wither.getLocation();
                            return !hasDistance(loc, location, mobLimit.getNearbyRadius());
                        })
                        .collect(Collectors.toList());
                if (nearbyEntities.size() > mobLimit.getLimitNearby()) {
                    Collection<Entity> filteredNearbyEntities = filterAndSort(nearbyEntities);
                    if (filteredNearbyEntities.size() > mobLimit.getLimitNearby()) {
                        int doRemove = filteredNearbyEntities.size() - mobLimit.getLimitNearby();
                        Iterator<Entity> iterator = filteredNearbyEntities.iterator();
                        for (int i = 0; i < doRemove; i++) {
                            iterator.next().remove();
                        }
                        limitedLogs.put(hashCode(2, entity.getType(), location),
                                "Лимит " + entity.getType().name() + " в радиусе " + filteredNearbyEntities.size() + "/" + mobLimit.getLimitNearby() + " локация спавна " +
                                        location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
                        return;
                    }
                }
            }
            return;
        }

        int limit = worldLimit.getCommonLimitNearby();
        if (location.getWorld().getEntityCount() > limit) {
            int radius = worldLimit.getCommonNearbyRadius();
            Collection<Entity> entities = NmsUtils.getNearbyEntities(
                    new Location(location.getWorld(), location.getX() - radius, Integer.MIN_VALUE, location.getZ() - radius),
                    new Location(location.getWorld(), location.getX() + radius, Integer.MAX_VALUE, location.getZ() + radius)
            );
            if (entities.size() > limit) {
                Collection<Entity> filteredEntities = this.filterAndSortAndIgnore(entities, worldLimit);
                if (filteredEntities.size() > limit) {
                    int doRemove = filteredEntities.size() - limit;
                    Iterator<Entity> iterator = filteredEntities.iterator();
                    for (int i = 0; i < doRemove; i++) {
                        iterator.next().remove();
                    }
                    limitedLogs.put(hashCode(3, null, location),
                            "Лимит мобов " + filteredEntities.size() + "/" + limit + " в радиусе от " +
                                    location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
                    return;
                }
            }
        }
    }

    private Comparator<Entity> comparator = Comparator
                    .<Entity>comparingInt(value -> entityTypePriorities[value.getType().ordinal()])
                    .thenComparingInt(value -> -value.getEntityId());

    public List<Entity> filterAndSort(Collection<Entity> entities) {
        return entities.stream()
                .filter(Entity::isValid)
                .filter(entity -> entity.getType() != EntityType.PLAYER)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<Entity> filterAndSortAndIgnore(Collection<Entity> entities, WorldLimit worldLimit) {
        return entities.stream()
                .filter(Entity::isValid)
                .filter(it -> !this.isIgnore(it, worldLimit))
                .filter(entity -> entity.getType() != EntityType.PLAYER)
                .sorted(comparator)
                .collect(Collectors.toList());
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
