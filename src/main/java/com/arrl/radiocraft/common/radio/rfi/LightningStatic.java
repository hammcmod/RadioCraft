package com.arrl.radiocraft.common.radio.rfi;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Lightning crashes (QRN): every lightning strike puts a burst of static on the radios that can hear it. A real
 * strike radiates a broadband pulse that HF receivers pick up for hundreds of kilometres, loudest on the long bands,
 * so here a strike anywhere within {@code lightning_static_range} of a powered station pops its interference level,
 * scaled by distance and by the same band factor as redstone noise. The level decays on its own
 * ({@link InterferenceLevel}), so a strike is a crash that is gone about a second later, as the proposal asks.
 * <p>
 * Stations are found through the redstone pickup zone registry, which already holds exactly the radios that play
 * static (loaded, powered, one antenna), so a strike costs one distance per station and nothing else. Only strikes
 * matter: thunderstorm weather on its own adds nothing.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class LightningStatic {

	@SubscribeEvent
	public static void entityJoin(EntityJoinLevelEvent event) {
		if(event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level)
			strike(level, bolt.position());
	}

	/** Pops every station in {@code level} that is within range of a strike at {@code pos}. Server thread only. */
	public static void strike(ServerLevel level, Vec3 pos) {
		if(!RadiocraftServerConfig.LIGHTNING_STATIC_ENABLED.get())
			return;
		double strength = RadiocraftServerConfig.LIGHTNING_STATIC_STRENGTH.get();
		double range = RadiocraftServerConfig.LIGHTNING_STATIC_RANGE.get();
		if(strength <= 0.0D)
			return;
		for(PickupZone zone : RedstoneInterference.getZones(level)) {
			double falloff = falloff(com.arrl.radiocraft.compat.sable.Sublevels.worldCenter(level, zone.radioPos()).distanceTo(pos), range);
			if(falloff > 0.0D && level.getBlockEntity(zone.radioPos()) instanceof RadioBlockEntity radio)
				radio.addDirectInterference(strength * falloff * RedstoneInterference.bandFactor(radio.getBand()));
		}
	}

	/** How much of a strike {@code distance} blocks away reaches a station: 1 on top of it, falling linearly to 0 at {@code range}. */
	public static double falloff(double distance, double range) {
		if(range <= 0.0D || distance >= range)
			return 0.0D;
		return Math.max(0.0D, 1.0D - distance / range);
	}
}
