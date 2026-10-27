package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.MicrowaveNetworkObjects;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.menus.RadioFaxMenu;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.data.FaxRates;
import com.arrl.radiocraft.common.radio.data.FileTransfer;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.voice.FaxAudio;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The radio fax: sends a document (a book, paper, a map or an enchanted book, see {@link FileTransfer}) to another fax
 * by callsign, through the radios it touches or the radio interfaces on its {@link StationBus}, or straight across a
 * microwave link serving it. It holds blank paper, books and maps (hoppers, or right click with them), the document to
 * send, and a tray of the copies received; an enchanted book is used up when it is sent.
 * <p>
 * A fax is a conversation: the call ({@link TncPacket.Kind#FAX_CALL}, with what is coming), the answer ("OK" once the
 * far end has a blank for it and room in its tray, or why not), then {@link FaxRates#PACKETS} numbered packets each
 * acknowledged before the next; the last carries the document and the far end prints it. A packet whose ack does not
 * come back in time goes again, up to {@code fax_packet_retries} times, so a weak signal makes a slow fax rather than
 * a lost one. How long each packet's tones last is set by the document and the band ({@link FaxRates}). Over a microwave
 * link there are no tones: the packets go straight to the far fax, ten times as fast, and interference loses some.
 * <p>
 * While it works the machine monitors the line through its own small speaker (quietly, through voice chat) and shows
 * the packet count on its LCD; both ends do.
 */
public class RadioFaxBlockEntity extends BlockEntity implements MenuProvider, StationTerminal, INetworkObjectProvider {

	/** Supply slots: blank paper, books and maps. */
	public static final int SLOT_PAPER = 0;
	public static final int SLOT_BOOK = 1;
	public static final int SLOT_MAP = 2;
	/** Slots for the copies received. */
	public static final int RECEIVED_SLOTS = 4;
	/** Lines of the log kept. */
	public static final int LOG_LINES = 6;
	/** Callsigns remembered on the LCD. */
	public static final int RECENT_MAX = 6;
	/** Times the call goes out before NO ANSWER. */
	private static final int CALL_TRIES = 3;
	/** Ticks after the call's tones that the answer may take. */
	private static final int ANSWER_WAIT_TICKS = 80;
	/** Ticks after a packet's tones that its ack may take. */
	private static final int ACK_WAIT_TICKS = 40;
	/** Ticks a receiving fax waits for the next packet, beyond that packet's tones and the sender's retries. */
	private static final int RECEIVE_PATIENCE_TICKS = 200;
	/** Ticks a SENT or FAILED status stays on the LCD. */
	private static final int STATUS_TICKS = 200;
	public static final String STATUS_READY = "READY";
	public static final String ANSWER_OK = "OK";

	public enum Phase { IDLE, CALLING, SENDING, RECEIVING }

	public enum Transport { RADIO, LINK }

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
	/** Copies printed from documents received; nothing can be put in from outside (insertItem refuses everything). */
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
	private String callsign = ""; // The machine's own; without one it sends under its bus's.
	private final List<String> recent = new ArrayList<>(); // Newest first.
	private final List<String> log = new ArrayList<>(); // Newest first.

	// The transfer, as the LCD shows it (saved so a reload shows what was going on; the server then fails it).
	private Phase phase = Phase.IDLE;
	private Transport transport = Transport.RADIO;
	private String peer = "";
	private String label = "";
	private String status = STATUS_READY;
	private int packetsDone = 0;
	private int retries = 0;
	private boolean syncPending = false;

	// Server only, not saved.
	private int packetTicks = 1;
	private int seq = 0;
	private int expected = 0;
	private long deadline = -1L;
	private long statusUntil = -1L;
	private int packetRetries = 0;
	private int callTries = 0;
	private ItemStack sending = ItemStack.EMPTY;
	@Nullable
	private BlockPos linkRadioPos = null;
	@Nullable
	private BlockPos linkPeerPos = null;
	private final SeenPackets seen = new SeenPackets();
	private long monitorFrames = 0L;
	@Nullable
	private LocationalAudioChannel channel;
	@Nullable
	private OpusEncoder encoder;

	public RadioFaxBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.RADIO_FAX.get(), pos, state);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide) {
			getNetworkObject(level, worldPosition); // On the power wire, a microwave radio can serve it there.
			if(phase != Phase.IDLE) { // Whatever was going on when the world closed is lost.
				phase = Phase.IDLE;
				setStatus("FAILED");
				statusUntil = level.getGameTime() + STATUS_TICKS;
			}
		}
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new MicrowaveNetworkObjects.StationLink(level, worldPosition);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof RadioFaxBlockEntity fax && level instanceof ServerLevel serverLevel)
			fax.serverTick(serverLevel, pos, state);
	}

	private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long now = level.getGameTime();
		if(syncPending && now % 2 == 0) {
			syncPending = false;
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
		switch(phase) {
			case IDLE -> {
				if(statusUntil >= 0L && now >= statusUntil) {
					statusUntil = -1L;
					setStatus(STATUS_READY);
				}
			}
			case CALLING -> {
				monitor(level, true);
				if(now >= deadline) {
					if(++callTries < CALL_TRIES)
						sendCall(level);
					else
						fail("NO ANSWER");
				}
			}
			case SENDING -> {
				monitor(level, false);
				if(transport == Transport.LINK)
					tickLink(level, now);
				else if(now >= deadline) {
					if(packetRetries >= RadiocraftServerConfig.FAX_PACKET_RETRIES.get())
						fail("FAILED " + packetsDone + "/" + FaxRates.PACKETS);
					else {
						packetRetries++;
						retries++;
						sendPacket(level);
					}
				}
			}
			case RECEIVING -> {
				monitor(level, false);
				if(now >= deadline)
					fail("LOST " + peer);
			}
		}
	}

	// -------------------- SENDING --------------------

	/** The radio interfaces and other terminals this fax is joined to. */
	public StationBus bus() {
		return level == null ? new StationBus(List.of(), List.of()) : StationBus.find(level, worldPosition);
	}

	/** The radios the fax works through: those touching it, and those on the radio interfaces of its bus. */
	public List<RadioBlockEntity> radios() {
		Set<RadioBlockEntity> out = new LinkedHashSet<>();
		if(level == null)
			return List.of();
		for(Direction direction : Direction.values())
			if(level.getBlockEntity(worldPosition.relative(direction)) instanceof RadioBlockEntity radio)
				out.add(radio);
		for(RadioInterfaceBlockEntity radioInterface : bus().interfaces())
			out.addAll(radioInterface.getRadios());
		return new ArrayList<>(out);
	}

	/** The callsign it sends under: its own, or the first on its bus. */
	public String myCallsign() {
		return callsign.isEmpty() ? bus().callsign() : callsign;
	}

	/** True if a fax to {@code to} is for this machine. A fax is always to one callsign, never to everyone. */
	public boolean isForMe(String to) {
		if(to.isEmpty() || TncPacket.isForEveryone(to))
			return false;
		return to.equals(myCallsign()) || bus().isFor(to);
	}

	/**
	 * Sends the document in the outbox to {@code to}. Returns null if it started, or the translation key saying why
	 * not: no callsign typed, nothing to send, a fax already going, no callsign of its own, or no radio switched on.
	 */
	@Nullable
	public String send(String to) {
		if(!(level instanceof ServerLevel serverLevel))
			return "screen.radiocraft.radio_fax.busy";
		String target = TncPacket.sanitizeCallsign(to);
		if(target.isEmpty() || TncPacket.isForEveryone(target))
			return "screen.radiocraft.radio_fax.no_callsign";
		if(phase != Phase.IDLE)
			return "screen.radiocraft.radio_fax.busy";
		ItemStack stack = outbox.getStackInSlot(0);
		if(stack.isEmpty() || !FileTransfer.canSend(stack))
			return "screen.radiocraft.radio_fax.nothing_to_send";
		if(myCallsign().isEmpty())
			return "screen.radiocraft.station.set_callsign_first";
		if(target.equals(myCallsign()))
			return "screen.radiocraft.radio_fax.to_self";
		remember(target);
		sending = stack.copyWithCount(1);
		peer = target;
		label = FileTransfer.describe(stack);
		packetsDone = 0;
		retries = 0;
		packetRetries = 0;
		callTries = 0;
		if(startLink(serverLevel))
			return null;
		RadioBlockEntity radio = radios().stream().filter(RadioBlockEntity::isPowered).findFirst().orElse(null);
		if(radio == null) {
			sending = ItemStack.EMPTY;
			return "screen.radiocraft.radio_fax.no_radio";
		}
		Band band = radio.getBand();
		transport = Transport.RADIO;
		packetTicks = FaxRates.packetTicks(stack, band);
		phase = Phase.CALLING;
		sendCall(serverLevel);
		return null;
	}

	private void sendCall(ServerLevel level) {
		TncPacket call = TncPacket.fax(level.getRandom().nextLong(), TncPacket.Kind.FAX_CALL, myCallsign(), peer, label + "|" + packetTicks, sending);
		seen.remember(call.id());
		if(!transmit(call)) {
			fail("NO RADIO");
			return;
		}
		deadline = level.getGameTime() + FaxAudio.calling().size() / 2 + ANSWER_WAIT_TICKS;
		setStatus("CALLING " + peer + (callTries > 0 ? " (" + (callTries + 1) + ")" : ""));
	}

	private void sendPacket(ServerLevel level) {
		boolean last = seq == FaxRates.PACKETS;
		TncPacket packet = TncPacket.fax(level.getRandom().nextLong(), TncPacket.Kind.FAX_PACKET, myCallsign(), peer,
				seq + "/" + FaxRates.PACKETS + "|" + packetTicks, last ? sending : ItemStack.EMPTY);
		seen.remember(packet.id());
		if(!transmit(packet)) {
			fail("NO RADIO");
			return;
		}
		deadline = level.getGameTime() + packetTicks + ACK_WAIT_TICKS;
		setStatus("SENDING PACKET " + seq + "/" + FaxRates.PACKETS + (packetRetries > 0 ? " RETRY " + packetRetries : ""));
	}

	/** Packet {@code seq} is acknowledged: on to the next, or done. */
	private void acked(ServerLevel level) {
		packetsDone = seq;
		packetRetries = 0;
		if(seq >= FaxRates.PACKETS)
			finish(level);
		else {
			seq++;
			sendPacket(level);
		}
	}

	private void finish(ServerLevel level) {
		if(FileTransfer.consumed(sending) && ItemStack.isSameItemSameComponents(outbox.getStackInSlot(0), sending))
			outbox.extractItem(0, 1, false); // An enchanted book is sent, not copied.
		logLine("TX " + peer + ": " + label);
		sending = ItemStack.EMPTY;
		phase = Phase.IDLE;
		statusUntil = level.getGameTime() + STATUS_TICKS;
		setStatus("SENT " + label);
		level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.4F, 1.4F);
	}

	/** The fax is over, one way or another; {@code reason} stays on the LCD a while. */
	private void fail(String reason) {
		logLine((phase == Phase.RECEIVING ? "RX " : "TX ") + peer + ": " + reason);
		sending = ItemStack.EMPTY;
		phase = Phase.IDLE;
		statusUntil = level == null ? -1L : level.getGameTime() + STATUS_TICKS;
		setStatus(reason);
	}

	/** Sends {@code packet} through every radio the fax works through. True if one took it. */
	private boolean transmit(TncPacket packet) {
		boolean sent = false;
		for(RadioBlockEntity radio : radios())
			sent |= radio.sendData(packet);
		return sent;
	}

	// -------------------- RECEIVING --------------------

	@Override
	public void receive(TncPacket packet) {
		if(!(level instanceof ServerLevel serverLevel) || !packet.isFax() || !seen.remember(packet.id()) || !isForMe(packet.to()))
			return;
		switch(packet.kind()) {
			case FAX_CALL -> answerCall(serverLevel, packet);
			case FAX_ANSWER -> {
				if(phase == Phase.CALLING && packet.from().equals(peer)) {
					if(packet.text().equals(ANSWER_OK)) {
						phase = Phase.SENDING;
						seq = 1;
						sendPacket(serverLevel);
					}
					else
						fail(peer + ": " + packet.text());
				}
			}
			case FAX_PACKET -> {
				if(phase == Phase.RECEIVING && transport == Transport.RADIO && packet.from().equals(peer))
					takePacket(serverLevel, packet.faxSeq(), packet.item(), true);
			}
			case FAX_ACK -> {
				if(phase == Phase.SENDING && transport == Transport.RADIO && packet.from().equals(peer) && packet.faxSeq() == seq)
					acked(serverLevel);
			}
			default -> {}
		}
	}

	/** Why a document could not be printed here: no blank for it, or no room; null if it could. */
	@Nullable
	private String printProblem(ItemStack document) {
		if(document.isEmpty() || FileTransfer.copy(document) == null)
			return "CANNOT COPY";
		int supplySlot = supplySlotFor(document);
		if(supplies.getStackInSlot(supplySlot).isEmpty())
			return "NO " + (supplySlot == SLOT_BOOK ? "BOOK" : supplySlot == SLOT_MAP ? "MAP" : "PAPER");
		if(receivedSlotFor(FileTransfer.copy(document)) < 0)
			return "TRAY FULL";
		return null;
	}

	private static int supplySlotFor(ItemStack document) {
		ItemStack blank = FileTransfer.blankFor(document);
		return blank.is(Items.BOOK) ? SLOT_BOOK : blank.is(Items.MAP) ? SLOT_MAP : SLOT_PAPER;
	}

	/** A call: answer OK and get ready if a blank and room are there, else say why not. */
	private void answerCall(ServerLevel level, TncPacket call) {
		String problem = phase != Phase.IDLE ? "BUSY" : printProblem(call.item());
		TncPacket answer = TncPacket.fax(level.getRandom().nextLong(), TncPacket.Kind.FAX_ANSWER, myCallsign(), call.from(), problem == null ? ANSWER_OK : problem, ItemStack.EMPTY);
		seen.remember(answer.id());
		if(!transmit(answer))
			return;
		if(problem == null)
			expect(level, Transport.RADIO, call.from(), call.faxLabel(), call.faxTicks());
		else
			logLine("RX " + call.from() + ": " + problem);
	}

	/** Gets ready to take {@code from}'s packets. */
	private void expect(ServerLevel level, Transport how, String from, String what, int ticks) {
		transport = how;
		peer = from;
		label = what;
		packetTicks = Math.max(1, ticks);
		expected = 1;
		packetsDone = 0;
		retries = 0;
		phase = Phase.RECEIVING;
		deadline = level.getGameTime() + patience();
		setStatus("RECEIVING FROM " + peer);
	}

	/** Ticks to wait for the next packet before giving the sender up: as long as the sender keeps trying, and a little. */
	private long patience() {
		return (long)(packetTicks + ACK_WAIT_TICKS) * (RadiocraftServerConfig.FAX_PACKET_RETRIES.get() + 1) + RECEIVE_PATIENCE_TICKS;
	}

	/** Packet {@code n} arrived: the next one expected is taken and (over the radio) acknowledged; the last prints. */
	private void takePacket(ServerLevel level, int n, ItemStack document, boolean ack) {
		if(n < 1 || n > expected)
			return; // Out of order: the sender will send it again.
		deadline = level.getGameTime() + patience();
		if(n == expected) {
			expected++;
			packetsDone = n;
			setStatus("RECEIVING PACKET " + n + "/" + FaxRates.PACKETS);
		}
		else
			retries++; // Heard again: our ack was lost.
		if(ack) {
			TncPacket reply = TncPacket.fax(level.getRandom().nextLong(), TncPacket.Kind.FAX_ACK, myCallsign(), peer, String.valueOf(n), ItemStack.EMPTY);
			seen.remember(reply.id());
			transmit(reply);
		}
		if(n == FaxRates.PACKETS && phase == Phase.RECEIVING)
			print(level, document);
	}

	/** The document arrived whole: a copy on a blank from the supply, into the tray. */
	private void print(ServerLevel level, ItemStack document) {
		String problem = printProblem(document);
		if(problem != null) {
			fail(problem);
			return;
		}
		ItemStack copy = FileTransfer.copy(document);
		int slot = receivedSlotFor(copy);
		supplies.extractItem(supplySlotFor(document), 1, false);
		ItemStack inSlot = received.getStackInSlot(slot);
		if(inSlot.isEmpty())
			received.setStackInSlot(slot, copy);
		else {
			inSlot.grow(1);
			received.setStackInSlot(slot, inSlot);
		}
		logLine("RX " + peer + ": " + label);
		phase = Phase.IDLE;
		statusUntil = level.getGameTime() + STATUS_TICKS;
		setStatus("RECEIVED " + label);
		level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.6F, 0.9F);
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

	// -------------------- MICROWAVE LINK --------------------

	/** The fax answering to {@code to} at the far end of a microwave link serving this one, and the radio it is on. */
	@Nullable
	private RadioFaxBlockEntity linkPeer(String to, BlockPos[] radioOut) {
		if(level == null)
			return null;
		for(MicrowaveRadioBlockEntity radio : MicrowaveRadioBlockEntity.servingStation(level, worldPosition)) {
			if(!radio.isLinked())
				continue;
			for(MicrowaveRadioBlockEntity far : radio.peers())
				for(BlockEntity station : far.stations())
					if(station instanceof RadioFaxBlockEntity fax && fax != this && fax.isForMe(to)) {
						radioOut[0] = radio.getBlockPos();
						return fax;
					}
		}
		return null;
	}

	/** Starts the fax over a microwave link if one reaches {@code peer}; false if none does. */
	private boolean startLink(ServerLevel level) {
		BlockPos[] radioPos = new BlockPos[1];
		RadioFaxBlockEntity far = linkPeer(peer, radioPos);
		if(far == null)
			return false;
		transport = Transport.LINK;
		linkRadioPos = radioPos[0];
		linkPeerPos = far.getBlockPos();
		packetTicks = FaxRates.linkPacketTicks(sending);
		phase = Phase.CALLING;
		setStatus("CALLING " + peer + " (LINK)");
		String answer = far.linkCall(level, myCallsign(), sending, label, packetTicks);
		if(!answer.equals(ANSWER_OK)) {
			fail(peer + ": " + answer);
			return true;
		}
		phase = Phase.SENDING;
		seq = 1;
		deadline = level.getGameTime() + packetTicks;
		setStatus("SENDING PACKET 1/" + FaxRates.PACKETS);
		return true;
	}

	/** A call over the link: the answer, and the machine gets ready if it is OK. */
	String linkCall(ServerLevel level, String from, ItemStack document, String what, int ticks) {
		String problem = phase != Phase.IDLE ? "BUSY" : printProblem(document);
		if(problem != null) {
			logLine("RX " + from + ": " + problem);
			return problem;
		}
		expect(level, Transport.LINK, from, what, ticks);
		return ANSWER_OK;
	}

	/** Packet {@code n} over the link. */
	void linkPacket(ServerLevel level, String from, int n, ItemStack document) {
		if(phase == Phase.RECEIVING && transport == Transport.LINK && from.equals(peer))
			takePacket(level, n, document, false);
	}

	private void tickLink(ServerLevel level, long now) {
		if(now < deadline)
			return;
		MicrowaveRadioBlockEntity radio = linkRadioPos == null ? null : MicrowaveRadioBlockEntity.at(level, linkRadioPos);
		RadioFaxBlockEntity far = linkPeerPos != null && level.getBlockEntity(linkPeerPos) instanceof RadioFaxBlockEntity fax ? fax : null;
		if(radio == null || far == null || !radio.isLinked() || far.phase != Phase.RECEIVING || !far.peer.equals(myCallsign())) {
			fail("LINK LOST");
			return;
		}
		deadline = now + packetTicks;
		if(!radio.getsThrough()) { // Interference: the packet goes again.
			retries++;
			if(++packetRetries > RadiocraftServerConfig.FAX_PACKET_RETRIES.get())
				fail("FAILED " + packetsDone + "/" + FaxRates.PACKETS);
			else
				setStatus("SENDING PACKET " + seq + "/" + FaxRates.PACKETS + " RETRY " + packetRetries);
			return;
		}
		far.linkPacket(level, myCallsign(), seq, seq == FaxRates.PACKETS ? sending : ItemStack.EMPTY);
		packetsDone = seq;
		packetRetries = 0;
		if(seq >= FaxRates.PACKETS)
			finish(level);
		else {
			seq++;
			setStatus("SENDING PACKET " + seq + "/" + FaxRates.PACKETS);
		}
	}

	// -------------------- THE MONITOR SPEAKER --------------------

	/** The machine's own speaker on the line: the tones, quietly, for anyone standing by it. */
	private void monitor(ServerLevel level, boolean calling) {
		if(RadiocraftVoicePlugin.API == null)
			return;
		if(channel == null) {
			channel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromServerLevel(level),
					RadiocraftVoicePlugin.API.createPosition(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D));
			if(channel != null)
				channel.setDistance(10.0F);
			encoder = RadiocraftVoicePlugin.API.createEncoder();
		}
		if(channel == null || encoder == null)
			return;
		int frames = level.getGameTime() % 2 == 0 ? 3 : 2;
		for(int i = 0; i < frames; i++)
			channel.send(encoder.encode(FaxAudio.monitorFrame(monitorFrames++, calling)));
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if(encoder != null) {
			encoder.close();
			encoder = null;
		}
		channel = null;
	}

	// -------------------- CALLSIGNS --------------------

	public String getCallsign() {
		return callsign;
	}

	public void setCallsign(String value) {
		callsign = TncPacket.sanitizeCallsign(value);
		setChanged();
		sync();
	}

	/** Takes the player's callsign; false if they have none. */
	public boolean claim(ServerPlayer player) {
		PlayerCallsignData data = PlayerCallsignSavedData.get(player.serverLevel().getServer().overworld()).getCallsignData(player.getUUID());
		if(data == null || data.callsign() == null || data.callsign().isBlank())
			return false;
		setCallsign(data.callsign());
		return true;
	}

	private void remember(String target) {
		recent.remove(target);
		recent.addFirst(target);
		while(recent.size() > RECENT_MAX)
			recent.removeLast();
		setChanged();
		sync();
	}

	/** Callsigns faxed lately, newest first. */
	public List<String> getRecent() {
		return List.copyOf(recent);
	}

	// -------------------- STATE FOR THE SCREEN --------------------

	private void setStatus(String text) {
		status = text;
		setChanged();
		sync();
	}

	private void logLine(String line) {
		log.addFirst(line);
		while(log.size() > LOG_LINES)
			log.removeLast();
		setChanged();
		sync();
	}

	public Phase getPhase() {
		return phase;
	}

	public Transport getTransport() {
		return transport;
	}

	/** True while a fax is going out or coming in. */
	public boolean isBusy() {
		return phase != Phase.IDLE;
	}

	/** What the LCD says. */
	public String getStatus() {
		return status;
	}

	/** The callsign at the other end of the fax going on (or the last one). */
	public String getPeer() {
		return peer;
	}

	/** What is being sent or received. */
	public String getLabel() {
		return label;
	}

	/** Packets acknowledged (or taken) so far, out of {@link FaxRates#PACKETS}. */
	public int getPacketsDone() {
		return packetsDone;
	}

	/** Packets that had to go again. */
	public int getRetries() {
		return retries;
	}

	public List<String> getLog() {
		return List.copyOf(log);
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

	/** Drops everything it holds: supplies, the document waiting to go and the copies received. */
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
		return Component.translatable("container.radiocraft.radio_fax");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new RadioFaxMenu(id, playerInventory, this);
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
		tag.putString("callsign", callsign);
		tag.put("recent", strings(recent));
		tag.put("log", strings(log));
		tag.putString("phase", phase.name());
		tag.putString("transport", transport.name());
		tag.putString("peer", peer);
		tag.putString("label", label);
		tag.putString("status", status);
		tag.putInt("packetsDone", packetsDone);
		tag.putInt("retries", retries);
	}

	private static ListTag strings(List<String> lines) {
		ListTag tags = new ListTag();
		for(String line : lines)
			tags.add(StringTag.valueOf(line));
		return tags;
	}

	private static void strings(CompoundTag tag, String key, List<String> into) {
		into.clear();
		for(Tag entry : tag.getList(key, Tag.TAG_STRING))
			into.add(entry.getAsString());
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
		callsign = TncPacket.sanitizeCallsign(tag.getString("callsign"));
		strings(tag, "recent", recent);
		strings(tag, "log", log);
		try {
			phase = Phase.valueOf(tag.getString("phase"));
			transport = Transport.valueOf(tag.getString("transport"));
		}
		catch(IllegalArgumentException e) {
			phase = Phase.IDLE;
			transport = Transport.RADIO;
		}
		peer = tag.getString("peer");
		label = tag.getString("label");
		status = tag.contains("status") ? tag.getString("status") : STATUS_READY;
		packetsDone = tag.getInt("packetsDone");
		retries = tag.getInt("retries");
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
