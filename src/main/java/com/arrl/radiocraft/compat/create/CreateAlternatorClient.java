package com.arrl.radiocraft.compat.create;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The alternator's client side: its shaft renderer, and its Flywheel visual. Only loaded when {@link CreateCompat#isActive()}. */
public final class CreateAlternatorClient {

	private CreateAlternatorClient() {
	}

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(CreateAlternator.BLOCK_ENTITY.get(), AlternatorRenderer::new);
	}

	/** Call at client setup. */
	public static void registerVisual() {
		SimpleBlockEntityVisualizer.builder(CreateAlternator.BLOCK_ENTITY.get()).factory(AlternatorVisual::new)
				.skipVanillaRender(be -> false).apply();
	}
}
