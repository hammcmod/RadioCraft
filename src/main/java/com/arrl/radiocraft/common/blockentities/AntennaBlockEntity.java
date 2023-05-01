package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.AntennaManager;
import com.arrl.radiocraft.common.radio.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.voice.AntennaNetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared BlockEntity for all blocks which act as an antenna-- used for processing packets/sending them to the network, receiving packets from the network & scheduling antenna update checks.
 */
public class AntennaBlockEntity extends BlockEntity {

	private Antenna<?> antenna = null;

	public AntennaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void transmitAudioPacket(short[] rawAudio, int wavelength, int frequency) {
		antenna.transmitAudioPacket(rawAudio, wavelength, frequency);
	}

	public void receiveAudioPacket(AntennaNetworkPacket packet) {

	}

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		if(antenna != null) {
			nbt.putString("antennaType", antenna.type.getId().toString());
			nbt.put("antennaData", antenna.serializeNBT());
		}
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		if(nbt.contains("antennaType")) {
			IAntennaType<?> type = AntennaTypes.getType(new ResourceLocation(nbt.getString("type")));
			if(type != null) {
				antenna = new Antenna<>(type, worldPosition);
				antenna.deserializeNBT(nbt.getCompound("antennaData"));
			}
		}
	}

	@Override
	public void onLoad() {
		if(antenna != null) { // Handle network set here where level is not null
			AntennaNetwork network = AntennaManager.getNetwork(level);
			network.addAntenna(worldPosition, antenna);
			antenna.setNetwork(network);
		}
	}

}
