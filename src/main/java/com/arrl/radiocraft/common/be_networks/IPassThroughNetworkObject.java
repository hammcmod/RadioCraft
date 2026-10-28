package com.arrl.radiocraft.common.be_networks;

/**
 * A network object that is one node for all of its sides: the wire walk goes in on one side and out of every
 * other, so everything reachable from any side ends up on one network. The antenna tuner is one on the coax (see
 * {@link ICoaxPassThrough}); a solar panel is one on the power side, so a row of panels feeds the charge controller
 * at its end. A device on both kinds of network can pass one through and not the other ({@link #passesThrough}): a
 * direction finder module hands power on to the finder and the next module but keeps its own antenna's coax to itself.
 */
public interface IPassThroughNetworkObject {

	/** True if this object passes the power walk ({@code power}) or the coax walk through. Both by default. */
	default boolean passesThrough(boolean power) {
		return true;
	}
}
