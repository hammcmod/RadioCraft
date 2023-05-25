package com.arrl.radiocraft.entity;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RadiocraftEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Radiocraft.MOD_ID);

    public static RegistryObject<EntityType<AntennaWireEntity>> ANTENNA_WIRE = ENTITIES.register("antenna_wire", () -> EntityType.Builder.<AntennaWireEntity>of(AntennaWireEntity::new, MobCategory.MISC)
            .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
            .sized(6/16f, 0.5f).canSpawnFarFromPlayer().fireImmune().build("antenna_wire"));
}
