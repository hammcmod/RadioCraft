package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.BENetworkRegistry;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.common.be_networks.network_objects.*;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;

public class BENetworkTypes {

    public static void register() {
        BENetworkRegistry.registerNetwork(BENetwork.COAXIAL_TYPE, BENetwork::new);
        BENetworkRegistry.registerNetwork(PowerBENetwork.TYPE, PowerBENetwork::new);

        BENetworkRegistry.registerObject(BENetworkObject.DEFAULT_TYPE, BENetworkObject::new);
        BENetworkRegistry.registerObject(SolarPanelNetworkObject.TYPE, SolarPanelNetworkObject::new);
        BENetworkRegistry.registerObject(BatteryNetworkObject.TYPE, (level, pos) -> new BatteryNetworkObject(level, pos,0, 0, 0));
        BENetworkRegistry.registerObject(RadioNetworkObject.TYPE, (level, pos) -> new RadioNetworkObject(level, pos,0, 0));
        BENetworkRegistry.registerObject(AntennaNetworkObject.TYPE, (level, pos) -> new AntennaNetworkObject(level, pos, AntennaNetworkManager.HF_ID));
        BENetworkRegistry.registerObject(ChargeControllerNetworkObject.TYPE, ChargeControllerNetworkObject::new);
        BENetworkRegistry.registerObject(AntennaTunerNetworkObject.TYPE, AntennaTunerNetworkObject::new);
        BENetworkRegistry.registerObject(com.arrl.radiocraft.common.be_networks.network_objects.AutoTunerNetworkObject.TYPE,
                com.arrl.radiocraft.common.be_networks.network_objects.AutoTunerNetworkObject::new);
        BENetworkRegistry.registerObject(DuplexerNetworkObject.TYPE, DuplexerNetworkObject::new);
        BENetworkRegistry.registerObject(com.arrl.radiocraft.common.be_networks.network_objects.BandFollowingAntennaNetworkObject.TYPE,
                com.arrl.radiocraft.common.be_networks.network_objects.BandFollowingAntennaNetworkObject::new);
        BENetworkRegistry.registerObject(com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject.TYPE,
                com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject::new);
        BENetworkRegistry.registerObject(SpeakerNetworkObject.TYPE, SpeakerNetworkObject::new);
        BENetworkRegistry.registerObject(DirectionFinderModuleNetworkObject.TYPE, DirectionFinderModuleNetworkObject::new);
        BENetworkRegistry.registerObject(com.arrl.radiocraft.common.be_networks.network_objects.IonosphericProcessorNetworkObject.TYPE,
                com.arrl.radiocraft.common.be_networks.network_objects.IonosphericProcessorNetworkObject::new);
        BENetworkRegistry.registerObject(MicrowaveNetworkObjects.Dish.TYPE, MicrowaveNetworkObjects.Dish::new);
        BENetworkRegistry.registerObject(MicrowaveNetworkObjects.Radio.TYPE, MicrowaveNetworkObjects.Radio::new);
        BENetworkRegistry.registerObject(MicrowaveNetworkObjects.StationLink.TYPE, MicrowaveNetworkObjects.StationLink::new);
        BENetworkRegistry.registerObject(RepeaterNetworkObject.TYPE, (level, pos) -> new RepeaterNetworkObject(level, pos, 0, 0));
        BENetworkRegistry.registerObject(com.arrl.radiocraft.common.blockentities.CommercialRepeaterBlockEntity.SelfPowered.TYPE, com.arrl.radiocraft.common.blockentities.CommercialRepeaterBlockEntity.SelfPowered::new);
    }

}
