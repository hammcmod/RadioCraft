package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.datacomponents.HandheldRadioState;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import net.minecraft.world.item.ItemStack;

import java.util.function.UnaryOperator;

public class VHFHandheldCapability implements IVHFHandheldCapability {

	private ItemStack installedBattery = ItemStack.EMPTY;
//	private boolean isPowered = false;
//	private int frequencyKiloHertz = 0;
//	private boolean isPTTDown = false;
	private final ItemStack thisRadio;
	//TODO temporary hack for testing, must be changed to use data attachment API for storing state, and produce a new capability on each request
//	private static final WeakHashMap<ItemStack, VHFHandheldCapability> capabilities = new WeakHashMap<>();

	public VHFHandheldCapability(ItemStack thisRadio) {
		this.thisRadio = thisRadio;
	}

	public static IVHFHandheldCapability getCapability(ItemStack radioItemstack) {
		return new VHFHandheldCapability(radioItemstack);
	}

	@Override
	public ItemStack getBattery() {
		return installedBattery;
	} //TODO add item to datacomponent

	@Override
	public void setBattery(ItemStack item) {
		installedBattery = item;
	}

	@Override
	public int getFrequencyKiloHertz() {
		return getState().freq();
	}

	@Override
	public void setFrequencyKiloHertz(int frequencyKiloHertz) {
		updateState((old) -> new HandheldRadioState(old.power(), old.ptt(), frequencyKiloHertz, old.receiveIndicatorStrength()));
	}

	@Override
	public boolean isPowered() {
		return getState().power();
	}

	@Override
	public void setPowered(boolean value) {
		updateState((old) -> new HandheldRadioState(value, old.ptt(), old.freq(), old.receiveIndicatorStrength()));
	}

	@Override
	public boolean isPTTDown() {
		return getState().ptt();
	}

	@Override
	public void setPTTDown(boolean value) {
		updateState((old) -> new HandheldRadioState(old.power(), value, old.freq(), old.receiveIndicatorStrength()));
	}

	@Override
	public void setReceiveStrength(float rec) {
		updateState((old) -> new HandheldRadioState(old.power(), old.ptt(), old.freq(), rec));
	}

	@Override
	public float getReceiveStrength() {
		return getState().receiveIndicatorStrength();
	}

	protected HandheldRadioState getState(){
		return thisRadio.getOrDefault(RadiocraftDataComponent.HANDHELD_RADIO_STATE_COMPONENT.value(), HandheldRadioState.getDefault());
	}

	protected void updateState(UnaryOperator<HandheldRadioState> updater){
		thisRadio.update(
				RadiocraftDataComponent.HANDHELD_RADIO_STATE_COMPONENT.value(),
				HandheldRadioState.getDefault(),
				updater
		);
	}
}
