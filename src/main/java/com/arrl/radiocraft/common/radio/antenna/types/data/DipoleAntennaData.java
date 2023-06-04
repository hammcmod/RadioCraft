package com.arrl.radiocraft.common.radio.antenna.types.data;

import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.nbt.CompoundTag;

public class DipoleAntennaData extends AntennaData {

		private double armLength1;
		private double armLength2;

		public DipoleAntennaData(double armLength1, double armLength2) {
			this.armLength1 = armLength1;
			this.armLength2 = armLength2;
		}

		public double getArmLength1() {
			return armLength1;
		}

		public double getArmLength2() {
			return armLength2;
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag nbt = new CompoundTag();
			nbt.putDouble("armLength1", armLength1);
			nbt.putDouble("armLength2", armLength2);
			return nbt;
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			armLength1 = nbt.getDouble("armLength1");
			armLength2 = nbt.getDouble("armLength2");
		}

	}