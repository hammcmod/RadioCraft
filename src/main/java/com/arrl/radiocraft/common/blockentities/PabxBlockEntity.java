package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.DataWire;
import com.arrl.radiocraft.common.be_networks.network_objects.PabxNetworkObject;
import com.arrl.radiocraft.common.blocks.PabxBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.PabxMenu;
import com.arrl.radiocraft.common.radio.microwave.MicrowaveRoutes;
import com.arrl.radiocraft.common.telephone.PabxNumbers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A phone PABX: every telephone needs one (maintainer, 2026-10-30). The phones it serves (touching it, or on its data
 * wire; a phone joins the nearest PABX) call each other by extension, numbered from {@link #getFirstExtension()}.
 * <p>
 * PABXes linked to each other, by data wire (or touching) or across microwave links, however many steps away, are one
 * phone network of equals (2026-10-31). A number dialed is an extension on the caller's own PABX if it has one;
 * otherwise, if it starts with the {@linkplain #getMaster() master number} of a powered PABX on the network, the rest
 * is an extension there: from 602's phones, "603100" rings extension 100 on PABX 603. Master numbers are all
 * {@link #masterDigits()} long, or empty (only the PABX's own phones can call it), and no two on a network match. Its
 * {@linkplain #getName() name} shows on a phone keying its master number. It needs power ({@link PabxNetworkObject});
 * with none, its phones have no line.
 */
public class PabxBlockEntity extends BlockEntity implements INetworkObjectProvider, MenuProvider {

	public static final int FIRST_EXTENSION = 100;
	/** Highest extension. */
	public static final int MAX_EXTENSION = 99999;
	/** Most PABXes one network search visits, so a huge web cannot stall a tick. */
	private static final int MAX_NETWORK = 256;

	private String master = "";
	private int firstExtension = FIRST_EXTENSION;
	private String name = "";
	/** The phones it has numbered, so a number set by hand later is left alone. */
	private final Set<BlockPos> numbered = new HashSet<>();
	private boolean powered = false;
	/** What the last save on its screen came to: a translation key and its argument (synced, not saved). */
	private String statusKey = "", statusArg = "";
	/** The client's copy of {@link #masterDigits()}, for the screen. */
	private int clientMasterDigits = 3;

	public PabxBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.PABX.get(), pos, state);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide())
			getNetworkObject(level, worldPosition); // Joins the power wire.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new PabxNetworkObject(level, worldPosition);
	}

	// -------------------- SETTINGS --------------------

	/** Digits in a master number (server config {@code pabx_master_digits}). */
	public static int masterDigits() {
		try {
			return RadiocraftServerConfig.PABX_MASTER_DIGITS.get();
		}
		catch(IllegalStateException e) { // Config not loaded yet.
			return 3;
		}
	}

	/** True while the wire feeds it (the client's copy is synced). */
	public boolean isPowered() {
		return powered;
	}

	/** The number other PABXes dial it by; empty for none (it can then only be called from its own phones). */
	public String getMaster() {
		return master;
	}

	public int getFirstExtension() {
		return firstExtension;
	}

	/** Its name, shown on a phone keying its master number; empty for none. */
	public String getName() {
		return name;
	}

	/** How many digits a master number must have: the client's synced copy, or the server config. */
	public int getMasterDigits() {
		return level != null && level.isClientSide ? clientMasterDigits : masterDigits();
	}

	/** The last save's result, for the screen: a translation key (empty for none) and its argument. */
	public String getStatusKey() {
		return statusKey;
	}

	public String getStatusArg() {
		return statusArg;
	}

	/**
	 * Sets the master number: empty, or exactly {@link #masterDigits()} digits that no other PABX on its network has.
	 * Returns null if it was set, else the translation key saying why not.
	 */
	@Nullable
	public String setMaster(String number) {
		String why = masterProblem(number);
		if(why != null)
			return why;
		master = number;
		sync();
		return null;
	}

	/** Why {@code number} cannot be this PABX's master number, as a translation key, or null if it can. */
	@Nullable
	private String masterProblem(String number) {
		if(!PabxNumbers.validMaster(number, masterDigits()))
			return "screen.radiocraft.pabx.master_length";
		if(!number.isEmpty())
			for(PabxBlockEntity other : network().keySet())
				if(other != this && other.master.equals(number))
					return "screen.radiocraft.pabx.master_taken";
		return null;
	}

	/** Sets the first extension, numbering every phone again from it if it changed. */
	public boolean setFirstExtension(int first) {
		if(first < 0 || first > MAX_EXTENSION)
			return false;
		if(first != firstExtension) {
			firstExtension = first;
			numbered.clear();
		}
		sync();
		return true;
	}

	/** Sets the name ({@link PabxNumbers#cleanName}: letters, digits, spaces and dashes, upper case, ten at most). */
	public void setName(String name) {
		this.name = PabxNumbers.cleanName(name);
		sync();
	}

	/**
	 * The screen's Save: all three settings, or none if the master number or first extension will not do. The result
	 * shows on the screen ({@link #getStatusKey()}). Returns true if saved.
	 */
	public boolean save(String number, String first, String newName) {
		String why = masterProblem(number);
		int extension = PabxNumbers.digits(first) && first.length() <= PabxNumbers.MAX_EXTENSION_DIGITS ? Integer.parseInt(first) : -1;
		if(why == null && (extension < 0 || extension > MAX_EXTENSION))
			why = "screen.radiocraft.pabx.bad_extension";
		if(why != null) {
			status(why, why.endsWith("master_length") ? Integer.toString(masterDigits()) : number);
			return false;
		}
		master = number;
		setFirstExtension(extension);
		setName(newName);
		status("screen.radiocraft.pabx.saved", "");
		return true;
	}

	/** Sets the line the screen shows under Save (cleared when the screen is opened). */
	public void status(String key, String arg) {
		statusKey = key;
		statusArg = arg;
		sync();
	}

	// -------------------- THE NETWORK --------------------

	/** The PABX a phone at {@code pos} belongs to: the nearest one touching it or on its data wire. */
	@Nullable
	public static PabxBlockEntity of(Level level, BlockPos pos) {
		List<PabxBlockEntity> found = DataWire.linked(level, pos, PabxBlockEntity.class);
		return found.isEmpty() ? null : found.get(0);
	}

	/** The telephones it serves: touching it or on its data wire, and nearer it than any other PABX. */
	public List<TelephoneBlockEntity> phones() {
		List<TelephoneBlockEntity> out = new ArrayList<>();
		if(level == null)
			return out;
		for(TelephoneBlockEntity phone : DataWire.linked(level, worldPosition, TelephoneBlockEntity.class))
			if(of(level, phone.getBlockPos()) == this)
				out.add(phone);
		return out;
	}

	/**
	 * Every PABX on this one's phone network, this one first, then outward: those it is wired to (touching, or on its
	 * data wire) and those across microwave links, and theirs in turn. Each maps to whether the way to it crosses a
	 * microwave route with interference or a marginal signal.
	 */
	public Map<PabxBlockEntity, Boolean> network() {
		Map<PabxBlockEntity, Boolean> out = new LinkedHashMap<>();
		out.put(this, false);
		if(level == null)
			return out;
		ArrayDeque<PabxBlockEntity> queue = new ArrayDeque<>(List.of(this));
		while(!queue.isEmpty() && out.size() < MAX_NETWORK) {
			PabxBlockEntity at = queue.poll();
			boolean lossy = out.get(at);
			for(PabxBlockEntity next : DataWire.linked(level, at.worldPosition, PabxBlockEntity.class))
				if(out.putIfAbsent(next, lossy) == null)
					queue.add(next);
			for(MicrowaveRoutes.Route route : MicrowaveRoutes.from(level, at.worldPosition))
				if(route.station() instanceof PabxBlockEntity next && out.putIfAbsent(next, lossy || route.lossy()) == null)
					queue.add(next);
		}
		return out;
	}

	/** The phone with extension {@code digits} here, or null (also if it has no power). */
	@Nullable
	private TelephoneBlockEntity extension(String digits) {
		if(!powered)
			return null;
		for(TelephoneBlockEntity phone : phones())
			if(PabxNumbers.isExtension(digits, phone.getExtension()))
				return phone;
		return null;
	}

	/** True if {@code digits} is one of its phones' extensions. */
	public boolean hasExtension(String digits) {
		for(TelephoneBlockEntity phone : phones())
			if(PabxNumbers.isExtension(digits, phone.getExtension()))
				return true;
		return false;
	}

	/**
	 * The phone {@code digits} rings when dialed from a phone on this PABX: one of its own extensions, else an
	 * extension on the powered PABX on its network whose master number {@code digits} starts with. Null if none.
	 */
	@Nullable
	public TelephoneBlockEntity resolve(String digits) {
		if(!powered)
			return null;
		TelephoneBlockEntity local = extension(digits);
		if(local != null)
			return local;
		for(PabxBlockEntity pabx : network().keySet()) {
			String rest = PabxNumbers.extensionAfter(digits, pabx.master);
			TelephoneBlockEntity phone = rest == null ? null : pabx.extension(rest);
			if(phone != null)
				return phone;
		}
		return null;
	}

	/** The master numbers and names of the powered PABXes on its network (this one included), nearest first. */
	public Map<String, String> directory() {
		Map<String, String> out = new LinkedHashMap<>();
		for(PabxBlockEntity pabx : network().keySet())
			if(pabx.powered && !pabx.master.isEmpty())
				out.putIfAbsent(pabx.master, pabx.name);
		return out;
	}

	/** True if both have power and are on one network. */
	public static boolean connected(@Nullable PabxBlockEntity a, @Nullable PabxBlockEntity b) {
		return a != null && b != null && a.powered && b.powered && (a == b || a.network().containsKey(b));
	}

	/** True if the way between them crosses a microwave route with interference or a marginal signal. */
	public static boolean lossy(@Nullable PabxBlockEntity a, @Nullable PabxBlockEntity b) {
		return a != null && b != null && a != b && Boolean.TRUE.equals(a.network().get(b));
	}

	// -------------------- TICK --------------------

	public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if(blockEntity instanceof PabxBlockEntity pabx)
			pabx.serverTick(level, state);
	}

	private void serverTick(Level level, BlockState state) {
		boolean now = getNetworkObject(level, worldPosition) instanceof PabxNetworkObject object && object.isPowered;
		if(now != powered) {
			powered = now;
			sync();
		}
		if(state.getValue(PabxBlock.LIT) != powered)
			level.setBlock(worldPosition, state.setValue(PabxBlock.LIT, powered), Block.UPDATE_CLIENTS);
		if(powered && level.getGameTime() % 20 == 0)
			number();
	}

	/** Gives each phone it has not numbered yet the lowest free extension from its first extension. */
	private void number() {
		List<TelephoneBlockEntity> phones = phones();
		for(TelephoneBlockEntity phone : phones) {
			if(numbered.contains(phone.getBlockPos()))
				continue;
			Set<Integer> taken = new HashSet<>();
			for(TelephoneBlockEntity other : phones)
				if(other != phone && numbered.contains(other.getBlockPos()))
					taken.add(other.getExtension());
			int extension = firstExtension;
			while(taken.contains(extension) && extension < MAX_EXTENSION)
				extension++;
			phone.assignExtension(extension);
			numbered.add(phone.getBlockPos().immutable());
			setChanged();
		}
		// Forget phones that are gone, so their numbers free up.
		Set<BlockPos> present = new HashSet<>();
		for(TelephoneBlockEntity phone : phones)
			present.add(phone.getBlockPos());
		if(numbered.retainAll(present))
			setChanged();
	}

	private void sync() {
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	// -------------------- SAVE AND SYNC --------------------

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("master", master);
		tag.putInt("firstExtension", firstExtension);
		tag.putString("name", name);
		ListTag list = new ListTag();
		for(BlockPos pos : numbered)
			list.add(LongTag.valueOf(pos.asLong()));
		tag.put("numbered", list);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		master = tag.getString("master"); // A "behind" tag from 1.10.0 is ignored: every PABX is a peer now.
		firstExtension = tag.contains("firstExtension") ? tag.getInt("firstExtension") : FIRST_EXTENSION;
		name = PabxNumbers.cleanName(tag.getString("name"));
		numbered.clear();
		for(Tag entry : tag.getList("numbered", Tag.TAG_LONG))
			numbered.add(BlockPos.of(((LongTag)entry).getAsLong()));
		if(tag.contains("powered")) { // The client's copy.
			powered = tag.getBoolean("powered");
			clientMasterDigits = tag.getInt("masterDigits");
			statusKey = tag.getString("statusKey");
			statusArg = tag.getString("statusArg");
		}
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putBoolean("powered", powered);
		tag.putInt("masterDigits", masterDigits());
		tag.putString("statusKey", statusKey);
		tag.putString("statusArg", statusArg);
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	// -------------------- MENU --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable(Radiocraft.translationKey("container", "pabx"));
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		if(!statusKey.isEmpty())
			status("", ""); // A fresh screen starts with no message.
		return new PabxMenu(id, this);
	}
}
