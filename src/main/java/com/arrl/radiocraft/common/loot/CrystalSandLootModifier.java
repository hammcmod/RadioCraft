package com.arrl.radiocraft.common.loot;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.crystal.CrystalBand;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Digging sand now and then turns up a raw quartz crystal, the way gravel gives flint. The chance and the share of
 * poor, good and pure crystals are in the server config; the band is picked at random.
 */
public class CrystalSandLootModifier extends LootModifier {

    public static final MapCodec<CrystalSandLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).apply(inst, CrystalSandLootModifier::new));

    public CrystalSandLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if(state == null || !state.is(Tags.Blocks.SANDS))
            return generatedLoot;
        RandomSource random = context.getRandom();
        if(random.nextDouble() >= RadiocraftServerConfig.CRYSTAL_SAND_CHANCE.get())
            return generatedLoot;
        CrystalBand band = CrystalBand.values()[random.nextInt(CrystalBand.values().length)];
        generatedLoot.add(new ItemStack(RadiocraftItems.CRYSTALS.get(band).get(rollQuality(random)).get()));
        return generatedLoot;
    }

    /** Poor, good or pure, by the configured weights. */
    static CrystalQuality rollQuality(RandomSource random) {
        int poor = RadiocraftServerConfig.CRYSTAL_POOR_WEIGHT.get();
        int good = RadiocraftServerConfig.CRYSTAL_GOOD_WEIGHT.get();
        int pure = RadiocraftServerConfig.CRYSTAL_PURE_WEIGHT.get();
        int total = poor + good + pure;
        if(total <= 0)
            return CrystalQuality.POOR;
        int roll = random.nextInt(total);
        if(roll < poor)
            return CrystalQuality.POOR;
        return roll < poor + good ? CrystalQuality.GOOD : CrystalQuality.PURE;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
