package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every station the guide book draws is built in the world from its page, exactly as the page's multiblock lays it
 * out, and every radio in it must end up on a power network with a battery: a page cannot show a station that has no
 * power. One test per page. In a Patchouli pattern the first layer is the top, each string is a row along X and each
 * character a step along Z.
 */
@GameTestHolder(Radiocraft.MOD_ID)
public class GuidePagesGameTests {

    private static final String ENTRIES = "/assets/radiocraft/patchouli_books/guide/en_us/entries/";
    private static final List<String> STATION_PAGES = List.of(
            "getting_started/first_station", "radios/hf_radios", "radios/vhf_base_station", "repeaters/repeater",
            "digital/aprs", "digital/teletype", "antennas/dipole", "antennas/vertical", "antennas/quad_loops",
            "antennas/five_btv", "power/overview");

    @GameTestGenerator
    public static List<TestFunction> stationPages() {
        List<TestFunction> tests = new ArrayList<>();
        for(String page : STATION_PAGES)
            tests.add(new TestFunction("guide_pages", "guidepage_" + page.replace('/', '_'), Radiocraft.MOD_ID + ":" + GameTestUtil.EMPTY,
                    200, 0, true, helper -> buildAndCheck(helper, page)));
        return tests;
    }

    private static JsonObject multiblock(String page) {
        try(InputStream in = GuidePagesGameTests.class.getResourceAsStream(ENTRIES + page + ".json")) {
            if(in == null)
                throw new IllegalStateException("No guide page " + page);
            JsonObject entry = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for(JsonElement p : entry.getAsJsonArray("pages"))
                if(p.getAsJsonObject().get("type").getAsString().equals("patchouli:multiblock"))
                    return p.getAsJsonObject().getAsJsonObject("multiblock");
            throw new IllegalStateException("No build on guide page " + page);
        } catch(java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void buildAndCheck(GameTestHelper helper, String page) {
        JsonObject mb = multiblock(page);
        Map<Character, BlockState> mapping = new HashMap<>();
        for(Map.Entry<String, JsonElement> e : mb.getAsJsonObject("mapping").entrySet()) {
            try {
                mapping.put(e.getKey().charAt(0), BlockStateParser.parseForBlock(helper.getLevel().holderLookup(Registries.BLOCK), e.getValue().getAsString(), false).blockState());
            } catch(CommandSyntaxException ex) {
                helper.fail(page + ": bad block " + e.getValue() + ": " + ex.getMessage());
            }
        }
        JsonArray layers = mb.getAsJsonArray("pattern");
        List<BlockPos> radios = new ArrayList<>(), batteries = new ArrayList<>();
        for(int l = layers.size() - 1; l >= 0; l--) { // Bottom layer first, so everything stands on something.
            JsonArray rows = layers.get(l).getAsJsonArray();
            for(int x = 0; x < rows.size(); x++) {
                String row = rows.get(x).getAsString();
                for(int z = 0; z < row.length(); z++) {
                    BlockState state = mapping.get(row.charAt(z));
                    if(state == null)
                        continue;
                    BlockPos pos = new BlockPos(1 + x, 2 + layers.size() - 1 - l, 1 + z);
                    helper.setBlock(pos, state);
                }
            }
        }
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    // Find the radios and batteries by their network objects, now that the blocks have joined up.
                    for(BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 2, 1), new BlockPos(16, 2 + layers.size(), 16))) {
                        BENetworkObject obj = IBENetworks.getObject(helper.getLevel(), helper.absolutePos(pos));
                        if(obj instanceof RadioNetworkObject)
                            radios.add(pos.immutable());
                        else if(obj instanceof BatteryNetworkObject)
                            batteries.add(pos.immutable());
                    }
                    helper.assertFalse(radios.isEmpty(), page + ": no radio in the build");
                    helper.assertFalse(batteries.isEmpty(), page + ": no battery in the build");
                    for(BlockPos radio : radios) {
                        BENetworkObject r = IBENetworks.getObject(helper.getLevel(), helper.absolutePos(radio));
                        boolean powered = false;
                        for(BlockPos battery : batteries)
                            powered |= shareNetwork(r, IBENetworks.getObject(helper.getLevel(), helper.absolutePos(battery)));
                        helper.assertTrue(powered, page + ": the radio at " + radio.toShortString() + " is not wired to a battery");
                    }
                })
                .thenSucceed();
    }

    private static boolean shareNetwork(BENetworkObject a, BENetworkObject b) {
        for(BENetwork network : a.getNetworks().values())
            if(network.getNetworkObjects().contains(b))
                return true;
        return false;
    }
}
