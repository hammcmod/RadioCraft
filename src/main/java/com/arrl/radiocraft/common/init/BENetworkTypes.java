package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.BENetworkRegistry;
import com.arrl.radiocraft.api.benetworks.power.PowerBENetwork;
import com.arrl.radiocraft.common.benetworks.power.BatteryNetworkObject;
import com.arrl.radiocraft.common.benetworks.power.SolarPanelNetworkObject;

public class BENetworkTypes {

    public static void register() {
        BENetworkRegistry.registerNetwork(BENetwork.DEFAULT_TYPE, BENetwork::new);
        BENetworkRegistry.registerNetwork(PowerBENetwork.TYPE, PowerBENetwork::new);

        BENetworkRegistry.registerObject(BENetworkObject.DEFAULT_TYPE, BENetworkObject::new);
        BENetworkRegistry.registerObject(SolarPanelNetworkObject.TYPE, SolarPanelNetworkObject::new);
        BENetworkRegistry.registerObject(BatteryNetworkObject.TYPE, () -> new BatteryNetworkObject(0, 0, 0));
    }

}
