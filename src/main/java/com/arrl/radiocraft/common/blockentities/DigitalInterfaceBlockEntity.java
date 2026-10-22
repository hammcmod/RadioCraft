package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.menus.DigitalInterfaceMenu;
import com.arrl.radiocraft.common.radio.data.AprsIcon;
import com.arrl.radiocraft.common.radio.data.FileTransfer;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.voice.Rtty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The digital interface (TNC): placed against one or more radios, it sends and decodes packets through them. So far
 * that is APRS: position beacons (on demand, on a redstone pulse or on a timer), messages to a callsign, and
 * digipeating: with more than one radio next to it, a packet heard on one goes back out on the others a second later,
 * once per packet. And RTTY: what is typed goes out live, character by character, and prints on every interface that
 * hears it (and on this one), a line per sender, using up a sheet of paper every {@link #LINES_PER_PAPER} lines. It
 * holds its own paper, books and maps (hoppers, or right click with them). It needs no power of its own; the radios
 * it uses must be on.
 */
public class DigitalInterfaceBlockEntity extends BlockEntity implements MenuProvider {

	/** Beacons and messages each keep the last this many. */
	public static final int HISTORY = 20;
	private static final int SEEN_MAX = 256;
	/** Supply slots: blank paper, books and maps. */
	public static final int SLOT_PAPER = 0;
	public static final int SLOT_BOOK = 1;
	public static final int SLOT_MAP = 2;
	/** RTTY lines one sheet of paper takes. */
	public static final int LINES_PER_PAPER = 20;
	/** RTTY lines kept on the roll. */
	public static final int RTTY_HISTORY = 60;
	/** Longest RTTY line before the printer starts a new one by itself. */
	public static final int RTTY_LINE_MAX = 200;
	/** Slots for the copies of files received. */
	public static final int RECEIVED_SLOTS = 4;
	/** Lines of the file log kept. */
	public static final int FILE_LOG = 6;
	/** Ticks the teletype printing sound lasts, so it is started again only when it has run out. */
	private static final int PRINT_SOUND_TICKS = 56;

	/** Digital interfaces loaded on the server, for the admin commands that reach all of them. */
	private static final Set<DigitalInterfaceBlockEntity> LOADED = ConcurrentHashMap.newKeySet();

	private String callsign = "";
	private String callsignSetBy = "";
	private String beaconComment = "";
	private AprsIcon icon = AprsIcon.HOUSE;
	private boolean beaconEnabled = false;
	private boolean contactBuzzer = true;
	private boolean messageBuzzer = true;
	private boolean unread = false;
	private long lastBeaconGameTime = -1L;

	private final List<Heard> beacons = new ArrayList<>(); // Newest first, one per station.
	private final List<Logged> messages = new ArrayList<>(); // Newest first: those for this callsign and those it sent.

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
	private int paperCredit = 0; // Lines left on the sheet in the printer.
	private final List<String> rttyLines = new ArrayList<>(); // Oldest first.
	private String rttyOpenFrom = null; // Who the line being printed is from; null when no line is open.
	private final StringBuilder rttyOpen = new StringBuilder();
	private long lastPrintSoundGameTime = -1000L;
	private boolean syncPending = false;

	// Server only, not saved: what has gone through here (so nothing repeats twice) and repeats waiting their turn.
	private final LinkedHashSet<Long> seen = new LinkedHashSet<>();
	private final List<PendingDigipeat> digipeats = new ArrayList<>();
	private boolean wasRedstonePowered = false;

	/** A beacon this station heard and when, in game time. */
	public record Heard(TncPacket packet, long gameTime) {}

	/** A message this station received ({@code outgoing} false) or sent. */
	public record Logged(TncPacket packet, boolean outgoing, long gameTime) {}

	private record PendingDigipeat(TncPacket packet, BlockPos heardOn, long dueGameTime) {}

	public DigitalInterfaceBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.DIGITAL_INTERFACE.get(), pos, state);
	}

	public static Collection<DigitalInterfaceBlockEntity> loaded() {
		return List.copyOf(LOADED);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			LOADED.add(this);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		LOADED.remove(this);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		LOADED.remove(this);
	}

	// -------------------- TICK --------------------

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof DigitalInterfaceBlockEntity tnc) {
			if(level instanceof ServerLevel serverLevel)
				tnc.serverTick(serverLevel);
			else
				tnc.clientTick(level);
		}
	}

	private void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if(syncPending && now % 2 == 0) { // At most ten updates a second, however fast RTTY prints.
			syncPending = false;
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
		Iterator<PendingDigipeat> it = digipeats.iterator();
		while(it.hasNext()) {
			PendingDigipeat pending = it.next();
			if(now < pending.dueGameTime())
				continue;
			it.remove();
			for(RadioBlockEntity radio : getRadios())
				if(!radio.getBlockPos().equals(pending.heardOn()))
					radio.sendData(pending.packet());
		}

		boolean redstone = level.hasNeighborSignal(worldPosition);
		if(redstone && !wasRedstonePowered)
			beacon(); // A rising edge sends a beacon.
		wasRedstonePowered = redstone;

		long interval = RadiocraftServerConfig.TNC_BEACON_INTERVAL_SECONDS.get() * 20L;
		if(beaconEnabled && (lastBeaconGameTime < 0 || now - lastBeaconGameTime >= interval || now < lastBeaconGameTime))
			beacon();
	}

	/** The new-message light: a red flicker on top until someone reads the messages. */
	private void clientTick(Level level) {
		if(unread && level.getGameTime() % 10 == 0)
			level.addParticle(DustParticleOptions.REDSTONE, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.45D, worldPosition.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
	}

	// -------------------- SENDING --------------------

	/** Radios on any side of this interface. */
	public List<RadioBlockEntity> getRadios() {
		if(level == null)
			return List.of();
		List<RadioBlockEntity> radios = new ArrayList<>();
		for(Direction direction : Direction.values()) {
			BlockPos at = worldPosition.relative(direction);
			if(level.isLoaded(at) && level.getBlockEntity(at) instanceof RadioBlockEntity radio)
				radios.add(radio);
		}
		return radios;
	}

	/** Sends a packet out through every radio next to it that is on. Returns true if at least one takes it. */
	private boolean transmit(TncPacket packet) {
		remember(packet.id()); // Our own packet coming back from a digipeater is not news.
		boolean sent = false;
		for(RadioBlockEntity radio : getRadios())
			sent |= radio.sendData(packet);
		return sent;
	}

	private long newId() {
		return level == null ? System.nanoTime() : level.getRandom().nextLong();
	}

	/** Beacons this station's position and comment. False if it has no callsign or no radio on to send it. */
	public boolean beacon() {
		if(level == null || callsign.isEmpty())
			return false;
		lastBeaconGameTime = level.getGameTime();
		setChanged();
		return transmit(TncPacket.beacon(newId(), callsign, worldPosition, beaconComment, icon.name()));
	}

	/** Sends {@code text} to {@code to} and logs it. False if it has no callsign, no recipient or no radio on. */
	public boolean sendMessage(String to, String text) {
		if(level == null || callsign.isEmpty())
			return false;
		TncPacket packet = TncPacket.message(newId(), callsign, to, text);
		if(packet.to().isEmpty() || packet.text().isEmpty() || !transmit(packet))
			return false;
		log(new Logged(packet, true, level.getGameTime()));
		sync();
		return true;
	}

	/**
	 * Sends RTTY characters as they are typed, and prints them here too (the local copy). False if there is no
	 * callsign, nothing printable, or no radio on to send it.
	 */
	public boolean typeRtty(String text) {
		if(level == null || callsign.isEmpty())
			return false;
		String clean = Rtty.sanitize(text, TncPacket.RTTY_TEXT_MAX);
		if(clean.isEmpty() || !transmit(TncPacket.rtty(newId(), callsign, clean)))
			return false;
		print(callsign, clean);
		return true;
	}

	// -------------------- FILES --------------------

	/**
	 * Sends the file in the outbox to {@code to} (empty or ALL for everyone); the original stays here. False if there
	 * is no callsign, nothing sendable, a transfer already going, or no radio on.
	 */
	public boolean sendFile(String to) {
		if(level == null || callsign.isEmpty() || isTransferring())
			return false;
		ItemStack stack = outbox.getStackInSlot(0);
		if(stack.isEmpty() || !FileTransfer.canSend(stack))
			return false;
		String target = TncPacket.sanitizeCallsign(to);
		TncPacket packet = TncPacket.file(newId(), callsign, TncPacket.isForEveryone(target) ? "" : target, stack);
		remember(packet.header().id());
		if(!transmit(packet))
			return false;
		startTransfer("TX " + (packet.to().isEmpty() ? TncPacket.EVERYONE : packet.to()) + ": " + packet.text(), FileTransfer.frameCount(stack));
		logFile("TX " + (packet.to().isEmpty() ? TncPacket.EVERYONE : packet.to()) + ": " + packet.text());
		return true;
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

	// -------------------- RTTY PRINTER --------------------

	/**
	 * Prints RTTY from {@code from}: a line per sender, a newline ends it. Starting a line takes paper; with none, the
	 * characters are lost until there is some.
	 */
	private void print(String from, String text) {
		boolean printed = false;
		for(char c : text.toCharArray()) {
			if(c == '\n') {
				if(rttyOpenFrom != null)
					endLine(true);
				continue;
			}
			if(rttyOpenFrom == null || !rttyOpenFrom.equals(from) || rttyOpen.length() >= RTTY_LINE_MAX) {
				if(rttyOpenFrom != null)
					endLine(false);
				if(!takePaperLine())
					continue;
				rttyOpenFrom = from;
				rttyOpen.setLength(0);
			}
			rttyOpen.append(c);
			printed = true;
		}
		if(printed && level != null && level.getGameTime() - lastPrintSoundGameTime >= PRINT_SOUND_TICKS) {
			lastPrintSoundGameTime = level.getGameTime();
			level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
		}
		setChanged();
		sync();
	}

	private void endLine(boolean bell) {
		rttyLines.add(rttyOpenFrom + ": " + rttyOpen);
		while(rttyLines.size() > RTTY_HISTORY)
			rttyLines.removeFirst();
		rttyOpenFrom = null;
		rttyOpen.setLength(0);
		if(bell && level != null)
			level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
	}

	/** Takes one line's worth of paper: off the sheet in the printer, or a new sheet from the supply. */
	private boolean takePaperLine() {
		if(paperCredit <= 0) {
			if(supplies.extractItem(SLOT_PAPER, 1, false).isEmpty())
				return false;
			paperCredit = LINES_PER_PAPER;
		}
		paperCredit--;
		return true;
	}

	/** RTTY lines the paper on hand will print: what is left of the sheet in the printer plus the supply. */
	public int getPaperLines() {
		return paperCredit + supplies.getStackInSlot(SLOT_PAPER).getCount() * LINES_PER_PAPER;
	}

	/** Printed RTTY lines, oldest first, not counting the one being printed. */
	public List<String> getRttyLines() {
		return List.copyOf(rttyLines);
	}

	/** The line being printed ("CALL: text"), or null when none is open. */
	@Nullable
	public String getRttyOpenLine() {
		return rttyOpenFrom == null ? null : rttyOpenFrom + ": " + rttyOpen;
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
	public void dropSupplies() {
		if(level == null)
			return;
		for(ItemStackHandler handler : List.of(supplies, outbox, received))
			for(int slot = 0; slot < handler.getSlots(); slot++)
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), handler.getStackInSlot(slot));
	}

	// -------------------- RECEIVING --------------------

	/**
	 * A packet one of the radios next to it decoded. RTTY prints; anything else is kept if it is news, and repeated on
	 * the other radios.
	 */
	public void receive(RadioBlockEntity radio, TncPacket packet) {
		if(packet.kind() == TncPacket.Kind.RTTY) {
			if(remember(packet.id()))
				print(packet.from(), packet.text());
			return;
		}
		if(packet.kind() == TncPacket.Kind.FILE_HEADER) {
			if(isFor(packet.to()) && remember(packet.id()))
				startTransfer("RX " + packet.from() + ": " + packet.text(), FileTransfer.frameCount(packet.item()));
			return;
		}
		if(packet.kind() == TncPacket.Kind.FILE) {
			if(isFor(packet.to()) && remember(packet.id()))
				printFile(packet);
			return;
		}
		if(!take(packet))
			return;
		if(getRadios().size() > 1 && level != null)
			digipeats.add(new PendingDigipeat(packet.digipeated(), radio.getBlockPos(), level.getGameTime() + RadiocraftServerConfig.TNC_DIGIPEAT_DELAY_TICKS.get()));
	}

	/** A packet put straight into this interface (admin commands): kept like one off the air, but not repeated. */
	public void receiveDirect(TncPacket packet) {
		take(packet);
	}

	/** True if any radio next to this interface is on and tuned within 1 kHz of {@code hz}. */
	public boolean isListeningOn(float hz) {
		for(RadioBlockEntity radio : getRadios())
			if(radio.isPowered() && Math.abs(radio.getFrequency() - hz) < 1000.0F)
				return true;
		return false;
	}

	private boolean take(TncPacket packet) {
		if(level == null || !remember(packet.id()))
			return false;
		long now = level.getGameTime();
		if(packet.kind() == TncPacket.Kind.BEACON) {
			boolean known = beacons.removeIf(heard -> heard.packet().from().equals(packet.from()));
			beacons.addFirst(new Heard(packet, now));
			trim(beacons);
			if(!known && contactBuzzer)
				buzz(1.4F);
		}
		else if(packet.kind() == TncPacket.Kind.MESSAGE && isFor(packet.to())) {
			log(new Logged(packet, false, now));
			setUnread(true);
			if(messageBuzzer)
				buzz(0.8F);
		}
		setChanged();
		sync();
		return true;
	}

	/** True if a message or file to {@code to} is for this station: its callsign, or everyone. */
	private boolean isFor(String to) {
		return TncPacket.isForEveryone(to) || (!callsign.isEmpty() && to.equals(callsign));
	}

	/** Notes a packet id; false if it was already noted. */
	private boolean remember(long id) {
		if(!seen.add(id))
			return false;
		if(seen.size() > SEEN_MAX) {
			Iterator<Long> oldest = seen.iterator();
			oldest.next();
			oldest.remove();
		}
		return true;
	}

	private void log(Logged entry) {
		messages.addFirst(entry);
		trim(messages);
		setChanged();
	}

	private static void trim(List<?> list) {
		while(list.size() > HISTORY)
			list.removeLast();
	}

	private void buzz(float pitch) {
		if(level != null)
			level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6F, pitch);
	}

	// -------------------- SETTINGS --------------------

	/** Sets this station to {@code player}'s callsign. False (and nothing changes) if they have none. */
	public boolean claim(ServerPlayer player) {
		PlayerCallsignData data = PlayerCallsignSavedData.get(player.serverLevel().getServer().overworld()).getCallsignData(player.getUUID());
		if(data == null || data.callsign() == null || data.callsign().isBlank())
			return false;
		setCallsign(data.callsign(), player.getGameProfile().getName());
		return true;
	}

	/** Gives the station its own callsign, remembering who set it. */
	public void setCallsign(String value, String setBy) {
		String clean = TncPacket.sanitizeCallsign(value);
		if(!clean.equals(callsign))
			setUnread(false); // Messages for the old callsign are not news to the new one.
		callsign = clean;
		callsignSetBy = setBy == null ? "" : setBy;
		setChanged();
		sync();
	}

	/** Applies the screen's settings; only the keys present change, and every value is checked. */
	public void applySettings(CompoundTag tag) {
		if(tag.contains("comment"))
			beaconComment = TncPacket.sanitizeText(tag.getString("comment"), TncPacket.BEACON_TEXT_MAX);
		if(tag.contains("icon"))
			icon = AprsIcon.byName(tag.getString("icon"));
		if(tag.contains("beaconEnabled")) {
			beaconEnabled = tag.getBoolean("beaconEnabled");
			lastBeaconGameTime = -1L; // Switching it on beacons right away.
		}
		if(tag.contains("contactBuzzer"))
			contactBuzzer = tag.getBoolean("contactBuzzer");
		if(tag.contains("messageBuzzer"))
			messageBuzzer = tag.getBoolean("messageBuzzer");
		setChanged();
		sync();
	}

	public void markRead() {
		setUnread(false);
	}

	private void setUnread(boolean value) {
		if(unread == value)
			return;
		unread = value;
		setChanged();
		sync();
		if(level != null)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** Comparator output: 15 while a message waits to be read. */
	public int getComparatorOutput() {
		return unread ? 15 : 0;
	}

	public String getCallsign() {
		return callsign;
	}

	public String getCallsignSetBy() {
		return callsignSetBy;
	}

	public String getBeaconComment() {
		return beaconComment;
	}

	public AprsIcon getIcon() {
		return icon;
	}

	public boolean isBeaconEnabled() {
		return beaconEnabled;
	}

	public boolean isContactBuzzer() {
		return contactBuzzer;
	}

	public boolean isMessageBuzzer() {
		return messageBuzzer;
	}

	public boolean hasUnread() {
		return unread;
	}

	public List<Heard> getBeacons() {
		return List.copyOf(beacons);
	}

	public List<Logged> getMessages() {
		return List.copyOf(messages);
	}

	// -------------------- MENU --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.digital_interface");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new DigitalInterfaceMenu(id, playerInventory, this);
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
		tag.putString("callsign", callsign);
		tag.putString("callsignSetBy", callsignSetBy);
		tag.putString("comment", beaconComment);
		tag.putString("icon", icon.name());
		tag.putBoolean("beaconEnabled", beaconEnabled);
		tag.putBoolean("contactBuzzer", contactBuzzer);
		tag.putBoolean("messageBuzzer", messageBuzzer);
		tag.putBoolean("unread", unread);
		tag.putLong("lastBeacon", lastBeaconGameTime);
		ListTag beaconTags = new ListTag();
		for(Heard heard : beacons) {
			CompoundTag entry = heard.packet().save();
			entry.putLong("heardAt", heard.gameTime());
			beaconTags.add(entry);
		}
		tag.put("beacons", beaconTags);
		ListTag messageTags = new ListTag();
		for(Logged logged : messages) {
			CompoundTag entry = logged.packet().save();
			entry.putLong("loggedAt", logged.gameTime());
			entry.putBoolean("outgoing", logged.outgoing());
			messageTags.add(entry);
		}
		tag.put("messages", messageTags);
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
		tag.putInt("paperCredit", paperCredit);
		ListTag rttyTags = new ListTag();
		for(String line : rttyLines)
			rttyTags.add(StringTag.valueOf(line));
		tag.put("rtty", rttyTags);
		if(rttyOpenFrom != null) {
			tag.putString("rttyOpenFrom", rttyOpenFrom);
			tag.putString("rttyOpen", rttyOpen.toString());
		}
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		callsign = TncPacket.sanitizeCallsign(tag.getString("callsign"));
		callsignSetBy = tag.getString("callsignSetBy");
		beaconComment = TncPacket.sanitizeText(tag.getString("comment"), TncPacket.BEACON_TEXT_MAX);
		icon = AprsIcon.byName(tag.getString("icon"));
		beaconEnabled = tag.getBoolean("beaconEnabled");
		contactBuzzer = !tag.contains("contactBuzzer") || tag.getBoolean("contactBuzzer");
		messageBuzzer = !tag.contains("messageBuzzer") || tag.getBoolean("messageBuzzer");
		unread = tag.getBoolean("unread");
		lastBeaconGameTime = tag.contains("lastBeacon") ? tag.getLong("lastBeacon") : -1L;
		beacons.clear();
		for(Tag entry : tag.getList("beacons", Tag.TAG_COMPOUND)) {
			CompoundTag compound = (CompoundTag)entry;
			beacons.add(new Heard(TncPacket.load(compound), compound.getLong("heardAt")));
		}
		messages.clear();
		for(Tag entry : tag.getList("messages", Tag.TAG_COMPOUND)) {
			CompoundTag compound = (CompoundTag)entry;
			messages.add(new Logged(TncPacket.load(compound), compound.getBoolean("outgoing"), compound.getLong("loggedAt")));
		}
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
		paperCredit = tag.getInt("paperCredit");
		rttyLines.clear();
		for(Tag entry : tag.getList("rtty", Tag.TAG_STRING))
			rttyLines.add(entry.getAsString());
		rttyOpenFrom = tag.contains("rttyOpenFrom") ? tag.getString("rttyOpenFrom") : null;
		rttyOpen.setLength(0);
		rttyOpen.append(tag.getString("rttyOpen"));
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
