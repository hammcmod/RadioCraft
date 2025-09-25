package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.RubberDuckyAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.data.RubberDuckyAntennaData;
import com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadio;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AntennaAnalyzerItem extends Item {

    public AntennaAnalyzerItem(Properties properties) {
        super(properties);
    }

    private Set<IAntenna> getAntennasAt(Level level, BlockPos pos) {
        Set<IAntenna> allAntennas = new HashSet<IAntenna>();
        for (Map.Entry<ResourceLocation, AntennaNetwork> entry: AntennaNetworkManager.networks.entrySet()) {
            for (IAntenna antenna : entry.getValue().allAntennas()) {
                IAntenna.AntennaPos ap = antenna.getAntennaPos();
                if (ap == null) continue;
                if (ap.level() == level && ap.position().equals(pos)) {
                    allAntennas.add(antenna);
                }
            }
        }
        return allAntennas;
    }

    private IAntenna getPlayerAntenna(Player player) {
        return AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).allAntennas().stream().filter((antenna) -> {
            IAntenna.AntennaPos ap = antenna.getAntennaPos();
            if (ap == null) return false;
            BlockPos playerPos = player.blockPosition();
            if (ap.level() == player.level() && ap.position().equals(playerPos)) {
                return antenna.getType() instanceof RubberDuckyAntennaType;
            }
            return false;
        }).findFirst().orElse(null);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        if(context.getLevel().isClientSide()) { return InteractionResult.PASS; }

        Set<IAntenna> antennas = getAntennasAt(context.getLevel(), context.getClickedPos());

        if (!antennas.isEmpty()) {
            IAntenna antenna = antennas.iterator().next();

            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("Antenna network at " + antenna.getAntennaPos().toString() + " has " + antennas.size() + " antennas"), true);
                return InteractionResult.CONSUME;
            }
        }

        if(context.getPlayer() != null) {
            IAntenna playerRadio = getPlayerAntenna(context.getPlayer());
            if(playerRadio != null) {
                RubberDuckyAntennaType type = (RubberDuckyAntennaType) playerRadio.getType();
                RubberDuckyAntennaData data = (RubberDuckyAntennaData) playerRadio.getData();

                context.getPlayer().displayClientMessage(Component.literal("You are holding a handheld radio, the antenna is " + data.getLength() + "m long and  has an SWR of " + type.getSWR(data, 2)), true);
                return InteractionResult.CONSUME;
            } else {
                context.getPlayer().displayClientMessage(Component.literal("No antenna networks found at " + context.getClickedPos() + "."), true);
                if (Radiocraft.IS_DEVELOPMENT_ENV) {
                    Set<IAntenna> allAntennasExceptPlayer = AntennaNetworkManager.getAllAntennas().stream().filter((it) -> !(it instanceof PlayerRadio)).collect(Collectors.toSet());
                    context.getPlayer().sendSystemMessage(Component.literal("There are " + allAntennasExceptPlayer.size() + " networks in the world: " + String.join(" ", allAntennasExceptPlayer.stream().map(Object::toString).toList())));
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
    }
}
