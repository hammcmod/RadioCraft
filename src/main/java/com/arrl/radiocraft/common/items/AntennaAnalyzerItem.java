package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
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

    private static final float FALLBACK_ANALYZER_FREQUENCY_HZ = 146_520_000.0f;

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
                        boolean hasHandheldRadio = hasHandheldRadio(player);
                        if (hasHandheldRadio) {
                            Float handheldFrequencyHz = resolveHandheldFrequency(player);
                            float frequencyHz = (handheldFrequencyHz != null && Float.isFinite(handheldFrequencyHz) && handheldFrequencyHz > 0.0f)
                                    ? handheldFrequencyHz
                                    : FALLBACK_ANALYZER_FREQUENCY_HZ;
                            String formattedLength = String.format("%.3f", data.getLength());
                            String formattedFrequencyMhz = String.format("%.3f", frequencyHz / 1_000_000.0f);
                            String formattedSwr = String.format("%.3f", type.getSWR(data, frequencyHz));
                            player.sendSystemMessage(Component.translatable(
                                    Radiocraft.translationKey("message", "antenna_analyzer.handheld_with_stats"),
                                    formattedLength, formattedFrequencyMhz, formattedSwr));
                        } else {
                            player.sendSystemMessage(Component.translatable(
                                    Radiocraft.translationKey("message", "antenna_analyzer.no_handheld")));
                        }
                    } else {
                        // This should never happen, but hey!
                        boolean hasHandheldRadio = hasHandheldRadio(player);
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

    private boolean hasHandheldRadio(Player player) {
        if (player.getMainHandItem().getItem() instanceof VHFHandheldItem) {
            return true;
        }

        if (player.getOffhandItem().getItem() instanceof VHFHandheldItem) {
            return true;
        }

        return player.getInventory().items.stream().anyMatch(stack -> stack.getItem() instanceof VHFHandheldItem);
    }

    private Float resolveHandheldFrequency(Player player) {
        Float freq = extractHandheldFrequency(player.getMainHandItem());
        if (freq != null) {
            return freq;
        }

        freq = extractHandheldFrequency(player.getOffhandItem());
        if (freq != null) {
            return freq;
        }

        for (ItemStack stack : player.getInventory().items) {
            freq = extractHandheldFrequency(stack);
            if (freq != null) {
                return freq;
            }
        }

        return null;
    }

    private Float extractHandheldFrequency(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof VHFHandheldItem)) {
            return null;
        }

        IVHFHandheldCapability cap = RadiocraftCapabilities.VHF_HANDHELDS.getCapability(stack, null);
        if (cap == null) {
            return null;
        }

        return cap.getFrequencyHertz();
    }
}
