package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.datacomponents.HandheldRadioState;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.world.item.ItemStack;

import java.util.function.UnaryOperator;

public class VHFHandheldCapability implements IVHFHandheldCapability {

	private ItemStack installedBattery = ItemStack.EMPTY;
//	private boolean isPowered = false;
//	private int frequencyHertz = 0;
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
	public float getFrequencyHertz() {
		return getState().freq();
	}

    @Override
    public void setFrequencyHertz(float frequencyHertz) {
        updateState(
            (old) -> new HandheldRadioState(
                old.power(),
                old.ptt(),
                Math.max(
                        Math.min(
                                frequencyHertz,
                                Band.getBand(2).maxFrequency()
                        ),
                        Band.getBand(2).minFrequency()
                ),
                old.gain(),
                old.micGain(),
                old.receiveIndicatorStrength(),
                old.vox()
            )
        );
    }	@Override
	public boolean isPowered() {
		return getState().power();
	}

    @Override
    public void setPowered(boolean value) {
        updateState((old) -> new HandheldRadioState(value, old.ptt(), old.freq(), old.gain(), old.micGain(), old.receiveIndicatorStrength(), old.vox()));
    }	@Override
	public boolean isPTTDown() {
		return getState().ptt();
	}

    @Override
    public void setPTTDown(boolean value) {
        updateState((old) -> new HandheldRadioState(old.power(), value, old.freq(), old.gain(), old.micGain(), old.receiveIndicatorStrength(), old.vox()));
    }    @Override
    public void setReceiveStrength(float rec) {
        updateState((old) -> new HandheldRadioState(old.power(), old.ptt(), old.freq(), old.gain(), old.micGain(), rec, old.vox()));
    }	@Override
	public float getReceiveStrength() {
		return getState().receiveIndicatorStrength();
	}

	@Override
	public float getGain() {
		return getState().gain();
	}

    @Override
    public void setGain(float gain) {
        updateState((old) -> new HandheldRadioState(old.power(), old.ptt(), old.freq(), gain, old.micGain(), old.receiveIndicatorStrength(), old.vox()));
    }	@Override
	public float getMicGain() {
		return getState().micGain();
	}

	@Override
	public void setMicGain(float micGain) {
		updateState((old) -> new HandheldRadioState(old.power(), old.ptt(), old.freq(), old.gain(), micGain, old.receiveIndicatorStrength(), old.vox()));
	}

	@Override
	public boolean isVoxEnabled() {
		return getState().vox();
	}

	@Override
	public void setVoxEnabled(boolean value) {
		updateState((old) -> new HandheldRadioState(old.power(), old.ptt(), old.freq(), old.gain(), old.micGain(), old.receiveIndicatorStrength(), value));
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
