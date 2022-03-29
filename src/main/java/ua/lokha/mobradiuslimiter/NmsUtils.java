package ua.lokha.mobradiuslimiter;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NmsUtils {
    public static List<Entity> getNearbyEntities(Location min, Location max) {
        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();

        double maxX = max.getX();
        double maxY = max.getY();
        double maxZ = max.getZ();

        return ((CraftWorld) min.getWorld()).getHandle().entityList.stream().filter(entity -> entity.locX >= minX &&
                        entity.locX <= maxX &&
                        entity.locY >= minY &&
                        entity.locY <= maxY &&
                        entity.locZ >= minZ &&
                        entity.locZ <= maxZ)
                .map(net.minecraft.server.v1_12_R1.Entity::getBukkitEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
