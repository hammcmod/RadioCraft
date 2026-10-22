package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * The terrain under the digital interface's APRS map, painted like a vanilla map (each column's top block in its map
 * colour, shaded by the slope towards the north) from the chunks the client has loaded. Anything further out than that
 * is drawn as dark unexplored ground. Kept as one small texture, repainted when the map moves or every few seconds.
 */
public class AprsMapTexture implements AutoCloseable {

	public static final ResourceLocation LOCATION = Radiocraft.id("dynamic/aprs_map");
	private static final int UNEXPLORED = 0xFF1E2224; // ABGR, as NativeImage stores it.
	private static final long REPAINT_MILLIS = 3000L;

	private final int width;
	private final int height;
	private final DynamicTexture texture;
	private BlockPos paintedCentre = null;
	private double paintedBlocksPerPixel = -1.0D;
	private long paintedAtMillis = 0L;

	public AprsMapTexture(int width, int height) {
		this.width = width;
		this.height = height;
		this.texture = new DynamicTexture(width, height, false);
		Minecraft.getInstance().getTextureManager().register(LOCATION, texture);
	}

	/**
	 * Repaints if the map moved, zoomed or has not been painted for a while.
	 *
	 * @param centre         The block at the middle of the map.
	 * @param centreX        Where that block is drawn, in texture pixels from the left (may be fractional).
	 * @param centreY        The same from the top.
	 * @param blocksPerPixel The map's scale.
	 */
	public void update(ClientLevel level, BlockPos centre, double centreX, double centreY, double blocksPerPixel) {
		long now = System.currentTimeMillis();
		if(centre.equals(paintedCentre) && blocksPerPixel == paintedBlocksPerPixel && now - paintedAtMillis < REPAINT_MILLIS)
			return;
		paintedCentre = centre;
		paintedBlocksPerPixel = blocksPerPixel;
		paintedAtMillis = now;
		NativeImage image = texture.getPixels();
		if(image == null)
			return;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for(int px = 0; px < width; px++) {
			int northY = Integer.MIN_VALUE;
			for(int py = -1; py < height; py++) { // Row -1 only gives the first row a northern neighbour to shade against.
				int x = centre.getX() + (int)Math.floor((px + 0.5D - centreX) * blocksPerPixel);
				int z = centre.getZ() + (int)Math.floor((py + 0.5D - centreY) * blocksPerPixel);
				int colour = UNEXPLORED;
				int y = Integer.MIN_VALUE;
				if(level.hasChunk(x >> 4, z >> 4)) {
					y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					MapColor mapColor = state.getMapColor(level, pos);
					if(mapColor != MapColor.NONE) {
						MapColor.Brightness shade = northY == Integer.MIN_VALUE || y == northY ? MapColor.Brightness.NORMAL
								: y > northY ? MapColor.Brightness.HIGH : MapColor.Brightness.LOW;
						colour = mapColor.calculateRGBColor(shade);
					}
				}
				northY = y;
				if(py >= 0)
					image.setPixelRGBA(px, py, dim(colour));
			}
		}
		texture.upload();
	}

	/** A little darker than a map, so the station icons and names stand out on it. */
	private static int dim(int abgr) {
		int b = (int)(((abgr >> 16) & 0xFF) * 0.7F), g = (int)(((abgr >> 8) & 0xFF) * 0.7F), r = (int)((abgr & 0xFF) * 0.7F);
		return 0xFF000000 | b << 16 | g << 8 | r;
	}

	@Override
	public void close() {
		Minecraft.getInstance().getTextureManager().release(LOCATION);
	}
}
