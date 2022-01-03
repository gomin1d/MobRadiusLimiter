package ua.lokha.mobradiuslimiter;

import lombok.Data;
import org.bukkit.entity.Entity;

@Data
public class MobLimit{
    private int limitNearby; // лимит в радиусе
    private int nearbyRadius;
    private int limitWorld; // лимит во всем мире
    private Class<? extends Entity> entityClass;

    public MobLimit(int limitNearby, int nearbyRadius, int limitWorld, Class<? extends Entity> entityClass){
        this.limitNearby = limitNearby;
        this.nearbyRadius = nearbyRadius;
        this.limitWorld = limitWorld;
        this.entityClass = entityClass;
    }
}
