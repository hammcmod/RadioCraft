package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.RubberDuckyAntennaType;
import com.arrl.radiocraft.common.radio.antenna.data.RubberDuckyAntennaData;
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

    private Set<IAntenna> getPlayerAntennas(Player player) {
        // TODO: This method currently only will get one antenna and that's the antenna known by PlayerRadio.
        //  In the future, this will want to actually check the inventory and list for each handheld radio there.
        return AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).allAntennas().stream().filter((antenna) -> {
            IAntenna.AntennaPos ap = antenna.getAntennaPos();
            Player test = antenna.getPlayer();
            return test != null && test.equals(player);
        }).collect(Collectors.toSet());
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        if(context.getLevel().isClientSide()) { return InteractionResult.PASS; }

        Player player = context.getPlayer();

        Set<IAntenna> antennas = getAntennasAt(context.getLevel(), context.getClickedPos());

        if (!antennas.isEmpty()) {
            IAntenna antenna = antennas.iterator().next();

            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        Radiocraft.translationKey("message", "antenna_analyzer.network_found"),
                        antenna.getAntennaPos(), antennas.size()), true);
                return InteractionResult.CONSUME;
            }
        }

        if(player != null) {
            Set<IAntenna> playerRadios = getPlayerAntennas(player);
            if(!playerRadios.isEmpty()) {
                playerRadios.forEach((it -> {
                    if (it.getType() instanceof RubberDuckyAntennaType) {
                        RubberDuckyAntennaType type = (RubberDuckyAntennaType) it.getType();
                        RubberDuckyAntennaData data = (RubberDuckyAntennaData) it.getData();
                        // This honestly just prevents us from always saying they have a radio in their inventory. It has mostly no other purpose right now.
                        boolean hasHandheldRadio = player.getInventory().items.stream().anyMatch((itemStack) -> itemStack.getItem() instanceof VHFHandheldItem);
                        if (hasHandheldRadio) {
                            String formattedLength = String.format("%.3f", data.getLength());
                            // TODO: I'm not sure the best way to pick the frequency to check in the general case; I selected default simplex for now.
                            String formattedSwr = String.format("%.3f", type.getSWR(data, 146_520_000.0f));
                            player.sendSystemMessage(Component.translatable(
                                    Radiocraft.translationKey("message", "antenna_analyzer.handheld_with_stats"),
                                    formattedLength, formattedSwr));
                        } else {
                            player.sendSystemMessage(Component.translatable(
                                    Radiocraft.translationKey("message", "antenna_analyzer.no_handheld")));
                        }
                    } else {
                        // This should never happen, but hey!
                        boolean hasHandheldRadio = player.getInventory().items.stream().anyMatch((itemStack) -> itemStack.getItem() instanceof VHFHandheldItem);
                        if (hasHandheldRadio) {
                            player.sendSystemMessage(Component.translatable(
                                    Radiocraft.translationKey("message", "antenna_analyzer.unknown_handheld_antenna")));
                        } else {
                            player.sendSystemMessage(Component.translatable(
                                    Radiocraft.translationKey("message", "antenna_analyzer.no_handheld")));
                        }
                    }
                }));
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable(
                        Radiocraft.translationKey("message", "antenna_analyzer.no_networks_at"),
                        context.getClickedPos()), true);
                if (Radiocraft.IS_DEVELOPMENT_ENV) {
                    Set<IAntenna> allAntennasExceptPlayer = AntennaNetworkManager.getAllAntennas().stream().filter((it) -> !(it instanceof PlayerRadio)).collect(Collectors.toSet());
                    player.sendSystemMessage(Component.translatable(
                            Radiocraft.translationKey("message", "antenna_analyzer.development_summary"),
                            allAntennasExceptPlayer.size(),
                            String.join(" ", allAntennasExceptPlayer.stream().map(Object::toString).toList())));
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
        tooltipComponents.add(Component.translatable(Radiocraft.translationKey("tooltip", "antenna_analyzer")));
    }
}
