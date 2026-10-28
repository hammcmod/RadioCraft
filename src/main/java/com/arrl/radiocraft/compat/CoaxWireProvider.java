package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaTunerNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.MicrowaveNetworkObjects;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import com.arrl.radiocraft.common.blocks.Strand;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.HashSet;
import java.util.Set;

public enum CoaxWireProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(!data.contains("coaxRadioCount"))
            return;

        int radioCount = data.getInt("coaxRadioCount");
        tooltip.add(Component.literal("Connected Radios: " + radioCount));
        for(int i = 0; i < radioCount; i++)
            tooltip.add(Component.literal(" - " + data.getString("coaxRadio_" + i)));

        int antennaCount = data.getInt("coaxAntennaCount");
        tooltip.add(Component.literal("Connected Antennas: " + antennaCount));
        for(int i = 0; i < antennaCount; i++)
            tooltip.add(Component.literal(" - " + data.getString("coaxAntenna_" + i)));

        int tunerCount = data.getInt("coaxTunerCount");
        if(tunerCount > 0)
            tooltip.add(Component.literal("Antenna Tuner: " + data.getString("coaxTuner_0") + (tunerCount > 1 ? " (+" + (tunerCount - 1) + " more, only one counts)" : "")));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();
        if(!WireUtils.hasCoax(level, pos))
            return;

        Set<BlockPos> visitedObjects = new HashSet<>();
        collectConnections(level, pos, visitedObjects);

        int radioCount = 0;
        int antennaCount = 0;
        int tunerCount = 0;
        for(BlockPos objectPos : visitedObjects) {
            BENetworkObject object = IBENetworks.getObject(level, objectPos);
            if(object instanceof RadioNetworkObject || object instanceof MicrowaveNetworkObjects.Radio) {
                data.putString("coaxRadio_" + radioCount++, describeBlock(level, objectPos));
            }
            else if(object instanceof MicrowaveNetworkObjects.Dish) { // A dish or horn: where it points.
                String aim = level.getBlockEntity(objectPos) instanceof com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity dish
                        ? String.format(java.util.Locale.ROOT, ", az %.0f el %+.0f", dish.getAzimuth(), dish.getElevation()) : "";
                data.putString("coaxAntenna_" + antennaCount++, describeBlock(level, objectPos) + aim);
            }
            else if(object instanceof AntennaNetworkObject antennaObject) {
                data.putString("coaxAntenna_" + antennaCount++, describeAntenna(level, antennaObject));
            }
            else if(object instanceof AntennaTunerNetworkObject) {
                data.putString("coaxTuner_" + tunerCount++, "at " + formatPos(objectPos));
            }
        }

        data.putInt("coaxRadioCount", radioCount);
        data.putInt("coaxAntennaCount", antennaCount);
        data.putInt("coaxTunerCount", tunerCount);
    }

    /**
     * Everything on the coax at {@code pos} (every coax strand there, in a bundle), or on the coax runs touching the
     * device at {@code pos}. Follows the same strands the network walk does, so a bundle's colours stay apart.
     */
    public static void collectConnections(Level level, BlockPos pos, Set<BlockPos> visitedObjects) {
        Set<Strand> visitedWires = new HashSet<>();
        Set<WireBlock> coax = Set.of(WireUtils.coaxWires());
        if(WireUtils.hasCoax(level, pos)) {
            for(Strand strand : Strand.at(level, pos))
                if(coax.contains(strand.kind()) && !strand.kind().conductsAll())
                    collectWireConnections(level, strand, visitedWires, visitedObjects);
            return;
        }

        BENetworkObject origin = IBENetworks.getObject(level, pos);
        if(origin instanceof ICoaxNetworkObject)
            visitedObjects.add(pos);

        for(BlockPos checkPos : WireBlock.wireLinksAround(level, pos, coax).keySet())
            enter(level, checkPos, null, coax, visitedWires, visitedObjects); // Neighbours and climbers coming up to this device.
        for(Direction direction : Direction.values()) {
            BlockPos checkPos = pos.relative(direction);
            if(IBENetworks.getObject(level, checkPos) instanceof ICoaxNetworkObject)
                visitedObjects.add(checkPos);
        }
    }

    /** Into the coax strands at {@code pos} that {@code from} joins (any, for a device). */
    private static void enter(Level level, BlockPos pos, @Nullable WireBlock from, Set<WireBlock> coax, Set<Strand> visitedWires, Set<BlockPos> visitedObjects) {
        for(Strand strand : Strand.at(level, pos))
            if(coax.contains(strand.kind()) && (from == null || from.joins(strand.kind())))
                collectWireConnections(level, strand, visitedWires, visitedObjects);
    }

    private static void collectWireConnections(Level level, Strand strand, Set<Strand> visitedWires, Set<BlockPos> visitedObjects) {
        if(!visitedWires.add(strand))
            return;
        WireBlock wire = strand.kind();
        BlockState state = strand.state(level);
        if(wire == null || state == null)
            return;
        Set<WireBlock> coax = Set.of(WireUtils.coaxWires());

        for(BlockPos checkPos : wire.getLinks(level, strand.pos(), state).keySet()) { // Six neighbours plus climb links, see WireBlock#getLinks.
            if(!Strand.at(level, checkPos).isEmpty()) {
                enter(level, checkPos, wire, coax, visitedWires, visitedObjects);
                continue;
            }

            BENetworkObject object = IBENetworks.getObject(level, checkPos);
            if(object instanceof ICoaxNetworkObject && visitedObjects.add(checkPos) && object instanceof ICoaxPassThrough) {
                // The run carries on through a tuner.
                for(BlockPos beyondPos : WireBlock.wireLinksAround(level, checkPos, coax).keySet())
                    enter(level, beyondPos, null, coax, visitedWires, visitedObjects);
            }
        }
    }

    private static String describeBlock(Level level, BlockPos pos) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return blockId.getPath() + " at " + formatPos(pos);
    }

    private static String describeAntenna(Level level, AntennaNetworkObject antennaObject) {
        StaticAntenna<?> antenna = antennaObject.getAntenna();
        String type = antenna == null ? "unmatched" : antenna.getType().getId().getPath();
        return type + " (" + describeBlock(level, antennaObject.getPos()) + ")";
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("coax_wire");
    }
}
