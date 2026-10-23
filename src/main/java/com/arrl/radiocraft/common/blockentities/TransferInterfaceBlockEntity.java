package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.menus.TransferInterfaceMenu;
import com.arrl.radiocraft.common.radio.data.FileTransfer;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
 * The digital transfer interface: sends books, paper and maps through the radio interfaces on its {@link StationBus}
 * (maps as SSTV), and prints copies of the files sent to this station on its own blanks. It holds blank paper, books
 * and maps (hoppers, or right click with them), the file waiting to go, and a tray of copies received.
 */
public class TransferInterfaceBlockEntity extends BlockEntity implements MenuProvider, StationTerminal {

	/** Supply slots: blank paper, books and maps. */
	public static final int SLOT_PAPER = 0;
	public static final int SLOT_BOOK = 1;
	public static final int SLOT_MAP = 2;
	/** Slots for the copies of files received. */
	public static final int RECEIVED_SLOTS = 4;
	/** Lines of the file log kept. */
	public static final int FILE_LOG = 6;

	private final ItemStackHandler supplies = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return isSupply(slot, stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			sync();
		}
	};
	private final ItemStackHandler outbox = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return FileTransfer.canSend(stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};
	/** Copies printed from files received; nothing can be put in from outside (insertItem refuses everything). */
	private final ItemStackHandler received = new ItemStackHandler(RECEIVED_SLOTS) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return false;
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};
	private String transferLabel = "";
	private long transferStartGameTime = -1L;
	private int transferTicks = 0;
	private final List<String> fileLog = new ArrayList<>(); // Newest first.
	private boolean syncPending = false;

	// Server only, not saved.
	private final SeenPackets seen = new SeenPackets();

	public TransferInterfaceBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.TRANSFER_INTERFACE.get(), pos, state);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof TransferInterfaceBlockEntity transfer && level instanceof ServerLevel serverLevel && transfer.syncPending
				&& serverLevel.getGameTime() % 2 == 0) {
			transfer.syncPending = false;
			serverLevel.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
	}

	// -------------------- SENDING --------------------

	/** The radio interfaces and other terminals this one is joined to. */
	public StationBus bus() {
		return level == null ? new StationBus(List.of(), List.of()) : StationBus.find(level, worldPosition);
	}

	/**
	 * Sends the file in the outbox to {@code to} (empty or ALL for everyone); the original stays here. False if there
	 * is no callsign on the bus, nothing sendable, a transfer already going, or no radio on.
	 */
	public boolean sendFile(String to) {
		StationBus bus = bus();
		if(level == null || bus.callsign().isEmpty() || isTransferring())
			return false;
		ItemStack stack = outbox.getStackInSlot(0);
		if(stack.isEmpty() || !FileTransfer.canSend(stack))
			return false;
		String target = TncPacket.sanitizeCallsign(to);
		TncPacket packet = TncPacket.file(level.getRandom().nextLong(), bus.callsign(), TncPacket.isForEveryone(target) ? "" : target, stack);
		seen.remember(packet.id());
		seen.remember(packet.header().id());
		if(!bus.transmit(packet))
			return false;
		String label = "TX " + (packet.to().isEmpty() ? TncPacket.EVERYONE : packet.to()) + ": " + packet.text();
		startTransfer(label, FileTransfer.frameCount(stack));
		logFile(label);
		return true;
	}

	// -------------------- RECEIVING --------------------

	@Override
	public void receive(TncPacket packet) {
		if(packet.kind() != TncPacket.Kind.FILE_HEADER && packet.kind() != TncPacket.Kind.FILE)
			return;
		if(!bus().isFor(packet.to()) || !seen.remember(packet.id()))
			return;
		if(packet.kind() == TncPacket.Kind.FILE_HEADER)
			startTransfer("RX " + packet.from() + ": " + packet.text(), FileTransfer.frameCount(packet.item()));
		else
			printFile(packet);
	}

	/** A file for this station arrived whole: print a copy on a blank from the supply, into the received tray. */
	private void printFile(TncPacket packet) {
		endTransfer();
		String from = "RX " + packet.from() + ": ";
		ItemStack copy = FileTransfer.copy(packet.item());
		if(copy == null) {
			logFile(from + "CANNOT COPY");
			return;
		}
		ItemStack blank = FileTransfer.blankFor(packet.item());
		int supplySlot = blank.is(Items.BOOK) ? SLOT_BOOK : blank.is(Items.MAP) ? SLOT_MAP : SLOT_PAPER;
		if(supplies.getStackInSlot(supplySlot).isEmpty()) {
			logFile(from + "NO BLANK " + (supplySlot == SLOT_BOOK ? "BOOK" : supplySlot == SLOT_MAP ? "MAP" : "PAPER"));
			return;
		}
		int slot = receivedSlotFor(copy);
		if(slot < 0) {
			logFile(from + "TRAY FULL");
			return;
		}
		supplies.extractItem(supplySlot, 1, false);
		ItemStack inSlot = received.getStackInSlot(slot);
		if(inSlot.isEmpty())
			received.setStackInSlot(slot, copy);
		else {
			inSlot.grow(1);
			received.setStackInSlot(slot, inSlot);
		}
		logFile(from + packet.text());
		if(level != null)
			level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.5F, 1.2F);
	}

	/** The received slot a copy goes in: one holding the same thing with room, else an empty one; -1 if none. */
	private int receivedSlotFor(ItemStack copy) {
		for(int slot = 0; slot < received.getSlots(); slot++) {
			ItemStack in = received.getStackInSlot(slot);
			if(!in.isEmpty() && ItemStack.isSameItemSameComponents(in, copy) && in.getCount() < in.getMaxStackSize())
				return slot;
		}
		for(int slot = 0; slot < received.getSlots(); slot++)
			if(received.getStackInSlot(slot).isEmpty())
				return slot;
		return -1;
	}

	// -------------------- PROGRESS --------------------

	private void startTransfer(String label, int frames) {
		transferLabel = label;
		transferStartGameTime = level == null ? 0L : level.getGameTime();
		transferTicks = Math.max(1, (int)Math.ceil(frames * 0.4D)); // 20 ms frames, 50 ms ticks.
		setChanged();
		sync();
	}

	private void endTransfer() {
		transferStartGameTime = -1L;
		setChanged();
		sync();
	}

	private void logFile(String line) {
		fileLog.addFirst(line);
		while(fileLog.size() > FILE_LOG)
			fileLog.removeLast();
		setChanged();
		sync();
	}

	/** True while a file is going out or coming in (by the clock: a lost file just runs out). */
	public boolean isTransferring() {
		return level != null && transferStartGameTime >= 0 && level.getGameTime() < transferStartGameTime + transferTicks + 20;
	}

	/** How far the current transfer is, 0 to 1, or -1 when there is none. */
	public float getTransferProgress() {
		if(!isTransferring())
			return -1.0F;
		return Math.min(1.0F, (level.getGameTime() - transferStartGameTime) / (float)transferTicks);
	}

	public String getTransferLabel() {
		return transferLabel;
	}

	public List<String> getFileLog() {
		return List.copyOf(fileLog);
	}

	public ItemStackHandler getOutbox() {
		return outbox;
	}

	public ItemStackHandler getReceived() {
		return received;
	}

	// -------------------- SUPPLIES --------------------

	/** True if {@code stack} goes in supply slot {@code slot}: blank paper, books and maps. */
	public static boolean isSupply(int slot, ItemStack stack) {
		return switch(slot) {
			case SLOT_PAPER -> stack.is(Items.PAPER);
			case SLOT_BOOK -> stack.is(Items.BOOK);
			case SLOT_MAP -> stack.is(Items.MAP);
			default -> false;
		};
	}

	public ItemStackHandler getSupplies() {
		return supplies;
	}

	/** Puts as much of {@code stack} as fits into its supply slot and returns what is left. */
	public ItemStack insertSupply(ItemStack stack) {
		for(int slot = 0; slot < supplies.getSlots(); slot++)
			if(isSupply(slot, stack))
				return supplies.insertItem(slot, stack, false);
		return stack;
	}

	/** Drops everything it holds: supplies, the file waiting to go and the copies received. */
	public void dropContents() {
		if(level == null)
			return;
		for(ItemStackHandler handler : List.of(supplies, outbox, received))
			for(int slot = 0; slot < handler.getSlots(); slot++)
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), handler.getStackInSlot(slot));
	}

	// -------------------- MENU --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.transfer_interface");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new TransferInterfaceMenu(id, playerInventory, this);
	}

	// -------------------- SAVE & SYNC --------------------

	/** Sends the new state to clients on the next even tick, so a burst of changes goes as one update. */
	private void sync() {
		if(level != null && !level.isClientSide)
			syncPending = true;
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("supplies", supplies.serializeNBT(registries));
		tag.put("outbox", outbox.serializeNBT(registries));
		tag.put("received", received.serializeNBT(registries));
		tag.putString("transferLabel", transferLabel);
		tag.putLong("transferStart", transferStartGameTime);
		tag.putInt("transferTicks", transferTicks);
		ListTag logTags = new ListTag();
		for(String line : fileLog)
			logTags.add(StringTag.valueOf(line));
		tag.put("fileLog", logTags);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if(tag.contains("supplies"))
			supplies.deserializeNBT(registries, tag.getCompound("supplies"));
		if(tag.contains("outbox"))
			outbox.deserializeNBT(registries, tag.getCompound("outbox"));
		if(tag.contains("received"))
			received.deserializeNBT(registries, tag.getCompound("received"));
		transferLabel = tag.getString("transferLabel");
		transferStartGameTime = tag.contains("transferStart") ? tag.getLong("transferStart") : -1L;
		transferTicks = tag.getInt("transferTicks");
		fileLog.clear();
		for(Tag entry : tag.getList("fileLog", Tag.TAG_STRING))
			fileLog.add(entry.getAsString());
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
