package com.arrl.radiocraft.common.radio.microwave;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a station (H19, direction finder, fax, telephone) reaches over microwave. The radios at one site, touching
 * each other or on one power wire ({@link MicrowaveRadioBlockEntity#siteMates}), are one data network, like radios
 * on a site's switch: what comes in on one link goes on out of the others. So a chain of sites relays end to end, up
 * to {@code microwave_max_hops} links. Every station served by a radio along the way is reachable; a station at this
 * site on another radio is reachable with no link at all.
 */
public final class MicrowaveRoutes {

	private MicrowaveRoutes() {}

	/**
	 * A way to {@code station}: {@code first} the radio at the starting site that carries it out, {@code last} the radio
	 * serving it at the far end, {@code hops} the links crossed, {@code senders} the radio sending on each of them and
	 * {@code ends} the radios at both ends of every link.
	 */
	public record Route(BlockEntity station, MicrowaveRadioBlockEntity first, MicrowaveRadioBlockEntity last, int hops,
						List<MicrowaveRadioBlockEntity> senders, List<MicrowaveRadioBlockEntity> ends) {

		/** True if a payload makes it the whole way this time: it must get through on every link (interference loses half). */
		public boolean getsThrough() {
			for(MicrowaveRadioBlockEntity radio : senders)
				if(!radio.getsThrough())
					return false;
			return true;
		}

		/** True if any link on the way has interference or a signal too weak for a clean call. */
		public boolean lossy() {
			for(MicrowaveRadioBlockEntity radio : ends)
				if(radio.hasInterference() || radio.isMarginal())
					return true;
			return false;
		}
	}

	/** The most links a route crosses (server config {@code microwave_max_hops}). */
	public static int maxHops() {
		try {
			return RadiocraftServerConfig.MICROWAVE_MAX_HOPS.get();
		}
		catch(IllegalStateException e) { // Config not loaded yet.
			return 8;
		}
	}

	/** How a radio was reached: the radio it came from, and whether that was across a link (else at the same site). */
	private record Step(@Nullable MicrowaveRadioBlockEntity from, boolean link, int hops) {}

	/**
	 * Every station reachable from the station at {@code pos}, each once, by the route with the fewest links (the
	 * station itself left out). Empty when no powered microwave radio serves it.
	 */
	public static List<Route> from(Level level, BlockPos pos) {
		BlockEntity self = level.getBlockEntity(pos);
		Map<MicrowaveRadioBlockEntity, Step> reached = new LinkedHashMap<>();
		Deque<MicrowaveRadioBlockEntity> queue = new ArrayDeque<>();
		for(MicrowaveRadioBlockEntity radio : MicrowaveRadioBlockEntity.servingStation(level, pos))
			if(radio.isPowered() && reached.putIfAbsent(radio, new Step(null, false, 0)) == null)
				queue.add(radio);
		int max = maxHops();
		// Fewest links first (a site costs none): a radio reached again by a shorter way is taken up again from there.
		while(!queue.isEmpty()) {
			MicrowaveRadioBlockEntity radio = queue.poll();
			int hops = reached.get(radio).hops();
			for(MicrowaveRadioBlockEntity mate : radio.siteMates()) {
				Step known = reached.get(mate);
				if(mate.isPowered() && (known == null || known.hops() > hops)) {
					reached.put(mate, new Step(radio, false, hops));
					queue.addFirst(mate); // Still this many hops: before anything further out.
				}
			}
			if(hops >= max)
				continue;
			for(MicrowaveRadioBlockEntity peer : radio.peers()) {
				Step known = reached.get(peer);
				if(known == null || known.hops() > hops + 1) {
					reached.put(peer, new Step(radio, true, hops + 1));
					queue.addLast(peer);
				}
			}
		}

		Map<BlockEntity, Route> routes = new LinkedHashMap<>();
		Map<MicrowaveRadioBlockEntity, Route> paths = new HashMap<>();
		List<MicrowaveRadioBlockEntity> byHops = new ArrayList<>(reached.keySet());
		byHops.sort((a, b) -> Integer.compare(reached.get(a).hops(), reached.get(b).hops()));
		for(MicrowaveRadioBlockEntity radio : byHops) {
			Route path = path(radio, reached, paths);
			for(BlockEntity station : radio.stations()) {
				Route existing = routes.get(station);
				if(station != self && (existing == null || existing.hops() > path.hops()))
					routes.put(station, new Route(station, path.first(), radio, path.hops(), path.senders(), path.ends()));
			}
		}
		return new ArrayList<>(routes.values());
	}

	/** The route to {@code radio} (its station left null), built from the steps back to a starting radio. */
	private static Route path(MicrowaveRadioBlockEntity radio, Map<MicrowaveRadioBlockEntity, Step> reached, Map<MicrowaveRadioBlockEntity, Route> paths) {
		Route known = paths.get(radio);
		if(known != null)
			return known;
		Step step = reached.get(radio);
		Route route;
		if(step.from() == null)
			route = new Route(null, radio, radio, 0, List.of(), List.of());
		else {
			Route before = path(step.from(), reached, paths);
			List<MicrowaveRadioBlockEntity> senders = new ArrayList<>(before.senders()), ends = new ArrayList<>(before.ends());
			if(step.link()) {
				senders.add(step.from());
				ends.add(step.from());
				ends.add(radio);
			}
			route = new Route(null, before.first(), radio, step.hops(), List.copyOf(senders), List.copyOf(ends));
		}
		paths.put(radio, route);
		return route;
	}

	/** The route from the station at {@code pos} to {@code station}, or null if it is out of reach. */
	@Nullable
	public static Route to(Level level, BlockPos pos, BlockEntity station) {
		for(Route route : from(level, pos))
			if(route.station() == station)
				return route;
		return null;
	}
}
