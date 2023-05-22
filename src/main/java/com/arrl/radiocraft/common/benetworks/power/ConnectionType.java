package com.arrl.radiocraft.common.benetworks.power;

public enum ConnectionType {
	PULL,
	PUSH,
	NO_INTERACT // This should only ever be used for objects which use their own push/pull logic.
}
