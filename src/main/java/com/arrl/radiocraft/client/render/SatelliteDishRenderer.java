package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.SatelliteDishBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib renderer for the microwave dish: the model (geometry, texture and placement animation from
 * {@code satellite_dish}) turned to the dish's exact azimuth rather than one of four directions, with the dish arm
 * ({@code lnb_arm}) tilted to its elevation.
 * <p>
 * The model as drawn faces south and its dish looks {@link #REST_ELEVATION} degrees up (the {@code dish} bone sits at
 * -50 degrees on the arm). Both are taken out here, so what is drawn matches what the link maths uses: a dish at
 * azimuth 0 and elevation 0 faces due north, level.
 */
public class SatelliteDishRenderer extends GeoBlockRenderer<SatelliteDishBlockEntity> {

    private static final SatelliteDishModel MODEL = new SatelliteDishModel();
    /** How far above the horizon the model's dish looks with the arm untilted, degrees. */
    public static final double REST_ELEVATION = 40.0D;

    public SatelliteDishRenderer(BlockEntityRendererProvider.Context context) {
        super(MODEL);
    }

    /**
     * Turned by the azimuth. The model unturned faces south (compass 180), and a positive turn about Y goes the other
     * way round from the compass, so 180 less the bearing.
     */
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        SatelliteDishBlockEntity dish = getAnimatable();
        if(dish == null) {
            super.rotateBlock(facing, poseStack);
            return;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - dish.getAzimuth()));
    }

    public static class SatelliteDishModel extends DefaultedBlockGeoModel<SatelliteDishBlockEntity> {
        public SatelliteDishModel() {
            super(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "satellite_dish"));
        }

        /** Tilts the dish and its feed to the elevation (level at 0), after the placement animation has had its say. */
        @Override
        public void setCustomAnimations(SatelliteDishBlockEntity animatable, long instanceId, AnimationState<SatelliteDishBlockEntity> animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            GeoBone arm = getAnimationProcessor().getBone("lnb_arm");
            if(arm != null)
                arm.setRotZ(arm.getRotZ() + (float)Math.toRadians(animatable.getElevation() - REST_ELEVATION));
        }
    }
}
