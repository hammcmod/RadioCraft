package com.arrl.radiocraft.common.be_networks;

/**
 * A network object that is one node for all of its sides: the wire walk goes in on one side and out of every
 * other, so everything reachable from any side ends up on one network. The antenna tuner is one on the coax (see
 * {@link ICoaxPassThrough}); a solar panel is one on the power side, so a row of panels feeds the charge controller
 * at its end.
 */
public interface IPassThroughNetworkObject {
}
