package ua.lokha.mobradiuslimiter;

import lombok.Data;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

@Data
public class WorldLimit {
    private Map<EntityType, MobLimit> mobLimitMap = new EnumMap<>(EntityType.class);
    private EnumSet<EntityType> ignoreEntityTypes = EnumSet.noneOf(EntityType.class);
    private int commonLimitNearby; // лимит в радиусе
    private int commonNearbyRadius;
}
