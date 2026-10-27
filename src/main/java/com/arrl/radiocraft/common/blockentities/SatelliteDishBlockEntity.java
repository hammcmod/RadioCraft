package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The microwave dish's block entity, with GeckoLib animation: it plays its placement animation once, then holds on
 * the final frame. Where it points is kept by {@link MicrowaveDishBlockEntity}.
 */
public class SatelliteDishBlockEntity extends MicrowaveDishBlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Animation that plays once on placement (defined in satellite_dish.animation.json)
    private static final RawAnimation PLACE_ANIMATION = RawAnimation.begin().thenPlay("placeanimation");

    private boolean hasPlayedAnimation = false;

    public SatelliteDishBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.SATELLITE_DISH.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "placement_controller", 0, state -> {
            if (!hasPlayedAnimation) {
                hasPlayedAnimation = true;
                return state.setAndContinue(PLACE_ANIMATION);
            }
            return PlayState.CONTINUE; // Holds on the last frame ("loop": "hold_on_last_frame").
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
