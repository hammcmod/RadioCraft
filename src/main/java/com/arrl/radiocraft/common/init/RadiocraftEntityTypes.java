package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RadiocraftEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Radiocraft.MOD_ID);

    public static RegistryObject<EntityType<AntennaWire>> ANTENNA_WIRE = ENTITIES.register("antenna_wire", () -> EntityType.Builder.<AntennaWire>of(AntennaWire::new, MobCategory.MISC)
            .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
            .sized(0.2F, 0.2F).canSpawnFarFromPlayer().fireImmune().build("antenna_wire"));
}
