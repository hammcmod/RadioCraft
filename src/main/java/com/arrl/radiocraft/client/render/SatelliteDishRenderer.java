package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.SatelliteDishBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib renderer for the Satellite Dish block entity.
 * <p>
 * Handles rendering of the satellite dish 3D model with placement animation support.
 * The animation is controlled by the block entity and plays once when the block is placed,
 * then holds on the final frame as defined in satellite_dish.animation.json.
 */
public class SatelliteDishRenderer extends GeoBlockRenderer<SatelliteDishBlockEntity> {

    private static final SatelliteDishModel MODEL = new SatelliteDishModel();

    public SatelliteDishRenderer(BlockEntityRendererProvider.Context context) {
        super(MODEL);
    }

    /**
     * Model class for Satellite Dish using GeckoLib's DefaultedBlockGeoModel.
     * Automatically loads:
     * - Geometry: assets/radiocraft/geo/block/satellite_dish.geo.json
     * - Texture: assets/radiocraft/textures/block/satelite_dish.png
     * - Animation: assets/radiocraft/animations/block/satellite_dish.animation.json
     */
    public static class SatelliteDishModel extends DefaultedBlockGeoModel<SatelliteDishBlockEntity> {
        public SatelliteDishModel() {
            super(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "satellite_dish"));
        }
    }
}
