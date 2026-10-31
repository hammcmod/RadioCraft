package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaAnalysis;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.RubberDuckyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.RubberDuckyAntennaType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AntennaAnalyzerItem extends Item {

    public AntennaAnalyzerItem(Properties properties) {
        super(properties);
    }

    private static final float FALLBACK_ANALYZER_FREQUENCY_HZ = 146_520_000.0f;
    /** Antennas reported from one click: more than one on a feedline is already a fault (overdraw). */
    private static final int MAX_REPORTED = 3;

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
        if(context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }

        Player player = context.getPlayer();
        if(player == null)
            return InteractionResult.PASS;

        List<StaticAntenna<?>> antennas = antennasAt(context.getLevel(), context.getClickedPos());
        if(antennas == null) {
            if(hasHandheldRadio(player))
                reportHandheld(player);
            else
                player.displayClientMessage(Component.translatable(Radiocraft.translationKey("message", "antenna_analyzer.nothing")), true);
            return InteractionResult.CONSUME;
        }
        if(antennas.isEmpty()) {
            player.displayClientMessage(Component.translatable(Radiocraft.translationKey("message", "antenna_analyzer.no_antenna")), true);
            return InteractionResult.CONSUME;
        }
        for(StaticAntenna<?> antenna : antennas.subList(0, Math.min(antennas.size(), MAX_REPORTED)))
            report(player, antenna);
        if(antennas.size() > 1)
            player.sendSystemMessage(Component.translatable(Radiocraft.translationKey("message", "antenna_analyzer.overdraw"), antennas.size()).withStyle(ChatFormatting.YELLOW));
        return InteractionResult.CONSUME;
    }

    /**
     * The antennas the analyzer reads at {@code pos}: the antenna itself (any block of it, or its balun), every antenna
     * on a radio's or a coax run's feedline, or null when the block is no part of a station.
     */
    private static @Nullable List<StaticAntenna<?>> antennasAt(Level level, BlockPos pos) {
        BENetworkObject object = IBENetworks.getObject(level, pos);
        if(object instanceof AntennaNetworkObject feed && feed.getAntenna() != null)
            return List.of(feed.getAntenna());
        StaticAntenna<?> direct = AntennaTypes.match(level, pos);
        if(direct != null)
            return List.of(direct);
        if(object instanceof AntennaNetworkObject)
            return List.of(); // A feed point with nothing on it yet.
        if(object instanceof RadioNetworkObject radio)
            return radio.getAntennas().stream().map(AntennaNetworkObject::getAntenna).filter(Objects::nonNull).collect(Collectors.toList());
        if(WireUtils.hasCoax(level, pos))
            return WireUtils.getConnections(level, pos, o -> o instanceof AntennaNetworkObject, WireUtils.coaxWires()).keySet().stream()
                    .map(o -> ((AntennaNetworkObject) o).getAntenna()).filter(Objects::nonNull).collect(Collectors.toList());
        return null;
    }

    private static void report(Player player, StaticAntenna<?> antenna) {
        AntennaAnalysis.Result result = AntennaAnalysis.analyze(f -> antenna.getSWR((float) f), Band.getDefaults().stream()
                .map(band -> Band.getBand(band.name())).filter(Objects::nonNull).sorted(Comparator.comparingDouble(Band::minFrequency)).toList());
        IAntenna.AntennaPos at = antenna.getAntennaPos();
        player.sendSystemMessage(Component.translatable(Radiocraft.translationKey("message", "antenna_analyzer.header"),
                AntennaTypes.displayName(antenna.type), at == null ? "?" : formatPos(at.position())).withStyle(ChatFormatting.GOLD));
        String band = result.best() == null ? "" : result.best().name();
        String swr = formatSwr(result.bestSwr());
        String mhz = String.format(Locale.ROOT, "%.3f", result.resonanceHertz() / 1_000_000.0D);
        String key = switch(result.verdict()) {
            case RESONANT -> "antenna_analyzer.resonant";
            case TOO_SHORT -> "antenna_analyzer.too_short";
            case TOO_LONG -> "antenna_analyzer.too_long";
            case POOR_MATCH -> "antenna_analyzer.poor_match";
            case NONE -> "antenna_analyzer.not_resonant";
        };
        player.sendSystemMessage(Component.translatable(Radiocraft.translationKey("message", key), band, swr, mhz));
        String sweep = result.bands().stream().map(b -> b.band().name() + " " + formatSwr(b.swr())).collect(Collectors.joining("  "));
        player.sendSystemMessage(Component.translatable(Radiocraft.translationKey("message", "antenna_analyzer.sweep"), sweep).withStyle(ChatFormatting.GRAY));
    }

    private static String formatSwr(double swr) {
        return !Double.isFinite(swr) || swr >= AntennaAnalysis.USELESS_SWR ? "10+" : String.format(Locale.ROOT, "%.1f", swr);
    }

    private void reportHandheld(Player player) {
        Float handheldFrequencyHz = resolveHandheldFrequency(player);
        float frequencyHz = (handheldFrequencyHz != null && Float.isFinite(handheldFrequencyHz) && handheldFrequencyHz > 0.0f)
                ? handheldFrequencyHz
                : FALLBACK_ANALYZER_FREQUENCY_HZ;
        for(IAntenna antenna : getPlayerAntennas(player)) {
            if(antenna.getType() instanceof RubberDuckyAntennaType type && antenna.getData() instanceof RubberDuckyAntennaData data) {
                player.sendSystemMessage(Component.translatable(Radiocraft.translationKey("message", "antenna_analyzer.handheld_with_stats"),
                        String.format(Locale.ROOT, "%.3f", data.getLength()), String.format(Locale.ROOT, "%.3f", frequencyHz / 1_000_000.0f),
                        String.format(Locale.ROOT, "%.2f", type.getSWR(data, frequencyHz))));
                return;
            }
        }
        player.sendSystemMessage(Component.translatable(Radiocraft.translationKey("message", "antenna_analyzer.unknown_handheld_antenna")));
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

    /** Chat arguments must be text or numbers: a {@link BlockPos} there throws when the message is built. */
    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
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
