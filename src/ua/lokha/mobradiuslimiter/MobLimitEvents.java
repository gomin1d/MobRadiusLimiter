package ua.lokha.mobradiuslimiter;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Лимиты с мобами
 */
public class MobLimitEvents implements Listener {

	private Main main;

	private Map<EntityType, MobLimit> mobLimitMap = new EnumMap<>(EntityType.class);

	public MobLimitEvents(Main main){
		this.main = main;
		mobLimitMap.put(EntityType.WITHER, new MobLimit(2, 800, 10, Wither.class));
		mobLimitMap.put(EntityType.SQUID, new MobLimit(10, 800, 40, Squid.class));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onAdd(EntityAddToWorldEvent event) {
		Entity entity = event.getEntity();
		Location location = entity.getLocation();
		if(entity.getType() == EntityType.PLAYER) return;

		MobLimit mobLimit = mobLimitMap.get(entity.getType());
		if(mobLimit != null){
			Collection<? extends Entity> mobs = location.getWorld().getEntitiesByClass(mobLimit.getEntityClass());
			if (mobs.size() > mobLimit.getLimitWorld()) {
				entity.remove();
				mobs.stream()
						.skip(mobLimit.getLimitWorld())
						.filter(Entity::isValid)
						.forEach(Entity::remove);
				main.getLogger().warning(
						"Лимит " + entity.getType().name() + " в мире " + mobs.size() + "/" + mobLimit.getLimitWorld() + " локация спавна " +
								location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
			} else {
				List<? extends Entity> nearbyEntities = mobs.stream()
						.filter(wither -> {
							Location loc = wither.getLocation();
							return !hasDistance2D(loc.getX(), loc.getZ(), location.getX(), location.getZ(), mobLimit.getNearbyRadius());
						})
						.collect(Collectors.toList());
				if (nearbyEntities.size() > mobLimit.getLimitNearby()) {
					entity.remove();
					mobs.stream()
							.skip(mobLimit.getLimitNearby())
							.filter(Entity::isValid)
							.forEach(Entity::remove);
					main.getLogger().warning(
							"Лимит " + entity.getType().name() + " в радиусе " + mobs.size() + "/" + mobLimit.getLimitNearby() + " локация спавна " +
									location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
				}
			}
		} else {
			Collection<Entity> entities = location.getWorld().getNearbyEntities(location, 800, 10000, 800);
			int limit = 400;
			if (entities.size() > limit) {
				entity.remove();
				entities.stream()
						.skip(limit)
						.filter(Entity::isValid)
						.filter(e -> !(e instanceof Player))
						.forEach(Entity::remove);
				main.getLogger().warning(
						"Лимит мобов " + entities.size() + "/" + limit + " в радиусе от " +
								location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
			}
		}
	}

	public static boolean hasDistance2D(double x1, double z1, double x2, double z2, double distance) {
		return Math.abs(x1 - x2) >= distance
				|| Math.abs(z1 - z2) >= distance;
	}

	public static class MobLimit{
		private int limitNearby; // лимит в радиусе
		private int nearbyRadius = 800;
		private int limitWorld; // лимит во всем мире
		private Class<? extends Entity> entityClass;

		public MobLimit(int limitNearby, int nearbyRadius, int limitWorld, Class<? extends Entity> entityClass){
			this.limitNearby = limitNearby;
			this.nearbyRadius = nearbyRadius;
			this.limitWorld = limitWorld;
			this.entityClass = entityClass;
		}

		public int getLimitNearby() {
			return limitNearby;
		}

		public int getNearbyRadius() {
			return nearbyRadius;
		}

		public int getLimitWorld() {
			return limitWorld;
		}

		public Class<? extends Entity> getEntityClass() {
			return entityClass;
		}

		@Override
		public String toString() {
			return "MobLimit{" +
					"limitNearby=" + limitNearby +
					", nearbyRadius=" + nearbyRadius +
					", limitWorld=" + limitWorld +
					", entityClass=" + entityClass +
					'}';
		}
	}

	public Map<EntityType, MobLimit> getMobLimitMap() {
		return mobLimitMap;
	}
}
