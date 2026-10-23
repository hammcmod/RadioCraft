package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.radio.data.TncPacket;

/**
 * A block on the station bus that uses radio interfaces rather than being one: it gets every packet the interfaces on
 * its bus decode, and sends through them. See {@link StationBus}.
 */
public interface StationTerminal {

	/**
	 * A packet a radio interface on the bus decoded. With two interfaces on one bus it arrives once from each, so a
	 * terminal keeps track of the packet ids it has seen.
	 */
	void receive(TncPacket packet);
}
