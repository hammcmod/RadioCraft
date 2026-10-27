package com.arrl.radiocraft.common.radio.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * The symbol a station picks to show on APRS maps, as real APRS has a house, a car, a boat and so on. Drawn with a
 * vanilla item until the icons get their own art.
 */
public enum AprsIcon {
	HOUSE(() -> Items.OAK_DOOR),
	PORTABLE(() -> Items.COMPASS),
	CAR(() -> Items.MINECART),
	BOAT(() -> Items.OAK_BOAT),
	HORSE(() -> Items.SADDLE),
	CAMP(() -> Items.CAMPFIRE),
	TOWER(() -> Items.LIGHTNING_ROD),
	DIGI(() -> Items.ENDER_EYE),
	/** A weather station: H19s with one in range beacon with it. */
	WX(() -> com.arrl.radiocraft.common.init.RadiocraftItems.WEATHER_STATION.get());

	private final Supplier<Item> item;

	AprsIcon(Supplier<Item> item) {
		this.item = item;
	}

	public Item item() {
		return item.get();
	}

	/** The icon of that name, ignoring case; {@link #HOUSE} for anything unknown. */
	public static AprsIcon byName(String name) {
		try {
			return valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch(IllegalArgumentException e) {
			return HOUSE;
		}
	}

	public AprsIcon next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
