package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
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
 * Satellite Dish block entity with GeckoLib animation support.
 * Plays placement animation once when the block is placed, then holds on the final frame.
 */
public class SatelliteDishBlockEntity extends AntennaBlockEntity implements GeoBlockEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Animation that plays once on placement (defined in satellite_dish.animation.json)
    private static final RawAnimation PLACE_ANIMATION = RawAnimation.begin().thenPlay("placeanimation");
    
    // Flag to track if animation has been played
    private boolean hasPlayedAnimation = false;

    public SatelliteDishBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, AntennaNetworkManager.VHF_ID);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "placement_controller", 0, state -> {
            // Play animation once, then stop
            if (!hasPlayedAnimation) {
                hasPlayedAnimation = true;
                return state.setAndContinue(PLACE_ANIMATION);
            }
            // After animation completes, hold on last frame (handled by animation JSON: "loop": "hold_on_last_frame")
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getType() {
        return RadiocraftBlockEntities.SATELLITE_DISH.get();
    }
}
