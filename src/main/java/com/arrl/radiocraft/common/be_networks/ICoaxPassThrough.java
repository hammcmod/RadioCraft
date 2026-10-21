package com.arrl.radiocraft.common.be_networks;

/**
 * A coax device the signal goes through rather than ending at: the antenna tuner (and the duplexer, once it does
 * something). The wire walk in {@link WireUtils} carries on out of the other sides of such a block, so a radio and an
 * antenna with one of these in between still end up on one coax network, with the device as a member.
 */
public interface ICoaxPassThrough extends ICoaxNetworkObject, IPassThroughNetworkObject {
}
