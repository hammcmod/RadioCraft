package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.antenna.types.*;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.JPoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.SlimJimAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.YagiAntennaType;

public class RadiocraftAntennaTypes {

	// HF Antennas
	public static com.arrl.radiocraft.common.radio.antenna.types.MoxonAntennaType MOXON;
	public static com.arrl.radiocraft.common.radio.antenna.types.DeltaLoopAntennaType DELTA_LOOP;
	public static DipoleAntennaType DIPOLE;
	public static EndFedAntennaType END_FED;
	public static HorizontalQuadLoopAntennaType HORIZONTAL_QUAD_LOOP;
	public static VerticalQuadLoopAntennaType VERTICAL_QUAD_LOOP;
	public static QuarterWaveVerticalAntennaType QUARTER_WAVE_VERTICAL;
	public static InvertedVAntennaType INVERTED_V;
	public static RandomWireAntennaType RANDOM_WIRE;
	public static OffCenterFedDipoleAntennaType OFF_CENTER_FED_DIPOLE;
	public static ImprovisedAntennaType IMPROVISED;

	// VHF Antennas
	public static JPoleAntennaType J_POLE;
	public static com.arrl.radiocraft.common.radio.antenna.types.DummyLoadAntennaType DUMMY_LOAD; // Every band.
	public static SlimJimAntennaType SLIM_JIM;
	public static YagiAntennaType YAGI;
	public static com.arrl.radiocraft.common.radio.antenna.types.vhf.SatelliteDishAntennaType SATELLITE_DISH;
	public static com.arrl.radiocraft.common.radio.antenna.types.IonosondeAntennaType IONOSONDE;
	public static com.arrl.radiocraft.common.radio.antenna.types.FiveBtvAntennaType FIVE_BTV; // Every HF band and VHF. // Every band, badly; for the ionospheric processor.

	public static void register() {
		// The Moxon first: its driven element starts like a dipole. The delta loop before the OCFD, which shares its 4:1 balun.
		MOXON = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.MoxonAntennaType());
		DELTA_LOOP = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.DeltaLoopAntennaType());
		DIPOLE = AntennaTypes.registerType(new DipoleAntennaType());
		AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.vhf.LightningRodAntennaType());
		END_FED = AntennaTypes.registerType(new EndFedAntennaType());
		HORIZONTAL_QUAD_LOOP = AntennaTypes.registerType(new HorizontalQuadLoopAntennaType());
		VERTICAL_QUAD_LOOP = AntennaTypes.registerType(new VerticalQuadLoopAntennaType());
		QUARTER_WAVE_VERTICAL = AntennaTypes.registerType(new QuarterWaveVerticalAntennaType());
		INVERTED_V = AntennaTypes.registerType(new InvertedVAntennaType());
		RANDOM_WIRE = AntennaTypes.registerType(new RandomWireAntennaType());
		OFF_CENTER_FED_DIPOLE = AntennaTypes.registerType(new OffCenterFedDipoleAntennaType());
		IMPROVISED = AntennaTypes.registerFallbackType(new ImprovisedAntennaType()); // Last: anything the others do not recognise.

		J_POLE = AntennaTypes.registerType(new JPoleAntennaType());
		DUMMY_LOAD = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.DummyLoadAntennaType());
		SLIM_JIM = AntennaTypes.registerType(new SlimJimAntennaType());
		YAGI = AntennaTypes.registerType(new YagiAntennaType());
		SATELLITE_DISH = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.vhf.SatelliteDishAntennaType());
		IONOSONDE = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.IonosondeAntennaType());
		FIVE_BTV = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.FiveBtvAntennaType());
	}

}
