//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ua.lokha.mobradiuslimiter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private MobLimitEvents events;

    public void onEnable() {
        events = new MobLimitEvents(this);
        Bukkit.getPluginManager().registerEvents(events, this);
        this.saveDefaultConfig();
        this.loadConfigParams();
        this.getCommand("mobradiuslimiter").setExecutor(this);
    }

    public void loadConfigParams() {
        FileConfiguration config = this.getConfig();
        events.getMobLimitMap().clear();
        for (String mobType : config.getConfigurationSection("limits").getKeys(false)) {
            try {
                EntityType entityType = EntityType.valueOf(mobType);
                Number limitNearby = (Number) config.get("limits." + mobType + ".limitNearby");
                Number nearbyRadius = (Number) config.get("limits." + mobType + ".nearbyRadius");
                Number limitWorld = (Number) config.get("limits." + mobType + ".limitWorld");
                MobLimitEvents.MobLimit mobLimit = new MobLimitEvents.MobLimit(
                        limitNearby.intValue(),
                        nearbyRadius.intValue(),
                        limitWorld.intValue(),
                        entityType.getEntityClass()
                );
                events.getMobLimitMap().put(entityType, mobLimit);
                this.getLogger().info("Загрузили лимит: " + mobLimit);
            } catch (Exception e) {
                this.getLogger().severe("Ошибка обработки limits." + mobType);
                e.printStackTrace();
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.reloadConfig();
        this.loadConfigParams();
        sender.sendMessage("§aКонфиг перезагружен.");
        return true;
    }
}
