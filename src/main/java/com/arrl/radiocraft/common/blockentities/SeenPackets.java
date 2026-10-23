package com.arrl.radiocraft.common.blockentities;

import java.util.Iterator;
import java.util.LinkedHashSet;

/** The last few hundred packet ids a station handled, so a packet heard twice (or its own, coming back) is not news. */
public class SeenPackets {

	private static final int MAX = 256;
	private final LinkedHashSet<Long> seen = new LinkedHashSet<>();

	/** Notes a packet id; false if it was already noted. */
	public boolean remember(long id) {
		if(!seen.add(id))
			return false;
		if(seen.size() > MAX) {
			Iterator<Long> oldest = seen.iterator();
			oldest.next();
			oldest.remove();
		}
		return true;
	}
}
