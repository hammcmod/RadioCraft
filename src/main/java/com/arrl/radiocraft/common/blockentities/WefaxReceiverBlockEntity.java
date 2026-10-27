package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.arrl.radiocraft.api.weather.WeatherSources;
import com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.WefaxReceiverBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.menus.WefaxReceiverMenu;
import com.arrl.radiocraft.common.radio.wefax.WefaxChart;
import com.arrl.radiocraft.common.radio.wefax.WefaxPrints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A WEFAX receiver: through the radio next to it (one with a real antenna, as the teletype needs) it prints weather
 * charts, a sheet of paper each. WX REPT prints the next chart of the broadcast schedule (radar, forecast, surface
 * analysis, round again); SOLR WX the solar weather report; AUTO prints a forecast by itself when dangerous weather is
 * on its way (at most every {@link #AUTO_GAP_TICKS}). A sheet takes {@link #PRINT_TICKS} to print, drawn line by line
 * on its screen, and comes out of the front.
 */
public class WefaxReceiverBlockEntity extends BlockEntity implements MenuProvider {

	public static final int PRINT_TICKS = 240;
	public static final int AUTO_GAP_TICKS = 5 * 60 * 20;
	public static final int PAPER_MAX = 64;
	private static final WefaxChart.Type[] SCHEDULE = { WefaxChart.Type.RADAR, WefaxChart.Type.FORECAST, WefaxChart.Type.SURFACE };

	private final ItemStackHandler paper = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return stack.is(Items.PAPER);
		}

		@Override
		protected void onContentsChanged(int slot) {
			sync();
		}
	};
	private boolean auto;
	private int scheduleIndex;
	private long lastAuto = -AUTO_GAP_TICKS;
	// The sheet being printed: its picture (synced, for the screen), when it began, and the item it becomes.
	private byte[] pixels = new byte[0];
	private long printStart = -1L;
	private ItemStack printing = ItemStack.EMPTY;

	public WefaxReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.WEFAX_RECEIVER.get(), pos, state);
	}

	// -------------------- PAPER --------------------

	/** Puts as much of {@code stack} as fits into the paper supply and returns what is left. */
	public ItemStack insertPaper(ItemStack stack) {
		return paper.insertItem(0, stack, false);
	}

	public int getPaper() {
		return paper.getStackInSlot(0).getCount();
	}

	public void dropContents() {
		if(level == null)
			return;
		Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), paper.getStackInSlot(0));
		if(!printing.isEmpty())
			Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), printing);
	}

	// -------------------- RADIO --------------------

	/**
	 * A radio with a real antenna on its coax, next to it or reached through the station gear it touches (a TNC and
	 * terminals beside it, see {@link StationBus}). Always true with the teletype's needs-radio rule off.
	 */
	public boolean hasRadio() {
		if(!RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get())
			return true;
		if(level == null)
			return false;
		for(Direction direction : Direction.values())
			if(hasAntenna(worldPosition.relative(direction)))
				return true;
		for(RadioInterfaceBlockEntity radioInterface : StationBus.find(level, worldPosition).interfaces())
			for(com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity radio : radioInterface.getRadios())
				if(hasAntenna(radio.getBlockPos()))
					return true;
		return false;
	}

	/** True if a radio at {@code pos} has a real antenna (not a dummy load) on its coax. */
	private boolean hasAntenna(BlockPos pos) {
		return IBENetworks.getObject(level, pos) instanceof RadioNetworkObject radio
				&& radio.getAntennas().stream().anyMatch(antenna -> !(antenna instanceof DummyLoadNetworkObject) && antenna.getAntenna() != null);
	}

	// -------------------- PRINTING --------------------

	public boolean isPrinting() {
		return printStart >= 0;
	}

	/** Whether it receives the solar report or the weather charts: with a radio beside it, or always if the server says so. */
	public boolean receives(boolean solar) {
		return (solar ? RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS : RadiocraftServerConfig.WEFAX_WEATHER_ALWAYS).get() || hasRadio();
	}

	/** Why it cannot print the solar report or a weather chart now (a translation key), or null if it can. */
	@Nullable
	public String cannotPrint(boolean solar) {
		if(isPrinting())
			return "screen.radiocraft.wefax.busy";
		if(!receives(solar))
			return "screen.radiocraft.wefax.no_radio";
		if(getPaper() <= 0)
			return "screen.radiocraft.wefax.no_paper";
		return null;
	}

	/** WX REPT: the next chart in the schedule. */
	public void printWeather() {
		if(!(level instanceof ServerLevel serverLevel) || cannotPrint(false) != null)
			return;
		WefaxChart.Type type = SCHEDULE[scheduleIndex % SCHEDULE.length];
		scheduleIndex = (scheduleIndex + 1) % SCHEDULE.length;
		start(WefaxPrints.printChart(serverLevel, WeatherSources.get(serverLevel), worldPosition, type, ""));
	}

	/** SOLR WX: the solar weather report, as text. */
	public void printSolar() {
		if(!(level instanceof ServerLevel serverLevel) || cannotPrint(true) != null)
			return;
		List<String> lines = new ArrayList<>();
		for(Component line : TeletypeBlockEntity.reportLines(serverLevel, worldPosition))
			lines.add(line.getString());
		start(WefaxPrints.printText(serverLevel, worldPosition, Component.translatable("item.radiocraft.wefax_chart.solar"), "SOLAR WEATHER", lines));
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean value) {
		auto = value;
		setChanged();
		sync();
	}

	private void start(WefaxPrints.Print print) {
		paper.extractItem(0, 1, false);
		pixels = print.pixels();
		printing = print.stack();
		printStart = level.getGameTime();
		setChanged();
		sync();
	}

	/** How far down the sheet the print has come, 0 to 1 (1 when not printing). */
	public float progress(long gameTime) {
		return printStart < 0 ? 1.0F : Math.min(1.0F, (gameTime - printStart) / (float)PRINT_TICKS);
	}

	/** The picture of the sheet being printed (or the last one), {@link WefaxChart#SIZE} square; empty for none. */
	public byte[] getPixels() {
		return pixels;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, WefaxReceiverBlockEntity receiver) {
		if(level instanceof ServerLevel serverLevel)
			receiver.serverTick(serverLevel);
	}

	private boolean radioWas;

	private void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if(now % 40 == 0 && hasRadio() != radioWas) { // The power lamp follows the radio beside it.
			radioWas = !radioWas;
			sync();
		}
		if(isPrinting()) {
			if(now % 9 == 0) // The drum motor, a little lower than the teletype's.
				level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_BACKGROUND.get(), SoundSource.BLOCKS, 0.35F, 0.7F);
			if(now - printStart >= PRINT_TICKS)
				finish(level);
			return;
		}
		if(auto && now % 100 == 0 && now - lastAuto >= AUTO_GAP_TICKS && cannotPrint(false) == null && dangerAhead(level)) {
			lastAuto = now;
			scheduleIndex = 1; // The forecast.
			printWeather();
		}
	}

	/** True if dangerous weather is here, or due within the weather mod's forecast horizon. */
	private boolean dangerAhead(ServerLevel level) {
		WeatherSource source = WeatherSources.get(level);
		if(!source.report(level, worldPosition).hazards().isEmpty())
			return true;
		return source.nextArrival(level, worldPosition, source.forecastHorizon(level))
				.map(arrival -> !arrival.peak().hazards().isEmpty()).orElse(false);
	}

	/** The sheet comes out of the front. */
	private void finish(ServerLevel level) {
		Direction front = getBlockState().hasProperty(WefaxReceiverBlock.FACING) ? getBlockState().getValue(WefaxReceiverBlock.FACING) : Direction.NORTH;
		BlockPos out = worldPosition.relative(front);
		Containers.dropItemStack(level, out.getX() + 0.5D, worldPosition.getY() + 0.4D, out.getZ() + 0.5D, printing);
		printing = ItemStack.EMPTY;
		printStart = -1L;
		level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.4F, 1.4F);
		setChanged();
		sync();
	}

	// -------------------- MENU, SAVE & SYNC --------------------

	private void sync() {
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("block.radiocraft.wefax_receiver");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new WefaxReceiverMenu(id, this);
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("paper", paper.serializeNBT(registries));
		tag.putBoolean("auto", auto);
		tag.putInt("schedule", scheduleIndex);
		tag.putByteArray("pixels", pixels);
		tag.putLong("printStart", printStart);
		if(!printing.isEmpty())
			tag.put("printing", printing.save(registries));
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		paper.deserializeNBT(registries, tag.getCompound("paper"));
		auto = tag.getBoolean("auto");
		scheduleIndex = tag.getInt("schedule");
		pixels = tag.getByteArray("pixels");
		printStart = tag.contains("printStart") ? tag.getLong("printStart") : -1L;
		printing = tag.contains("printing") ? ItemStack.parseOptional(registries, tag.getCompound("printing")) : ItemStack.EMPTY;
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putBoolean("hasRadio", hasRadio());
		return tag;
	}

	private boolean clientHasRadio;

	@Override
	public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.handleUpdateTag(tag, registries);
		clientHasRadio = tag.getBoolean("hasRadio");
	}

	/** On the client: whether the server last said a radio is beside it (the power lamp). */
	public boolean clientHasRadio() {
		return clientHasRadio;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(@NotNull net.minecraft.network.Connection connection, @NotNull ClientboundBlockEntityDataPacket packet, HolderLookup.@NotNull Provider registries) {
		handleUpdateTag(packet.getTag(), registries);
	}
}
