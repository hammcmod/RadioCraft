package com.arrl.radiocraft.common.radio.voice.handheld;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.datacomponents.ManpackState;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.items.HFManpackItem;
import com.arrl.radiocraft.common.items.ManpackMicItem;
import com.arrl.radiocraft.common.network.clientbound.CManpackPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.TransmitterLog;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.ManpackWhipAntennaType;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.voice.Detuning;
import com.arrl.radiocraft.common.radio.voice.RadioVoiceProcessor;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import com.arrl.radiocraft.common.radio.voice.ReceiverMix;
import com.arrl.radiocraft.common.radio.voice.SpeakerKind;
import com.arrl.radiocraft.compat.curios.CuriosCompat;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The HF manpack on a player: a QRP SSB/CW transceiver on 10m or 20m with a whip ({@link ManpackWhipAntennaType}).
 * Like the VHF handheld it is an antenna on the move, its position and terrain worked out again every tick. It works
 * while worn (chest, or the Curios back slot) or held. It talks while the player holds the PTT on its screen, or
 * holds right click with its handset ({@link ManpackMicItem}); only its wearer's voice goes in. What it hears comes
 * out where the player stands, through a small speaker ({@link SpeakerKind#BUILT_IN}).
 */
public class ManpackRadio implements IAntenna {

	private static final Map<UUID, ManpackRadio> RADIOS = new ConcurrentHashMap<>();

	private WeakReference<Player> playerRef;
	private volatile AntennaNetwork network = null;
	private final AtomicReference<AntennaPos> antennaPos = new AtomicReference<>();
	private volatile ManpackState state = null; // Null while the player has no manpack to hand.
	private volatile boolean screenPtt = false;
	private volatile boolean indoors = false;
	private volatile boolean transmitting = false;
	// Stations heard at once are added up (SSB); used under this object's lock.
	private final com.arrl.radiocraft.common.radio.voice.VoiceMix.Aligner aligner = new com.arrl.radiocraft.common.radio.voice.VoiceMix.Aligner();
	private final com.arrl.radiocraft.common.radio.voice.VoiceMix.Renderer renderer = new com.arrl.radiocraft.common.radio.voice.VoiceMix.Renderer();
	private de.maxhenkel.voicechat.api.opus.OpusEncoder encoder; // The manpack's own, not the talker's.
	private LocationalAudioChannel channel = null;

	// The S-meter, and power use, between ticks.
	private volatile double lastSignal = 0.0D;
	private volatile long lastSignalNanos = 0L;
	private float sentSignal = -1.0F;
	private boolean sentTransmitting = false;
	private int spent = 0;

	private ManpackRadio(Player player) {
		playerRef = new WeakReference<>(player);
	}

	public static Optional<ManpackRadio> get(UUID uuid) {
		return Optional.ofNullable(RADIOS.get(uuid));
	}

	// -------------------- THE MANPACK ITSELF --------------------

	/** The manpack the player has to hand: worn on the chest, on the back (Curios), or held. */
	public static ItemStack find(Player player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if(chest.getItem() instanceof HFManpackItem)
			return chest;
		for(ItemStack worn : CuriosCompat.wornManpacks(player))
			return worn;
		if(player.getMainHandItem().getItem() instanceof HFManpackItem)
			return player.getMainHandItem();
		if(player.getOffhandItem().getItem() instanceof HFManpackItem)
			return player.getOffhandItem();
		return ItemStack.EMPTY;
	}

	public static ManpackState stateOf(ItemStack stack) {
		ManpackState state = stack.get(RadiocraftDataComponent.MANPACK_STATE.get());
		return state == null ? ManpackState.fresh(ManpackState.CAPACITY) : state;
	}

	@Nullable
	public Player getPlayer() {
		return playerRef == null ? null : playerRef.get();
	}

	public void setScreenPtt(boolean held) {
		screenPtt = held;
	}

	/** True while it is on the air (voice; CW keys itself). */
	public boolean isTransmitting() {
		return transmitting;
	}

	private boolean pttHeld(Player player) {
		if(screenPtt)
			return true;
		boolean micInHand = player.getMainHandItem().getItem() instanceof ManpackMicItem || player.getOffhandItem().getItem() instanceof ManpackMicItem;
		return micInHand && PlayerRadioManager.get(player.getUUID()).map(PlayerRadio::isUseHeld).orElse(false);
	}

	/** Once a tick: where it is, whether it is on, its battery, and what the player's client should show. */
	private void tick() {
		Player player = getPlayer();
		ItemStack stack = player == null ? ItemStack.EMPTY : find(player);
		if(stack.isEmpty() || player.isSpectator()) {
			state = null;
			antennaPos.set(null);
			transmitting = false;
			return;
		}
		ManpackState current = stateOf(stack);
		if(current.micOut() && !ManpackMicItem.carries(player)) // The handset came back (or was lost): it is on its hook.
			current = current.withMicOut(false);
		if(current.on()) {
			boolean keyed = !current.cw() && pttHeld(player);
			transmitting = keyed;
			if(RadiocraftServerConfig.RADIO_POWER_REQUIRED.get()) {
				spent += keyed ? RadiocraftServerConfig.MANPACK_TRANSMIT_USE.get() : RadiocraftServerConfig.MANPACK_RECEIVE_USE.get();
				if(spent >= current.energy())
					current = current.withEnergy(0).withOn(false); // Flat: it goes dead.
				else if(spent >= 200 || player.level().getGameTime() % 100L == 0L) { // Written back now and then, not every tick.
					current = current.withEnergy(current.energy() - spent);
					spent = 0;
				}
			}
		}
		else
			transmitting = false;
		if(!current.equals(stack.get(RadiocraftDataComponent.MANPACK_STATE.get())))
			stack.set(RadiocraftDataComponent.MANPACK_STATE.get(), current);
		state = current;
		antennaPos.set(new AntennaPos(player.blockPosition(), player.level()));
		indoors = !player.level().canSeeSky(BlockPos.containing(player.getEyePosition()));

		float signal = System.nanoTime() - lastSignalNanos < 300_000_000L ? (float)lastSignal : 0.0F;
		signal = Math.round(signal * 20.0F) / 20.0F;
		if(player instanceof ServerPlayer serverPlayer && (signal != sentSignal || transmitting != sentTransmitting)) {
			sentSignal = signal;
			sentTransmitting = transmitting;
			CManpackPacket.send(serverPlayer, signal, transmitting);
		}
	}

	// -------------------- THE NOISE --------------------

	/** HF noise where it stands: the solar noise floor, plus any aurora overhead. */
	private double noiseFloor(Level level, BlockPos pos) {
		SolarEvent.SolarEventInstance event = SolarEventManager.getEvent(level);
		double noise = event == null ? 0.0D : event.getEvent().getNoise();
		if(Aurora.affects(level, pos))
			noise += RadiocraftServerConfig.AURORA_STATIC.get();
		return Math.min(1.0D, noise);
	}

	// -------------------- TRANSMIT --------------------

	/** The wearer talks: out over the whip, if it is on, in SSB and keyed. Voice chat thread. */
	public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
		ManpackState current = state;
		if(current == null || !current.on() || current.cw() || !transmitting)
			return;
		short[] audio = rawAudio.clone();
		for(int i = 0; i < audio.length; i++)
			audio[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(audio[i] * current.micGain())));
		transmitAudioPacket(level, audio, Band.getBand(current.bandName()), current.frequency(), sourcePlayer);
	}

	@Override
	public void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer) {
		AntennaNetwork network = this.network;
		AntennaPos here = antennaPos.get();
		if(network == null || here == null || band == null || !here.level().equals(level.getServerLevel()))
			return;
		TransmitterLog.record(this, here, band, frequencyHertz);
		for(IAntenna antenna : others(network)) {
			AntennaPos there = antenna.getAntennaPos();
			if(there == null || there.level() != here.level())
				continue;
			AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), band, frequencyHertz, 1.0F, this, sourcePlayer, 0.0F);
			packet.setDestination(antenna);
			packet.setStrength(ManpackWhipAntennaType.INSTANCE.getTransmitEfficiency(packet, new EmptyAntennaData(), there.position(), false));
			if(packet.getStrength() > 0.0D)
				antenna.receiveAudioPacket(packet);
		}
	}

	/** Morse from the manpack's screen, keyed in CW mode. */
	public void transmitMorse(Collection<CWBuffer> buffers) {
		ManpackState current = state;
		AntennaPos here = antennaPos.get();
		if(current == null || !current.on() || !current.cw() || here == null || !(here.level() instanceof net.minecraft.server.level.ServerLevel serverLevel))
			return;
		transmitCWPacket(serverLevel, buffers, Band.getBand(current.bandName()), current.frequency());
	}

	@Override
	public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequencyHertz) {
		AntennaNetwork network = this.network;
		AntennaPos here = antennaPos.get();
		if(network == null || here == null || band == null)
			return;
		TransmitterLog.record(this, here, band, frequencyHertz);
		for(IAntenna antenna : others(network)) {
			AntennaPos there = antenna.getAntennaPos();
			if(there == null || there.level() != here.level())
				continue;
			AntennaCWPacket packet = new AntennaCWPacket(level, buffers, band, frequencyHertz, 1.0F, this);
			packet.setDestination(antenna);
			packet.setStrength(ManpackWhipAntennaType.INSTANCE.getTransmitEfficiency(packet, new EmptyAntennaData(), there.position(), true));
			if(packet.getStrength() > 0.0D)
				antenna.receiveCWPacket(packet);
		}
	}

	private Set<IAntenna> others(AntennaNetwork network) {
		Set<IAntenna> master = network.allAntennas();
		Set<IAntenna> copy;
		synchronized(master) {
			copy = new HashSet<>(master);
		}
		copy.remove(this);
		return copy;
	}

	// -------------------- RECEIVE --------------------

	@Override
	public void receiveAudioPacket(AntennaVoicePacket packet) {
		ManpackState current = state;
		AntennaPos here = antennaPos.get();
		Player player = getPlayer();
		if(current == null || !current.on() || current.cw() || transmitting || here == null || player == null || RadiocraftVoicePlugin.API == null
				|| packet.getLevel() != here.level())
			return;
		Detuning.Effect effect = Detuning.ssb(current.frequency(), packet.getFrequency(), current.frequency() < 10_000_000.0F);
		if(!effect.audible())
			return;
		double strength = ManpackWhipAntennaType.INSTANCE.getReceiveEfficiency(packet, new EmptyAntennaData(), here.position()) * effect.strength();
		if(strength <= 0.0D)
			return;
		lastSignal = strength;
		lastSignalNanos = System.nanoTime();
		double level = ReceiverMix.voiceLevel(strength, noiseFloor(here.level(), here.position())) * current.gain();
		synchronized(this) {
			if(channel == null) {
				channel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromServerLevel(packet.getLevel()),
						RadiocraftVoicePlugin.API.createPosition(player.getX(), player.getEyeY(), player.getZ()));
				if(channel == null)
					return;
				channel.setDistance(12.0F);
			}
			channel.updateLocation(RadiocraftVoicePlugin.API.createPosition(player.getX(), player.getEyeY(), player.getZ()));
			long now = System.nanoTime();
			List<com.arrl.radiocraft.common.radio.voice.VoiceMix.Signal> slot = aligner.offer(new com.arrl.radiocraft.common.radio.voice.VoiceMix.Signal(
					com.arrl.radiocraft.common.radio.voice.VoiceMix.key(packet.getSourcePlayer(), packet.getSource()), packet.getSourcePlayer(),
					packet.getRawAudio(), level, 0.0D, effect.pitchShiftHz(), effect.splatter(), 0.0D, -1.0D), now);
			if(slot.isEmpty())
				return; // Another station leads: this frame goes out mixed with its next one.
			short[] audio = renderer.render(slot, SpeakerKind.BUILT_IN.apply(RadioVoiceProcessor.currentSettings()), 1.0D, now).pcm();
			if(encoder == null)
				encoder = RadiocraftVoicePlugin.API.createEncoder();
			channel.send(encoder.encode(audio));
		}
	}

	@Override
	public void receiveCWPacket(AntennaCWPacket packet) {
		ManpackState current = state;
		AntennaPos here = antennaPos.get();
		if(current == null || !current.on() || !current.cw() || here == null || !(getPlayer() instanceof ServerPlayer player))
			return;
		if(Math.abs(packet.getFrequency() - current.frequency()) > 1000.0F)
			return;
		double strength = ManpackWhipAntennaType.INSTANCE.getReceiveEfficiency(packet, new EmptyAntennaData(), here.position());
		if(strength <= 0.0D)
			return;
		lastSignal = strength;
		lastSignalNanos = System.nanoTime();
		List<CWBuffer> buffers = List.copyOf(packet.getBuffers());
		player.server.execute(() -> com.arrl.radiocraft.common.network.clientbound.CManpackCWPacket.send(player, buffers, (float)strength));
	}

	// -------------------- IANTENNA --------------------

	@Override
	public AntennaPos getAntennaPos() {
		return antennaPos.get();
	}

	@Override
	public IAntennaType<? extends AntennaData> getType() {
		return ManpackWhipAntennaType.INSTANCE;
	}

	@Override
	public AntennaData getData() {
		return new EmptyAntennaData();
	}

	@Override
	public boolean isIndoors() {
		return indoors;
	}

	// -------------------- LIFECYCLE --------------------

	@EventBusSubscriber(modid = Radiocraft.MOD_ID)
	public static final class Events {

		private Events() {}

		@SubscribeEvent
		public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
			ManpackRadio radio = new ManpackRadio(event.getEntity());
			RADIOS.put(event.getEntity().getUUID(), radio);
			radio.joinNetwork();
		}

		@SubscribeEvent
		public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
			ManpackRadio radio = RADIOS.remove(event.getEntity().getUUID());
			if(radio != null && radio.network != null)
				radio.network.removeAntenna(radio);
			if(radio != null)
				synchronized(radio) {
					if(radio.encoder != null) {
						radio.encoder.close();
						radio.encoder = null;
					}
				}
		}

		@SubscribeEvent
		public static void onClone(PlayerEvent.Clone event) {
			ManpackRadio radio = RADIOS.get(event.getOriginal().getUUID());
			if(radio != null) {
				radio.playerRef = new WeakReference<>(event.getEntity());
				radio.joinNetwork();
			}
		}

		@SubscribeEvent
		public static void onServerTick(ServerTickEvent.Post event) {
			for(ManpackRadio radio : RADIOS.values())
				radio.tick();
		}
	}

	private void joinNetwork() {
		if(network != null)
			network.removeAntenna(this);
		network = AntennaNetworkManager.getNetwork(AntennaNetworkManager.HF_ID);
		if(network != null)
			network.addAntenna(this);
	}
}
